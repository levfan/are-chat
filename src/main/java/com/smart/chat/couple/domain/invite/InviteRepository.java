package com.smart.chat.couple.domain.invite;

import java.util.List;
import java.util.Optional;

/** 邀请聚合的仓储端口。 */
public interface InviteRepository {

    /** 按 id 取邀请；取不到由用例层决定是 404 还是别的 */
    Optional<Invite> findById(String id);

    /** 发给某人、还没处理的邀请（新→旧） */
    List<Invite> findPendingTo(String username);

    /** 某人发出去、还没被处理的邀请（新→旧） */
    List<Invite> findPendingFrom(String username);

    /** 两人之间是否已有待处理邀请（方向任意）——发起前的重复闸门 */
    Optional<Invite> findPendingBetween(String left, String right);

    /** 新建则插入、已有则整行回写（本聚合镜像 couple_invite 的全部列） */
    void save(Invite invite);

    /** 账号注销时清掉与他相关的全部邀请 */
    void deleteAllInvolving(String username);
}
