package com.smart.chat.messaging.application;

import com.smart.chat.messaging.domain.RuleViolation;
import com.smart.chat.messaging.domain.conversation.PrivateMessage;
import com.smart.chat.messaging.domain.conversation.PrivateMessageRepository;
import com.smart.chat.messaging.domain.friend.Friend;
import com.smart.chat.messaging.domain.friend.FriendRepository;
import com.smart.chat.messaging.domain.friend.FriendshipGate;
import com.smart.chat.messaging.domain.pin.Conversation;
import com.smart.chat.messaging.domain.pin.ConversationPin;
import com.smart.chat.messaging.domain.pin.ConversationPinRepository;
import com.smart.chat.messaging.domain.reaction.MessageReaction;
import com.smart.chat.messaging.domain.reaction.MessageReactionRepository;
import com.smart.chat.messaging.infrastructure.throttle.MessageRateLimiter;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.smart.chat.messaging.application.DomainRules.guard;
import static com.smart.chat.messaging.application.DomainRules.rule;

/**
 * 点对点私聊：发送（落库 + 在线推送）、历史/搜索（装配回应/已读状态）、
 * 表情回应、编辑、已读回执、撤回；拉黑双向拦截。
 * 新增：82 文件消息、84 会话内置顶、85 清空聊天记录、86 敏感词过滤、87 发送限流、95 附件面板。
 * <p>
 * 改造后这里不再摸 Mapper：取数一律经 {@code domain} 的仓储端口，
 * 「谁能撤/谁能改/两分钟窗口/内容形态/引用必须同会话/置顶不能是撤回的消息」这些裁决由
 * {@link PrivateMessage} 与 {@link FriendshipGate} 说，文案与 code 由 {@link DomainRules} 原样翻译。
 */
@Service
public class PrivateMessageService {

    /** 回应支持的表情白名单（情侣风全集 36 个，与前端 REACTION_ALL 严格对齐） */
    public static final Set<String> REACTION_EMOJIS = MessageReaction.SUPPORTED;

    /** 82 文件消息：content 为 JSON（name/size/url），url 必须是站内下载地址 */
    public static final String TYPE_FILE = PrivateMessage.TYPE_FILE;

    /** 会话消息视图：在消息之上装配回应列表 / 已读回执 / 编辑标记。 */
    public record MessageVO(String id, String fromUser, String toUser, String content, String msgType,
                            String status, String replyToId, boolean read, boolean edited,
                            List<ReactionVO> reactions, Long created) {
    }

    public record ReactionVO(String username, String emoji) {
    }

    public record PinVO(String msgId, String createdBy) {
    }

    /**
     * 发送/编辑后的消息投影：字段名与顺序沿用改造前直接返回 {@code private_message} 行时的 JSON
     * （前端逐字段对账，PO 让出业务名之后这一份对外形状不能变）。
     */
    public record SentMessageVO(String id, String fromUser, String toUser, String content, String msgType,
                                String status, String replyToId, Integer readFlag, Integer edited,
                                Long created) {
        public static SentMessageVO of(PrivateMessage m) {
            return new SentMessageVO(m.id(), m.fromUser(), m.toUser(), m.content(), m.msgType(), m.status(),
                    m.replyToId(), m.readFlagRaw(), m.editedRaw(), m.created());
        }
    }

    private final PrivateMessageRepository messageRepository;
    private final FriendRepository friendRepository;
    private final MessageReactionRepository reactionRepository;
    private final ImPushService push;
    /** 合法用户目录：手机号注册产生的账号 */
    private final AccountDirectory accounts;
    /** 86 敏感词过滤 */
    private final ModerationService moderation;
    /** 87 发送限流（防刷屏） */
    private final MessageRateLimiter rateLimiter;
    /** 84 会话内置顶 */
    private final ConversationPinRepository pinRepository;

    public PrivateMessageService(PrivateMessageRepository messageRepository, FriendRepository friendRepository,
                                 MessageReactionRepository reactionRepository,
                                 ImPushService push, AccountDirectory accounts, ModerationService moderation,
                                 MessageRateLimiter rateLimiter, ConversationPinRepository pinRepository) {
        this.messageRepository = messageRepository;
        this.friendRepository = friendRepository;
        this.reactionRepository = reactionRepository;
        this.push = push;
        this.accounts = accounts;
        this.moderation = moderation;
        this.rateLimiter = rateLimiter;
        this.pinRepository = pinRepository;
    }

