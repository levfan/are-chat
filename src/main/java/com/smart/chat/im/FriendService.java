package com.smart.chat.im;

import com.smart.chat.auth.AppUser;
import com.smart.chat.auth.AppUserService;
import com.smart.chat.common.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 好友：申请（可附言）/同意/拒绝/删除/备注/分组标签/置顶/免打扰/拉黑/会话列表（含未读、最后一条消息、对方在线状态）。
 */
@Service
public class FriendService {

    public record FriendVO(String id, String username, String nickname, String remark, String tag, boolean pinned,
                           boolean muted, boolean blocked, boolean online, String status, Long lastSeenAt, long unread,
                           MessagePreview lastMessage) {
    }

    public record MessagePreview(String content, String msgType, long created, boolean fromMe) {
    }

    public record FriendRequestVO(String id, String fromUser, String toUser, String message, String status,
                                  Long created) {
        public static FriendRequestVO of(FriendRequest r) {
            return new FriendRequestVO(r.getId(), r.getFromUser(), r.getToUser(),
                    r.getMessage() == null ? "" : r.getMessage(), r.getStatus(), r.getCreated());
        }
    }

    private final FriendMapper friendMapper;
    private final FriendRequestMapper requestMapper;
    private final PrivateMessageMapper messageMapper;
    private final UserProfileMapper profileMapper;
    private final ImPushService push;
    /** 合法用户目录：手机号注册产生的账号 */
    private final AppUserService userService;

    public FriendService(FriendMapper friendMapper, FriendRequestMapper requestMapper,
                         PrivateMessageMapper messageMapper, UserProfileMapper profileMapper, ImPushService push,
                         AppUserService userService) {
        this.friendMapper = friendMapper;
        this.requestMapper = requestMapper;
        this.messageMapper = messageMapper;
        this.profileMapper = profileMapper;
        this.push = push;
        this.userService = userService;
    }

    public List<FriendVO> listFriends(String me) {
        List<Friend> rows = friendMapper.findAllByOwner(me);
        // 对方资料（昵称 / 在线状态 online/busy/away）来自用户资料表
        List<String> peers = rows.stream().map(Friend::getFriendUsername).toList();
        Map<String, UserProfile> profileMap = peers.isEmpty() ? Map.of()
                : profileMapper.selectBatchIds(peers).stream()
                        .collect(Collectors.toMap(UserProfile::getUsername, p -> p));
        // 未读数与「最后一条」各一趟批量取，替代原先每人两趟（联系人越多省得越多，见 V49 与两个批量方法注释）
        Map<String, Long> latestCreated = messageMapper.findLatestCreatedPerPeer(me);
        Map<String, Long> unreadMap = new HashMap<>();
        for (Map<String, Object> row : friendMapper.selectUnreadCountsByPeer(me)) {
            if (row.get("unread") instanceof Number number) {
                unreadMap.put(String.valueOf(row.get("peer")), number.longValue());
            }
        }
        Map<String, PrivateMessage> lastMessage = pickLastMessages(
                messageMapper.findMessagesAtCreated(me, peers, latestCreated.values()), me, latestCreated);
        List<FriendVO> result = new ArrayList<>();
        for (Friend row : rows) {
            String peer = row.getFriendUsername();
            long unread = unreadMap.getOrDefault(peer, 0L);
            PrivateMessage latest = lastMessage.get(peer);
            MessagePreview preview = latest == null ? null
                    : new MessagePreview(latest.getContent(), latest.getMsgType(), latest.getCreated(),
                            latest.getFromUser().equals(me));
            UserProfile profile = profileMap.get(peer);
            result.add(new FriendVO(row.getId(), peer,
                    profile == null ? "" : profile.getNickname(),
                    row.getRemark() == null ? "" : row.getRemark(),
                    row.getTag() == null ? "" : row.getTag(),
                    Boolean.TRUE.equals(row.getPinned()), Boolean.TRUE.equals(row.getMuted()),
                    Integer.valueOf(1).equals(row.getBlocked()),
                    push.isOnline(peer),
                    profile == null || profile.getPresenceStatus() == null ? "online" : profile.getPresenceStatus(),
                    row.getLastSeenAt(), unread, preview));
        }
        // 置顶优先 → 最后一条消息时间倒序 → 用户名
        result.sort((a, b) -> {
            int byPinned = Boolean.compare(Boolean.TRUE.equals(b.pinned()), Boolean.TRUE.equals(a.pinned()));
            if (byPinned != 0) {
                return byPinned;
            }
            long at = a.lastMessage() == null ? 0L : a.lastMessage().created();
            long bt = b.lastMessage() == null ? 0L : b.lastMessage().created();
            if (at != bt) {
                return Long.compare(bt, at);
            }
            return a.username().compareTo(b.username());
        });
        return result;
    }

