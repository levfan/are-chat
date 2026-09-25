package com.smart.chat.im;

import com.smart.chat.auth.AppUserService;
import com.smart.chat.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 点对点私聊：发送（落库 + 在线推送）、历史/搜索（装配回应/收藏/已读状态）、
 * 表情回应、收藏、编辑、已读回执、撤回；拉黑双向拦截。
 */
@Service
public class PrivateMessageService {

    /** 回应支持的表情（保持克制的专业风格） */
    public static final Set<String> REACTION_EMOJIS = Set.of("👍", "❤️", "😂", "😮", "😢", "🔥");

    /** 会话消息视图：在消息之上装配回应列表 / 收藏 / 已读回执 / 编辑标记。 */
    public record MessageVO(String id, String fromUser, String toUser, String content, String msgType,
                            String status, String replyToId, boolean read, boolean edited, boolean starred,
                            List<ReactionVO> reactions, Long created) {
    }

    public record ReactionVO(String username, String emoji) {
    }

    /** 收藏夹条目：peer 为消息的另一方。 */
    public record StarVO(String msgId, String peer, String content, String msgType, String status, Long created) {
    }

    private final PrivateMessageMapper messageMapper;
    private final FriendMapper friendMapper;
    private final MessageReactionMapper reactionMapper;
    private final MessageStarMapper starMapper;
    private final ImPushService push;
    /** 合法用户目录：手机号注册产生的账号 */
    private final AppUserService userService;

    public PrivateMessageService(PrivateMessageMapper messageMapper, FriendMapper friendMapper,
                                 MessageReactionMapper reactionMapper, MessageStarMapper starMapper,
                                 ImPushService push, AppUserService userService) {
        this.messageMapper = messageMapper;
        this.friendMapper = friendMapper;
        this.reactionMapper = reactionMapper;
        this.starMapper = starMapper;
        this.push = push;
        this.userService = userService;
    }

    public PrivateMessage send(String me, String peer, String content, String type, String replyToId) {
        String rawPeer = peer == null ? "" : peer.trim();
        // 用户名统一小写；对方必须是手机号注册过的合法用户
        String peerName = userService.normalizeUsername(rawPeer);
        if (peerName.equals(me)) {
            throw new BusinessException(400, "不能给自己发私信");
        }
        if (!userService.exists(peerName)) {
            throw new BusinessException(400, "查无此人：对方还没有用手机号注册");
        }
        if (friendMapper.findByOwnerAndFriend(me, peerName).isEmpty()) {
            throw new BusinessException(403, "还不是好友，先加个好友吧");
        }
        // 拉黑双向拦截
        friendMapper.findByOwnerAndFriend(me, peerName)
                .filter(f -> Integer.valueOf(1).equals(f.getBlocked()))
                .ifPresent(f -> {
                    throw new BusinessException(403, "已拉黑对方，解除后才能发消息");
                });
        friendMapper.findByOwnerAndFriend(peerName, me)
                .filter(f -> Integer.valueOf(1).equals(f.getBlocked()))
                .ifPresent(f -> {
                    throw new BusinessException(403, "对方已将你拉黑");
                });
        boolean poke = PrivateMessage.TYPE_POKE.equals(type);
        boolean image = PrivateMessage.TYPE_IMAGE.equals(type);
        boolean card = PrivateMessage.TYPE_CARD.equals(type);
        boolean location = PrivateMessage.TYPE_LOCATION.equals(type);
        String msgType = poke ? PrivateMessage.TYPE_POKE
                : image ? PrivateMessage.TYPE_IMAGE
                : card ? PrivateMessage.TYPE_CARD
                : location ? PrivateMessage.TYPE_LOCATION
                : PrivateMessage.TYPE_TEXT;
        String raw = content == null ? "" : content.trim();
        String text;
        if (poke) {
            // 67 拍一拍支持自定义后缀（最多 100 字），留空用默认文案
            text = raw.isEmpty() ? PrivateMessage.POKE_TEXT : raw;
            if (text.length() > PrivateMessage.POKE_SUFFIX_MAX) {
                throw new BusinessException(400, "拍一拍后缀最长 100 字");
            }
        } else {
            text = raw;
            if (text.isEmpty()) {
                throw new BusinessException(400, "消息内容不能为空");
            }
            if (text.length() > 2000) {
                throw new BusinessException(400, "消息最长 2000 字");
            }
        }
        if (image && !text.startsWith(PrivateMessage.IMAGE_URL_PREFIX)) {
            throw new BusinessException(400, "图片消息必须先上传到站内");
        }
        if ((card || location) && !text.startsWith("{")) {
            throw new BusinessException(400, "卡片消息内容格式不正确");
        }
        PrivateMessage message = PrivateMessage.of(me, peerName, text, msgType);
        if (!poke && replyToId != null && !replyToId.isBlank()) {
            message.setReplyToId(requireReplyInConversation(me, peerName, replyToId.trim()).getId());
        }
        messageMapper.insert(message);
        if (push.isOnline(peerName)) {
            push.pushDm(message);
        }
        return message;
    }