    public PrivateMessage send(String me, String peer, String content, String type, String replyToId) {
        // 87 防刷屏：所有消息类型统一限流
        rateLimiter.check(me);
        String rawPeer = peer == null ? "" : peer.trim();
        // 用户名统一小写；对方必须是手机号注册过的合法用户
        String peerName = accounts.normalizeUsername(rawPeer);
        guard(() -> FriendshipGate.requireMessagingPartner(me, peerName));
        guard(() -> FriendshipGate.requireKnownAccountToMail(accounts.exists(peerName)));
        Optional<Friend> mine = friendRepository.findByOwnerAndFriend(me, peerName);
        Optional<Friend> theirs = friendRepository.findByOwnerAndFriend(peerName, me);
        // 拉黑双向拦截
        guard(() -> FriendshipGate.requireMessagingAllowed(mine, theirs));
        String msgType = PrivateMessage.resolveType(type);
        boolean poke = PrivateMessage.TYPE_POKE.equals(msgType);
        String raw = content == null ? "" : content.trim();
        String text = switch (msgType) {
            // 82 文件消息：content 为 {name,size,url} JSON，站内地址校验属请求体格式检查，留在用例里
            case PrivateMessage.TYPE_FILE -> requireFilePayload(raw);
            // 67 拍一拍的后缀直接进领域，空串由领域回落默认文案
            case PrivateMessage.TYPE_POKE -> raw;
            // 86 敏感词过滤先于长度/空校验（censor 模式替换后照发）
            default -> moderation.clean(me, raw);
        };
        PrivateMessage message = rule(() -> PrivateMessage.offer(me, peerName, text, msgType,
                System.currentTimeMillis()));
        if (!poke && replyToId != null && !replyToId.isBlank()) {
            String quotedId = replyToId.trim();
            PrivateMessage quoted = rule(() -> messageRepository.findById(quotedId)
                    .orElseThrow(() -> RuleViolation.notFound("引用的消息不存在")));
            guard(() -> quoted.requireBetween(me, peerName));
            message.quote(quoted.id());
        }
        messageRepository.save(message);
        if (push.isOnline(peerName)) {
            push.pushDm(message);
        }
        return message;
    }

    /** 82 文件消息校验：name/size/url 必填，url 必须指向站内文件下载地址 */
    private String requireFilePayload(String raw) {
        if (raw.isEmpty() || !raw.startsWith("{")) {
            throw new BusinessException(400, "文件消息格式不正确");
        }
        com.alibaba.fastjson2.JSONObject payload;
        try {
            payload = com.alibaba.fastjson2.JSON.parseObject(raw);
        } catch (Exception e) {
            throw new BusinessException(400, "文件消息格式不正确");
        }
        String name = payload == null ? null : payload.getString("name");
        String url = payload == null ? null : payload.getString("url");
        if (name == null || name.isBlank() || url == null || !url.startsWith(PrivateMessage.IMAGE_URL_PREFIX)) {
            throw new BusinessException(400, "文件消息必须携带站内文件的名称与下载地址");
        }
        return raw;
    }

