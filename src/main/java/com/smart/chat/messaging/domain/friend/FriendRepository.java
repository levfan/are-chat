package com.smart.chat.messaging.domain.friend;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 好友边的仓储端口：只说领域语言（谁是拥有者、谁是对方、成对删除、按人刷新在线时间）。
 * <p>
 * 未读数走 {@link #unreadCountByPeer}：那条 JOIN 聚合是联系人列表的既有口径（一趟算完，
 * 不允许逐条查），返回的是读投影 Map 而不是聚合，见 05 第 2.3 条。
 */
public interface FriendRepository {

    /** 按主键取一条边（归属还没确认，所以返回之后必须再走 {@link Friend#requireOwnedBy}） */
    Optional<Friend> find(String id);

    /** 拥有者视角的那条边 */
    Optional<Friend> findByOwnerAndFriend(String owner, String friend);

    /** 我的全部好友（联系人列表驱动表） */
    List<Friend> findAllByOwner(String owner);

    /** 新边落库；已有边只回写聚合持有的列（last_seen_at 等不归它管，见 {@link Friend}） */
    void save(Friend friend);

    /** 删除两个人的双向两条边——只删一边会留下半条关系 */
    void deletePair(String one, String other);

    /** 账号注销：摘掉与这个人相关的全部边（两个方向都算） */
    void deleteAllEdgesOf(String username);

    /** 刷新某人在所有好友列表里的「最近在线」时间（批量 UPDATE，不经过 save） */
    void touchLastSeenOf(String username, long at);

    /** 联系人列表专用：一趟算出每个对端的未读数（无消息时为 0） */
    Map<String, Long> unreadCountByPeer(String owner);
}
