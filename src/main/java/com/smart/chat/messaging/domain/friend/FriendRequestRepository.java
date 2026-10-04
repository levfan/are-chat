package com.smart.chat.messaging.domain.friend;

import java.util.List;
import java.util.Optional;

/** 好友申请的仓储端口。 */
public interface FriendRequestRepository {

    /** 按主键取申请（找不到就是「申请不存在」，由 application 翻译） */
    Optional<FriendRequest> find(String id);

    /** 收到且仍待处理的申请（已同意/已拒绝不再出现，否则前端一直挂徽标） */
    List<FriendRequest> listIncoming(String me);

    /** 我发出且仍待处理的申请 */
    List<FriendRequest> listOutgoing(String me);

    /** 指定方向（{@code from → to}）上是否已有一张待处理申请 */
    Optional<FriendRequest> findPendingBetween(String from, String to);

    /** 新申请插入、已有申请按聚合持有的列更新 */
    void save(FriendRequest request);
}