    /**
     * 从批量捞回的行里，为每个对端挑出「就是最后一条」的那行。
     * <p>{@code created IN (...)} 只是粗筛——同一毫秒内该会话的其它消息也会被带进来，
     * 所以再按 {@code latestCreated} 精确对齐；同一毫秒真有多行时取先遇到的一行，
     * 与原先 {@code ORDER BY created DESC LIMIT 1} 在并列时的任意行为一致。
     */
    private Map<String, PrivateMessage> pickLastMessages(List<PrivateMessage> messages, String me,
                                                         Map<String, Long> latestCreated) {
        Map<String, PrivateMessage> out = new HashMap<>();
        for (PrivateMessage m : messages) {
            String peer = m.getFromUser().equals(me) ? m.getToUser() : m.getFromUser();
            Long at = latestCreated.get(peer);
            if (at == null || m.getCreated() == null || !m.getCreated().equals(at)) {
                continue;
            }
            out.putIfAbsent(peer, m);
        }
        return out;
    }

    public FriendRequestVO apply(String me, String target, String message) {
        String raw = target == null ? "" : target.trim();
        if (raw.isEmpty()) {
            throw new BusinessException(400, "想加谁？手机号或用户名不能为空");
        }
        // 用户名统一小写；对方必须是手机号注册过的合法用户
        String targetName = userService.normalizeUsername(raw);
        if (targetName.equals(me)) {
            throw new BusinessException(400, "不能添加自己为好友");
        }
        if (!userService.exists(targetName)) {
            throw new BusinessException(400, "查无此人：对方还没用手机号注册，或手机号/用户名输错了");
        }
        if (friendMapper.findByOwnerAndFriend(me, targetName).isPresent()) {
            throw new BusinessException(409, "你们已经是好友了");
        }
        if (requestMapper.findPendingBetween(me, targetName).isPresent()) {
            throw new BusinessException(409, "好友申请已发送，等对方处理吧");
        }
        if (requestMapper.findPendingBetween(targetName, me).isPresent()) {
            throw new BusinessException(409, "对方已经先向你发起了申请，去「好友申请」处理吧");
        }
        FriendRequest request = FriendRequest.of(me, targetName);
        if (message != null && !message.isBlank()) {
            String note = message.trim();
            if (note.length() > 100) {
                throw new BusinessException(400, "申请留言最长 100 个字");
            }
            request.setMessage(note);
        }
        requestMapper.insert(request);
        push.pushFriendEvent("friend-request", targetName, request.getId());
        return FriendRequestVO.of(request);
    }

    /** 加好友联想候选：账号 + 脱敏手机号 + 与我的关系（可添加 / 已是好友 / 已申请 / 待我处理） */
    public record UserSuggestion(String username, String phone, String relation) {
    }

    /**
     * 按输入关键字给出候选用户名（匹配用户名或手机号，忽略大小写），最多 10 条。
     * 关系值：available / friend / pending-out / pending-in
     */
    public List<UserSuggestion> suggest(String me, String keyword) {
        List<UserSuggestion> result = new ArrayList<>();
        for (AppUser candidate : userService.search(keyword, me, 10)) {
            result.add(new UserSuggestion(candidate.getUsername(), candidate.maskedPhone(),
                    relation(me, candidate.getUsername())));
        }
        return result;
    }