    /** 引用回复：引用的消息必须存在，且属于同一会话。 */
    private PrivateMessage requireReplyInConversation(String me, String peer, String replyToId) {
        PrivateMessage target = messageMapper.selectById(replyToId);
        if (target == null) {
            throw new BusinessException(404, "引用的消息不存在");
        }
        boolean sameConversation = (me.equals(target.getFromUser()) && peer.equals(target.getToUser()))
                || (peer.equals(target.getFromUser()) && me.equals(target.getToUser()));
        if (!sameConversation) {
            throw new BusinessException(400, "只能引用本会话内的消息");
        }
        return target;
    }

    /** 会话内关键字搜索（最近 50 条，正序返回）。 */
    public List<MessageVO> search(String me, String peer, String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) {
            throw new BusinessException(400, "搜索关键字不能为空");
        }
        List<PrivateMessage> rows = messageMapper.searchConversation(me, peer, q).stream()
                .sorted(java.util.Comparator.comparing(PrivateMessage::getCreated))
                .toList();
        return toVOs(rows, me);
    }

    /** 倒序分页拉取后反转为正序（before 为游标：早于该时间戳）。 */
    public List<MessageVO> history(String me, String peer, Long before, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<PrivateMessage> page = messageMapper.findConversationPage(me, peer, before, safeLimit);
        return toVOs(page.stream().sorted(java.util.Comparator.comparing(PrivateMessage::getCreated)).toList(), me);
    }

    /** 56 导出当前会话全部消息（含撤回/编辑标记与回应，正序）。 */
    public List<MessageVO> export(String me, String peer) {
        String peerName = peer == null ? "" : peer.trim();
        if (peerName.isEmpty()) {
            throw new BusinessException(400, "会话对象不能为空");
        }
        return toVOs(messageMapper.findConversationAll(me, peerName), me);
    }

    /** 一次性装配：回应列表、我的收藏、我发消息的已读状态、编辑标记。 */
    private List<MessageVO> toVOs(List<PrivateMessage> rows, String me) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<String> ids = rows.stream().map(PrivateMessage::getId).toList();
        Map<String, List<ReactionVO>> reactionMap = reactionMapper.findByMsgIds(ids).stream()
                .collect(Collectors.groupingBy(MessageReaction::getMsgId,
                        Collectors.mapping(r -> new ReactionVO(r.getUsername(), r.getEmoji()), Collectors.toList())));
        Set<String> starredIds = starMapper.findByUsernameAndMsgIds(me, ids).stream()
                .map(MessageStar::getMsgId)
                .collect(Collectors.toSet());
        List<MessageVO> result = new ArrayList<>(rows.size());
        for (PrivateMessage m : rows) {
            boolean read = m.getFromUser().equals(me) && Integer.valueOf(1).equals(m.getReadFlag());
            result.add(new MessageVO(m.getId(), m.getFromUser(), m.getToUser(), m.getContent(), m.getMsgType(),
                    m.getStatus(), m.getReplyToId(), read, Integer.valueOf(1).equals(m.getEdited()),
                    starredIds.contains(m.getId()),
                    reactionMap.getOrDefault(m.getId(), List.of()), m.getCreated()));
        }
        return result;
    }

    @Transactional
    public void markRead(String me, String peer) {
        Friend row = friendMapper.findByOwnerAndFriend(me, peer)
                .orElseThrow(() -> new BusinessException(403, "还不是好友"));
        row.setLastReadAt(System.currentTimeMillis());
        friendMapper.updateById(row);
        // 已读回执：把对方发来的消息标记已读
        messageMapper.markIncomingRead(peer, me);
        push.pushRead(me, peer);
    }

    public void recall(String me, String messageId) {
        PrivateMessage message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException(404, "消息不存在");
        }
        if (!message.getFromUser().equals(me)) {
            throw new BusinessException(403, "只能撤回自己发的消息");
        }
        if (PrivateMessage.STATUS_RECALLED.equals(message.getStatus())) {
            throw new BusinessException(409, "这条消息已经撤回过了");
        }
        if (System.currentTimeMillis() - message.getCreated() > PrivateMessage.RECALL_WINDOW_MS) {
            throw new BusinessException(400, "超过 2 分钟，撤不回来了～");
        }
        message.setStatus(PrivateMessage.STATUS_RECALLED);
        messageMapper.updateById(message);
        if (push.isOnline(message.getToUser())) {
            push.pushRecall(message);
        }
    }

    // ---------- 表情回应 ----------

    /** toggle：已回应则取消，未回应则添加。会话双方都收推送。 */
    @Transactional
    public boolean toggleReaction(String me, String msgId, String emoji) {
        PrivateMessage message = requireParticipantMessage(me, msgId);
        if (emoji == null || !REACTION_EMOJIS.contains(emoji)) {
            throw new BusinessException(400, "不支持的表情回应");
        }
        MessageReaction existing = reactionMapper.findUnique(msgId, me, emoji);
        boolean added;
        if (existing != null) {
            reactionMapper.deleteById(existing.getId());
            added = false;
        } else {
            reactionMapper.insert(MessageReaction.of(msgId, me, emoji));
            added = true;
        }
        push.pushReaction(message, me, emoji, added);
        return added;
    }

    // ---------- 收藏 ----------

    public boolean toggleStar(String me, String msgId) {
        requireParticipantMessage(me, msgId);
        MessageStar existing = starMapper.findUnique(me, msgId);
        if (existing != null) {
            starMapper.deleteById(existing.getId());
            return false;
        }
        starMapper.insert(MessageStar.of(me, msgId));
        return true;
    }

    /** 收藏夹：按收藏时间倒序，peer 为消息另一方。 */
    public List<StarVO> listStars(String me) {
        List<MessageStar> stars = starMapper.findByUsername(me);
        if (stars.isEmpty()) {
            return List.of();
        }
        Map<String, PrivateMessage> messages = stars.stream()
                .map(s -> messageMapper.selectById(s.getMsgId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(PrivateMessage::getId, Function.identity()));
        return stars.stream()
                .filter(s -> messages.containsKey(s.getMsgId()))
                .map(s -> {
                    PrivateMessage m = messages.get(s.getMsgId());
                    String peer = m.getFromUser().equals(me) ? m.getToUser() : m.getFromUser();
                    return new StarVO(m.getId(), peer, m.getContent(), m.getMsgType(), m.getStatus(), m.getCreated());
                })
                .toList();
    }

    // ---------- 编辑 ----------

    /** 2 分钟内可编辑自己发出的文本消息。 */
    public PrivateMessage edit(String me, String msgId, String content) {
        PrivateMessage message = messageMapper.selectById(msgId);
        if (message == null) {
            throw new BusinessException(404, "消息不存在");
        }
        if (!message.getFromUser().equals(me)) {
            throw new BusinessException(403, "只能编辑自己发的消息");
        }
        if (!PrivateMessage.TYPE_TEXT.equals(message.getMsgType())) {
            throw new BusinessException(400, "只有文本消息可以编辑");
        }
        if (PrivateMessage.STATUS_RECALLED.equals(message.getStatus())) {
            throw new BusinessException(409, "消息已撤回，不能编辑");
        }
        if (System.currentTimeMillis() - message.getCreated() > PrivateMessage.RECALL_WINDOW_MS) {
            throw new BusinessException(400, "超过 2 分钟，不能编辑了");
        }
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BusinessException(400, "消息内容不能为空");
        }
        if (text.length() > 2000) {
            throw new BusinessException(400, "消息最长 2000 字");
        }
        message.setContent(text);
        message.setEdited(1);
        messageMapper.updateById(message);
        push.pushEdit(message);
        return message;
    }

    private PrivateMessage requireParticipantMessage(String me, String msgId) {
        PrivateMessage message = messageMapper.selectById(msgId);
        if (message == null) {
            throw new BusinessException(404, "消息不存在");
        }
        if (!message.getFromUser().equals(me) && !message.getToUser().equals(me)) {
            throw new BusinessException(403, "只能操作本会话内的消息");
        }
        return message;
    }
}
