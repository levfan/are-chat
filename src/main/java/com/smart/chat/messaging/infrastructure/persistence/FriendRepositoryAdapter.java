package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.friend.Friend;
import com.smart.chat.messaging.domain.friend.FriendRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link FriendRepository} 的 MyBatis-Plus 适配器。
 * <p>
 * 更新时<b>只回写聚合持有的那几列</b>：{@code last_seen_at} 由 {@link #touchLastSeenOf} 按人批量刷新
 * （WS 上下线那条 UPDATE 一次改很多人的行），聚合只是读它来显示「x 分钟前在线」；
 * 若用聚合重建整行，一次备注修改就会把对方刚刷新的在线时间清成 null。所以先 {@code selectById} 取回原行，
 * 改完再写回去（同 {@code CoupleSpaceRepositoryAdapter} 已确立的纪律）。
 */
@Component
public class FriendRepositoryAdapter implements FriendRepository {

    private final FriendMapper friendMapper;

    public FriendRepositoryAdapter(FriendMapper friendMapper) {
        this.friendMapper = friendMapper;
    }

    @Override
    public Optional<Friend> find(String id) {
        return Optional.ofNullable(friendMapper.selectById(id)).map(FriendRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Friend> findByOwnerAndFriend(String owner, String friend) {
        return friendMapper.findByOwnerAndFriend(owner, friend).map(FriendRepositoryAdapter::toDomain);
    }

    @Override
    public List<Friend> findAllByOwner(String owner) {
        return friendMapper.findAllByOwner(owner).stream().map(FriendRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(Friend friend) {
        FriendPO existing = friendMapper.selectById(friend.id());
        if (existing == null) {
            FriendPO po = new FriendPO();
            po.setId(friend.id());
            po.setOwnerUsername(friend.ownerUsername());
            po.setFriendUsername(friend.friendUsername());
            po.setRemark(friend.remark());
            po.setTag(friend.tag());
            po.setPinned(friend.pinned());
            po.setMuted(friend.muted());
            po.setBlocked(friend.blocked());
            po.setLastReadAt(friend.lastReadAt());
            po.setLastSeenAt(friend.lastSeenAt());
            po.setCreated(friend.created());
            friendMapper.insert(po);
            return;
        }
        applyOwnedFields(existing, friend);
        friendMapper.updateById(existing);
    }

    /** 聚合负责维护的列；owner/friend/created 是边的身份、last_seen_at 归批量刷新，这里一律不碰 */
    private static void applyOwnedFields(FriendPO po, Friend friend) {
        po.setRemark(friend.remark());
        po.setTag(friend.tag());
        po.setPinned(friend.pinned());
        po.setMuted(friend.muted());
        po.setBlocked(friend.blocked());
        po.setLastReadAt(friend.lastReadAt());
    }

    @Override
    public void deletePair(String one, String other) {
        friendMapper.deletePair(one, other);
    }

    @Override
    public void deleteAllEdgesOf(String username) {
        friendMapper.deleteAllByOwner(username);
        friendMapper.deleteAllByFriend(username);
    }

    @Override
    public void touchLastSeenOf(String username, long at) {
        friendMapper.updateLastSeenByUsername(username, at);
    }

    @Override
    public Map<String, Long> unreadCountByPeer(String owner) {
        Map<String, Long> unread = new HashMap<>();
        for (Map<String, Object> row : friendMapper.selectUnreadCountsByPeer(owner)) {
            if (row.get("unread") instanceof Number number) {
                unread.put(String.valueOf(row.get("peer")), number.longValue());
            }
        }
        return unread;
    }

    private static Friend toDomain(FriendPO po) {
        return Friend.restore(po.getId(), po.getOwnerUsername(), po.getFriendUsername(), po.getRemark(), po.getTag(),
                po.getPinned(), po.getMuted(), po.getBlocked(), po.getLastReadAt(), po.getLastSeenAt(),
                po.getCreated());
    }
}