    /** 会话内关键字搜索（最近 50 条，正序返回）。 */
    public List<MessageVO> search(String me, String peer, String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) {
            throw new BusinessException(400, "搜索关键字不能为空");
        }
        List<PrivateMessage> rows = messageRepository.searchConversation(me, peer, q).stream()
                .sorted(Comparator.comparing(PrivateMessage::created))
                .toList();
        return toVOs(rows, me);
    }

    /** 倒序分页拉取后反转为正序（before 为游标：早于该时间戳）。 */
    public List<MessageVO> history(String me, String peer, Long before, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<PrivateMessage> page = messageRepository.findConversationPage(me, peer, before, safeLimit);
        return toVOs(page.stream().sorted(Comparator.comparing(PrivateMessage::created)).toList(), me);
    }

    /** 一次性装配：回应列表、我发消息的已读状态、编辑标记。 */
    private List<MessageVO> toVOs(List<PrivateMessage> rows, String me) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> ids = rows.stream().map(PrivateMessage::id).toList();
        Map<String, List<ReactionVO>> reactionMap = reactionRepository.listByMsgIds(ids).stream()
                .collect(Collectors.groupingBy(MessageReaction::msgId,
                        Collectors.mapping(r -> new ReactionVO(r.username(), r.emoji()), Collectors.toList())));
        List<MessageVO> result = new ArrayList<>(rows.size());
        for (PrivateMessage m : rows) {
            boolean read = m.sentBy(me) && m.readFlag();
            result.add(new MessageVO(m.id(), m.fromUser(), m.toUser(), m.content(), m.msgType(),
                    m.status(), m.replyToId(), read, m.editedFlag(),
                    reactionMap.getOrDefault(m.id(), List.of()), m.created()));
        }
        return result;
    }

    @Transactional
    public void markRead(String me, String peer) {
        Friend row = rule(() -> friendRepository.findByOwnerAndFriend(me, peer)
                .orElseThrow(() -> RuleViolation.forbidden("还不是好友")));
        row.markReadAt(System.currentTimeMillis());
        friendRepository.save(row);
        // 已读回执：把对方发来的消息标记已读
        messageRepository.markIncomingRead(peer, me);
        push.pushRead(me, peer);
    }

    public void recall(String me, String messageId) {
        PrivateMessage message = requireMessage(messageId);
        guard(() -> message.recallBy(me, System.currentTimeMillis()));
        messageRepository.save(message);
        if (push.isOnline(message.toUser())) {
            push.pushRecall(message);
        }
    }

    // ---------- 表情回应 ----------

    /** toggle：已回应则取消，未回应则添加。会话双方都收推送。 */
    @Transactional
    public boolean toggleReaction(String me, String msgId, String emoji) {
        PrivateMessage message = requireParticipantMessage(me, msgId);
        // 表情白名单先判，再去查有没有回过（顺序沿用改造前，否则非法表情会多打一趟查询）
        Optional<MessageReaction> existing = rule(() -> {
            MessageReaction.requireSupported(emoji);
            return reactionRepository.findByMsgIdAndUserAndEmoji(msgId, me, emoji);
        });
        boolean added;
        if (existing.isPresent()) {
            reactionRepository.deleteById(existing.get().id());
            added = false;
        } else {
            reactionRepository.save(rule(() -> MessageReaction.add(msgId, me, emoji, System.currentTimeMillis())));
            added = true;
        }
        push.pushReaction(message, me, emoji, added);
        return added;
    }

    // ---------- 编辑 ----------

    /** 2 分钟内可编辑自己发出的文本消息。 */
    public PrivateMessage edit(String me, String msgId, String content) {
        PrivateMessage message = requireMessage(msgId);
        guard(() -> message.editBy(me, System.currentTimeMillis(), content));
        messageRepository.save(message);
        push.pushEdit(message);
        return message;
    }

    private PrivateMessage requireMessage(String msgId) {
        return rule(() -> messageRepository.findById(msgId)
                .orElseThrow(() -> RuleViolation.notFound("消息不存在")));
    }

    private PrivateMessage requireParticipantMessage(String me, String msgId) {
        PrivateMessage message = requireMessage(msgId);
        guard(() -> message.requireParticipant(me));
        return message;
    }

    // ---------- 84 会话内置顶消息 ----------

    /** 置顶：仅双方可见的会话级置顶，一个会话一条，后者覆盖前者。 */
    public PinVO pin(String me, String peer, String msgId) {
        String peerName = accounts.normalizeUsername(peer);
        requireConversation(me, peerName);
        PrivateMessage message = requireParticipantMessage(me, msgId);
        guard(() -> message.requirePinable());
        pinRepository.replace(ConversationPin.by(me, peerName, message.id(), System.currentTimeMillis()));
        push.pushPin(me, peerName, message.id(), true);
        return new PinVO(message.id(), me);
    }

    public void unpin(String me, String peer) {
        String peerName = accounts.normalizeUsername(peer);
        requireConversation(me, peerName);
        pinRepository.clear(Conversation.between(me, peerName));
        push.pushPin(me, peerName, null, false);
    }

    /** 当前会话置顶（无则返回 null，由 Optional 表达） */
    public Optional<PinVO> currentPin(String me, String peer) {
        String peerName = accounts.normalizeUsername(peer);
        return pinRepository.findFor(Conversation.between(me, peerName))
                .map(pin -> new PinVO(pin.msgId(), pin.createdBy()));
    }

    private void requireConversation(String me, String peerName) {
        guard(() -> FriendshipGate.requireConversationUsable(friendRepository.findByOwnerAndFriend(me, peerName)));
    }

    // ---------- 85 清空聊天记录 ----------

    /** 清空双方会话全部消息（危险操作，前端二次确认后调用），返回删除条数 */
    @Transactional
    public long clearConversation(String me, String peer) {
        String peerName = accounts.normalizeUsername(peer);
        requireConversation(me, peerName);
        int deleted = messageRepository.deleteConversation(me, peerName);
        unpin(me, peerName);
        return deleted;
    }
}
