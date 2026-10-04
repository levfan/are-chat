package com.smart.chat.messaging.application;

import com.smart.chat.messaging.domain.RuleViolation;
import com.smart.chat.messaging.domain.conversation.PrivateMessage;
import com.smart.chat.messaging.domain.conversation.PrivateMessageRepository;
import com.smart.chat.messaging.domain.friend.Friend;
import com.smart.chat.messaging.domain.friend.FriendRepository;
import com.smart.chat.messaging.domain.friend.FriendRequest;
import com.smart.chat.messaging.domain.friend.FriendRequestRepository;
import com.smart.chat.messaging.domain.friend.FriendshipGate;
import com.smart.chat.messaging.domain.profile.UserProfile;
import com.smart.chat.messaging.domain.profile.UserProfileRepository;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.smart.chat.messaging.application.DomainRules.guard;
import static com.smart.chat.messaging.application.DomainRules.rule;

/**
 * 好友：申请（可附言）/同意/拒绝/删除/备注/分组标签/置顶/免打扰/拉黑/会话列表（含未读、最后一条消息、对方在线状态）。
 * <p>
 * 改造后这里只做四件事：取会话身份、经端口取聚合、调领域方法、把结果投影成 VO 或推 WS。
 * 关系与状态的裁决在 {@link Friend} / {@link FriendRequest} / {@link FriendshipGate} 里，
 * 取数口径（未读一趟 JOIN、「最后一条」两趟 GROUP BY 再按时间点回捞）在 {@link FriendRepository} 的实现里。
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
            return new FriendRequestVO(r.id(), r.fromUser(), r.toUser(),
                    r.message() == null ? "" : r.message(), r.status(), r.created());
        }
    }

    /** 加好友联想候选：账号 + 脱敏手机号 + 与我的关系（可添加 / 已是好友 / 已申请 / 待我处理） */
    public record UserSuggestion(String username, String phone, String relation) {
    }

    private final FriendRepository friendRepository;
    private final FriendRequestRepository requestRepository;
    private final PrivateMessageRepository messageRepository;
    private final UserProfileRepository profileRepository;
    private final ImPushService push;
    /** 合法用户目录：手机号注册产生的账号 */
    private final AccountDirectory accounts;

    public FriendService(FriendRepository friendRepository, FriendRequestRepository requestRepository,
                         PrivateMessageRepository messageRepository, UserProfileRepository profileRepository,
                         ImPushService push, AccountDirectory accounts) {
        this.friendRepository = friendRepository;
        this.requestRepository = requestRepository;
        this.messageRepository = messageRepository;
        this.profileRepository = profileRepository;
        this.push = push;
        this.accounts = accounts;
    }

    public List<FriendVO> listFriends(String me) {
        List<Friend> rows = friendRepository.findAllByOwner(me);
        // 对方资料（昵称 / 在线状态 online/busy/away）来自用户资料表
        List<String> peers = rows.stream().map(Friend::friendUsername).toList();
        Map<String, UserProfile> profileMap = peers.isEmpty() ? Map.of()
                : profileRepository.listByUsernames(peers).stream()
                        .collect(Collectors.toMap(UserProfile::username, p -> p));
        // 未读数与「最后一条」各一趟批量取，替代原先每人两趟（联系人越多省得越多，见 V49 与两个批量方法注释）
        Map<String, Long> latestCreated = messageRepository.findLatestCreatedPerPeer(me);
        Map<String, Long> unreadMap = friendRepository.unreadCountByPeer(me);
        Map<String, PrivateMessage> lastMessage = pickLastMessages(
                messageRepository.findMessagesAtCreated(me, peers, latestCreated.values()), me, latestCreated);
        List<FriendVO> result = new ArrayList<>();
        for (Friend row : rows) {
            String peer = row.friendUsername();
            long unread = unreadMap.getOrDefault(peer, 0L);
            PrivateMessage latest = lastMessage.get(peer);
            MessagePreview preview = latest == null ? null
                    : new MessagePreview(latest.content(), latest.msgType(), latest.created(), latest.sentBy(me));
            UserProfile profile = profileMap.get(peer);
            result.add(new FriendVO(row.id(), peer,
                    profile == null ? "" : profile.nickname(),
                    row.remark() == null ? "" : row.remark(),
                    row.tag() == null ? "" : row.tag(),
                    row.pinnedFlag(), row.mutedFlag(), row.blockedFlag(),
                    push.isOnline(peer),
                    profile == null ? "online" : profile.presenceStatusForView(),
                    row.lastSeenAt(), unread, preview));
        }
        // 置顶优先 → 最后一条消息时间倒序 → 用户名
        result.sort((a, b) -> {
            int byPinned = Boolean.compare(b.pinned(), a.pinned());
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
            String peer = m.peerOf(me);
            Long at = latestCreated.get(peer);
            if (at == null || m.created() == null || !m.created().equals(at)) {
                continue;
            }
            out.putIfAbsent(peer, m);
        }
        return out;
    }

    public FriendRequestVO apply(String me, String target, String message) {
        String raw = target == null ? "" : target.trim();
        if (raw.isEmpty()) {
            // 请求体里没给目标：属于入参校验，不是关系裁决，留在用例里（同改造前）
            throw new BusinessException(400, "想加谁？手机号或用户名不能为空");
        }
        // 用户名统一小写；对方必须是手机号注册过的合法用户
        String targetName = accounts.normalizeUsername(raw);
        // 申请单先建起来：「不能添加自己为好友」这条闸门的位置沿用改造前（在查用户目录之前）
        FriendRequest request = rule(() -> FriendRequest.offer(me, targetName, System.currentTimeMillis()));
        guard(() -> FriendshipGate.requireKnownAccountToApply(accounts.exists(targetName)));
        boolean alreadyFriends = friendRepository.findByOwnerAndFriend(me, targetName).isPresent();
        boolean pendingOut = requestRepository.findPendingBetween(me, targetName).isPresent();
        boolean pendingIn = requestRepository.findPendingBetween(targetName, me).isPresent();
        guard(() -> FriendshipGate.requireApplyPossible(alreadyFriends, pendingOut, pendingIn));
        guard(() -> request.attachMessage(message));
        requestRepository.save(request);
        push.pushFriendEvent("friend-request", targetName, request.id());
        return FriendRequestVO.of(request);
    }

    /**
     * 按输入关键字给出候选用户名（匹配用户名或手机号，忽略大小写），最多 10 条。
     * 关系值：available / friend / pending-out / pending-in
     */
    public List<UserSuggestion> suggest(String me, String keyword) {
        List<UserSuggestion> result = new ArrayList<>();
        for (AccountDirectory.Account candidate : accounts.search(keyword, me, 10)) {
            result.add(new UserSuggestion(candidate.username(), candidate.maskedPhone(),
                    relation(me, candidate.username())));
        }
        return result;
    }

    private String relation(String me, String name) {
        if (friendRepository.findByOwnerAndFriend(me, name).isPresent()) {
            return "friend";
        }
        if (requestRepository.findPendingBetween(me, name).isPresent()) {
            return "pending-out";
        }
        if (requestRepository.findPendingBetween(name, me).isPresent()) {
            return "pending-in";
        }
        return "available";
    }

    @Transactional
    public void accept(String me, String requestId) {
        FriendRequest request = requireRequest(requestId);
        guard(() -> request.acceptBy(me, System.currentTimeMillis()));
        requestRepository.save(request);

        String from = request.fromUser();
        ensureFriendEdge(from, me);
        ensureFriendEdge(me, from);

        // 系统消息进入双方会话流（收件人视角未读 +1）
        PrivateMessage system = rule(() -> PrivateMessage.offer(me, from, "我们已经成为好友，现在开始聊天吧！",
                PrivateMessage.TYPE_SYSTEM, System.currentTimeMillis()));
        messageRepository.save(system);
        push.pushDm(system);
        push.pushFriendEvent("friend-accepted", from, request.id());
    }

    public void reject(String me, String requestId) {
        FriendRequest request = requireRequest(requestId);
        guard(() -> request.rejectBy(me, System.currentTimeMillis()));
        requestRepository.save(request);
    }

    public List<FriendRequestVO> incoming(String me) {
        return requestRepository.listIncoming(me).stream().map(FriendRequestVO::of).toList();
    }

    public List<FriendRequestVO> outgoing(String me) {
        return requestRepository.listOutgoing(me).stream().map(FriendRequestVO::of).toList();
    }

    /** 拥有者对自己那条边的私设：备注/分组/置顶/免打扰/拉黑，请求里没传的字段一律不动。 */
    public void updateFriend(String me, String friendId, String remark, Boolean pinned, Boolean muted,
                             String tag, Boolean blocked) {
        Friend row = requireOwnFriend(me, friendId);
        if (remark != null) {
            guard(() -> row.changeRemark(remark));
        }
        if (tag != null) {
            guard(() -> row.changeTag(tag));
        }
        if (blocked != null) {
            row.changeBlocked(blocked);
        }
        if (pinned != null) {
            row.changePinned(pinned);
        }
        if (muted != null) {
            row.changeMuted(muted);
        }
        friendRepository.save(row);
    }

    public void deleteFriend(String me, String friendId) {
        Friend row = requireOwnFriend(me, friendId);
        String peer = row.friendUsername();
        friendRepository.deletePair(me, peer);
        push.pushFriendEvent("friend-deleted", peer, row.id());
    }

    /** 边是自己的才继续；别人的边与不存在的边同样回「好友不存在」，不透露归属（口径沿用改造前）。 */
    private Friend requireOwnFriend(String me, String friendId) {
        Friend row = rule(() -> friendRepository.find(friendId)
                .orElseThrow(() -> RuleViolation.notFound("好友不存在")));
        guard(() -> row.requireOwnedBy(me));
        return row;
    }

    private FriendRequest requireRequest(String requestId) {
        return rule(() -> requestRepository.find(requestId)
                .orElseThrow(() -> RuleViolation.notFound("申请不存在")));
    }

    /** 同意时补一条边：已经有就不重复建（唯一键 uq_friend_pair 也拦着）。 */
    private void ensureFriendEdge(String owner, String friend) {
        Optional<Friend> existing = friendRepository.findByOwnerAndFriend(owner, friend);
        if (existing.isEmpty()) {
            friendRepository.save(Friend.add(owner, friend, System.currentTimeMillis()));
        }
    }
}