    private String relation(String me, String name) {
        if (friendMapper.findByOwnerAndFriend(me, name).isPresent()) {
            return "friend";
        }
        if (requestMapper.findPendingBetween(me, name).isPresent()) {
            return "pending-out";
        }
        if (requestMapper.findPendingBetween(name, me).isPresent()) {
            return "pending-in";
        }
        return "available";
    }

    @Transactional
    public void accept(String me, String requestId) {
        FriendRequest request = requireRequest(requestId);
        if (!request.getToUser().equals(me)) {
            throw new BusinessException(403, "只能处理发给自己的申请");
        }
        if (!FriendRequest.STATUS_PENDING.equals(request.getStatus())) {
            throw new BusinessException(409, "该申请已经处理过了");
        }
        request.setStatus(FriendRequest.STATUS_ACCEPTED);
        request.setUpdatedAt(System.currentTimeMillis());
        requestMapper.updateById(request);

        String from = request.getFromUser();
        ensureFriendRow(from, me);
        ensureFriendRow(me, from);

        // 系统消息进入双方会话流（收件人视角未读 +1）
        PrivateMessage system = PrivateMessage.of(me, from, "我们已经成为好友，现在开始聊天吧！", PrivateMessage.TYPE_SYSTEM);
        messageMapper.insert(system);
        push.pushDm(system);
        push.pushFriendEvent("friend-accepted", from, request.getId());
    }

    public void reject(String me, String requestId) {
        FriendRequest request = requireRequest(requestId);
        if (!request.getToUser().equals(me)) {
            throw new BusinessException(403, "只能处理发给自己的申请");
        }
        if (!FriendRequest.STATUS_PENDING.equals(request.getStatus())) {
            throw new BusinessException(409, "该申请已经处理过了");
        }
        request.setStatus(FriendRequest.STATUS_REJECTED);
        request.setUpdatedAt(System.currentTimeMillis());
        requestMapper.updateById(request);
    }

    public List<FriendRequestVO> incoming(String me) {
        return requestMapper.findIncoming(me).stream().map(FriendRequestVO::of).toList();
    }

    public List<FriendRequestVO> outgoing(String me) {
        return requestMapper.findOutgoing(me).stream().map(FriendRequestVO::of).toList();
    }

    public void updateFriend(String me, String friendId, String remark, Boolean pinned, Boolean muted,
                             String tag, Boolean blocked) {
        Friend row = requireOwnFriend(me, friendId);
        if (remark != null) {
            String trimmed = remark.trim();
            if (trimmed.length() > 32) {
                throw new BusinessException(400, "备注最长 32 个字");
            }
            row.setRemark(trimmed);
        }
        if (tag != null) {
            String trimmedTag = tag.trim();
            if (trimmedTag.length() > 16) {
                throw new BusinessException(400, "分组标签最长 16 个字");
            }
            row.setTag(trimmedTag);
        }
        if (blocked != null) {
            row.setBlocked(blocked ? 1 : 0);
        }
        if (pinned != null) {
            row.setPinned(pinned);
        }
        if (muted != null) {
            row.setMuted(muted);
        }
        friendMapper.updateById(row);
    }

    public void deleteFriend(String me, String friendId) {
        Friend row = requireOwnFriend(me, friendId);
        String peer = row.getFriendUsername();
        friendMapper.deletePair(me, peer);
        push.pushFriendEvent("friend-deleted", peer, row.getId());
    }

    private Friend requireOwnFriend(String me, String friendId) {
        Friend row = friendMapper.selectById(friendId);
        if (row == null || !row.getOwnerUsername().equals(me)) {
            throw new BusinessException(404, "好友不存在");
        }
        return row;
    }

    private FriendRequest requireRequest(String requestId) {
        FriendRequest request = requestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(404, "申请不存在");
        }
        return request;
    }

    private void ensureFriendRow(String owner, String friend) {
        if (friendMapper.findByOwnerAndFriend(owner, friend).isEmpty()) {
            friendMapper.insert(Friend.of(owner, friend));
        }
    }
}
