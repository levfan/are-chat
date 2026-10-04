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
import com.smart.chat.messaging.domain.star.MessageStar;
import com.smart.chat.messaging.domain.star.MessageStarRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.smart.chat.messaging.application.DomainRules.guard;
import static com.smart.chat.messaging.application.DomainRules.rule;

/**
 * 点对点私聊：发送（落库 + 在线推送）、历史/搜索（装配回应/收藏/已读状态）、
 * 表情回应、收藏、编辑、已读回执、撤回；拉黑双向拦截。
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

    /** 会话消息视图：在消息之上装配回应列表 / 收藏 / 已读回执 / 编辑标记 / 心动时刻。 */
    public record MessageVO(String id, String fromUser, String toUser, String content, String msgType,
                            String status, String replyToId, boolean read, boolean edited, boolean starred,
                            List<ReactionVO> reactions, Long created, Long heartAt) {
    }

    public record ReactionVO(String username, String emoji) {
    }

    /** 收藏夹条目：peer 为消息的另一方。 */
    public record StarVO(String msgId, String peer, String content, String msgType, String status, Long created) {
    }

    /** 81 全局搜索命中：peer 为会话另一方，便于前端跳转 */
    public record GlobalSearchHitVO(String id, String peer, String content, String fromUser, Long created) {
    }

    /** 95 附件条目 */
    public record AttachmentVO(String id, String name, long size, String url, String fromUser, Long created) {
    }

    public record PinVO(String msgId, String createdBy) {
    }

    /**
     * 发送/编辑后的消息投影：字段名与顺序沿用改造前直接返回 {@code private_message} 行时的 JSON
     * （前端逐字段对账，PO 让出业务名之后这一份对外形状不能变）。
     */
    public record SentMessageVO(String id, String fromUser, String toUser, String content, String msgType,
                                String status, String replyToId, Integer readFlag, Integer edited, Long heartAt,
                                Long created) {
        public static SentMessageVO of(PrivateMessage m) {
            return new SentMessageVO(m.id(), m.fromUser(), m.toUser(), m.content(), m.msgType(), m.status(),
                    m.replyToId(), m.readFlagRaw(), m.editedRaw(), m.heartAt(), m.created());
        }
    }

    private final PrivateMessageRepository messageRepository;
    private final FriendRepository friendRepository;
    private final MessageReactionRepository reactionRepository;
    private final MessageStarRepository starRepository;
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
                                 MessageReactionRepository reactionRepository, MessageStarRepository starRepository,
                                 ImPushService push, AccountDirectory accounts, ModerationService moderation,
                                 MessageRateLimiter rateLimiter, ConversationPinRepository pinRepository) {
        this.messageRepository = messageRepository;
        this.friendRepository = friendRepository;
        this.reactionRepository = reactionRepository;
        this.starRepository = starRepository;
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

    /** 56 导出当前会话全部消息（含撤回/编辑标记与回应，正序）。 */
    public List<MessageVO> export(String me, String peer) {
        String peerName = peer == null ? "" : peer.trim();
        if (peerName.isEmpty()) {
            throw new BusinessException(400, "会话对象不能为空");
        }
        return toVOs(messageRepository.findConversationAll(me, peerName), me);
    }

    /** 一次性装配：回应列表、我的收藏、我发消息的已读状态、编辑标记。 */
    private List<MessageVO> toVOs(List<PrivateMessage> rows, String me) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> ids = rows.stream().map(PrivateMessage::id).toList();
        Map<String, List<ReactionVO>> reactionMap = reactionRepository.listByMsgIds(ids).stream()
                .collect(Collectors.groupingBy(MessageReaction::msgId,
                        Collectors.mapping(r -> new ReactionVO(r.username(), r.emoji()), Collectors.toList())));
        Set<String> starredIds = starRepository.listByUsernameAndMsgIds(me, ids).stream()
                .map(MessageStar::msgId)
                .collect(Collectors.toSet());
        List<MessageVO> result = new ArrayList<>(rows.size());
        for (PrivateMessage m : rows) {
            boolean read = m.sentBy(me) && m.readFlag();
            result.add(new MessageVO(m.id(), m.fromUser(), m.toUser(), m.content(), m.msgType(),
                    m.status(), m.replyToId(), read, m.editedFlag(), starredIds.contains(m.id()),
                    reactionMap.getOrDefault(m.id(), List.of()), m.created(), m.heartAt()));
        }
        return result;
    }

    /**
     * F36 心动时刻：标记/取消标记一条消息。只有消息双方能操作；
     * 标记后实时推送给对方（message-hearted 事件），在情侣空间可回顾。
     */
    public MessageVO markHeart(String me, String messageId, boolean hearted) {
        PrivateMessage message = rule(() -> messageRepository.findById(messageId)
                .orElseThrow(() -> RuleViolation.notFound("这条消息不存在")));
        guard(() -> message.heartBy(me, hearted, System.currentTimeMillis()));
        messageRepository.save(message);
        String peer = message.peerOf(me);
        push.pushCoupleEvent(hearted ? "message-hearted" : "message-unhearted", me, peer,
                hearted ? "TA 收藏了一条心动时刻 💗 快去情侣空间看看" : "TA 取消了一条心动时刻标记");
        return new MessageVO(message.id(), message.fromUser(), message.toUser(), message.content(),
                message.msgType(), message.status(), message.replyToId(),
                message.sentBy(me) && message.readFlag(), message.editedFlag(), false, List.of(),
                message.created(), message.heartAt());
    }

    /** F36 心动时刻列表：与某人聊天中被标记的消息（新→旧，最多 100 条）。 */
    public List<MessageVO> heartMoments(String me, String peer) {
        String peerName = peer == null ? "" : peer.trim();
        List<PrivateMessage> rows = messageRepository.findHeartMoments(me, peerName);
        return toVOs(rows, me);
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

    // ---------- 收藏 ----------

    public boolean toggleStar(String me, String msgId) {
        requireParticipantMessage(me, msgId);
        Optional<MessageStar> existing = starRepository.findByUsernameAndMsgId(me, msgId);
        if (existing.isPresent()) {
            starRepository.deleteById(existing.get().id());
            return false;
        }
        starRepository.save(MessageStar.add(me, msgId, System.currentTimeMillis()));
        return true;
    }

    /** 收藏夹：按收藏时间倒序，peer 为消息另一方。 */
    public List<StarVO> listStars(String me) {
        List<MessageStar> stars = starRepository.listByUsername(me);
        if (stars.isEmpty()) {
            return List.of();
        }
        Map<String, PrivateMessage> messages = messageRepository
                .listByIds(stars.stream().map(MessageStar::msgId).distinct().toList()).stream()
                .collect(Collectors.toMap(PrivateMessage::id, Function.identity()));
        return stars.stream()
                .filter(s -> messages.containsKey(s.msgId()))
                .map(s -> {
                    PrivateMessage m = messages.get(s.msgId());
                    return new StarVO(m.id(), m.peerOf(me), m.content(), m.msgType(), m.status(), m.created());
                })
                .toList();
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

    // ---------- 81 全局消息搜索 ----------

    /** 跨会话搜索我参与的全部文本消息（未撤回，最近 50 条，按时间倒序） */
    public List<GlobalSearchHitVO> searchGlobal(String me, String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) {
            throw new BusinessException(400, "搜索关键字不能为空");
        }
        return messageRepository.searchGlobal(me, q).stream()
                .map(m -> new GlobalSearchHitVO(m.id(), m.peerOf(me), m.content(), m.fromUser(), m.created()))
                .toList();
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

    // ---------- 95 会话附件面板 ----------

    /** 会话内图片/文件附件（最近 100 条，倒序） */
    public List<AttachmentVO> attachments(String me, String peer, String type) {
        String peerName = accounts.normalizeUsername(peer);
        requireConversation(me, peerName);
        String msgType = "file".equals(type) ? PrivateMessage.TYPE_FILE : PrivateMessage.TYPE_IMAGE;
        List<PrivateMessage> rows = messageRepository.findAttachments(me, peerName, msgType);
        List<AttachmentVO> result = new ArrayList<>(rows.size());
        for (PrivateMessage m : rows) {
            if (msgType.equals(PrivateMessage.TYPE_IMAGE)) {
                result.add(new AttachmentVO(m.id(), "图片", 0, m.content(), m.fromUser(), m.created()));
            } else {
                com.alibaba.fastjson2.JSONObject payload;
                try {
                    payload = com.alibaba.fastjson2.JSON.parseObject(m.content());
                } catch (Exception e) {
                    continue;
                }
                if (payload == null) {
                    continue;
                }
                result.add(new AttachmentVO(m.id(), payload.getString("name"),
                        payload.getLongValue("size"), payload.getString("url"), m.fromUser(), m.created()));
            }
        }
        return result;
    }
}
