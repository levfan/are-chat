package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface FriendMapper extends BaseMapperCompat<FriendPO> {

    /**
     * 联系人列表专用：一趟算完每个好友的未读数。
     * <p>阈值 {@code last_read_at} 存在 friend 行上、计数在 private_message 上，两边条件不同，
     * 只能靠这次 JOIN 聚合；驱动表是好友（几十行），探测侧走 V49 的
     * {@code idx_pm_to_from_created (to_user, from_user, created)}。
     * <p>原先是每人一次 {@code countUnread}，N 个好友 N 次全表扫（实测 59 人一趟 ~48ms，
     * 对比逐次 ~210ms/次）。
     *
     * @return 每行 {@code peer}（对端用户名）与 {@code unread}（未读条数，无消息时为 0）
     */
    @Select("""
            SELECT f.friend_username AS peer, COUNT(m.id) AS unread
            FROM friend f
            LEFT JOIN private_message m
                   ON m.from_user = f.friend_username
                  AND m.to_user = f.owner_username
                  AND m.status = 'SENT'
                  AND m.created > COALESCE(f.last_read_at, 0)
            WHERE f.owner_username = #{owner}
            GROUP BY f.friend_username
            """)
    List<Map<String, Object>> selectUnreadCountsByPeer(String owner);

    default Optional<FriendPO> findByOwnerAndFriend(String owner, String friend) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<FriendPO>()
                .eq(FriendPO::getOwnerUsername, owner)
                .eq(FriendPO::getFriendUsername, friend)
                .last("LIMIT 1")));
    }

    default List<FriendPO> findAllByOwner(String owner) {
        return selectList(new LambdaQueryWrapper<FriendPO>()
                .eq(FriendPO::getOwnerUsername, owner));
    }

    /** 删除双向好友关系。 */
    default void deletePair(String a, String b) {
        delete(new LambdaQueryWrapper<FriendPO>()
                .eq(FriendPO::getOwnerUsername, a)
                .eq(FriendPO::getFriendUsername, b));
        delete(new LambdaQueryWrapper<FriendPO>()
                .eq(FriendPO::getOwnerUsername, b)
                .eq(FriendPO::getFriendUsername, a));
    }

    /** 84 注销清理：删除以该用户为发起人的全部好友关系 */
    default void deleteAllByOwner(String owner) {
        delete(new LambdaQueryWrapper<FriendPO>().eq(FriendPO::getOwnerUsername, owner));
    }

    /** 84 注销清理：删除以该用户为对方的其他人的好友关系 */
    default void deleteAllByFriend(String friendUsername) {
        delete(new LambdaQueryWrapper<FriendPO>().eq(FriendPO::getFriendUsername, friendUsername));
    }

    /** 刷新某用户在所有好友列表里的「最近在线」时间。 */
    default void updateLastSeenByUsername(String friendUsername, Long lastSeenAt) {
        update(null, new LambdaUpdateWrapper<FriendPO>()
                .eq(FriendPO::getFriendUsername, friendUsername)
                .set(FriendPO::getLastSeenAt, lastSeenAt));
    }
}
