package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface FriendRequestMapper extends BaseMapperCompat<FriendRequestPO> {

    /**
     * 收到的待处理申请（只返回 PENDING）。
     * 已同意/已拒绝的记录不再出现在列表里，否则前端会一直显示未读徽标和处理按钮。
     */
    default List<FriendRequestPO> findIncoming(String me) {
        return selectList(new LambdaQueryWrapper<FriendRequestPO>()
                .eq(FriendRequestPO::getToUser, me)
                .eq(FriendRequestPO::getStatus, FriendRequestPO.STATUS_PENDING)
                .orderByDesc(FriendRequestPO::getCreated));
    }

    /** 我发出的待处理申请（只返回 PENDING，对应「等待对方同意」） */
    default List<FriendRequestPO> findOutgoing(String me) {
        return selectList(new LambdaQueryWrapper<FriendRequestPO>()
                .eq(FriendRequestPO::getFromUser, me)
                .eq(FriendRequestPO::getStatus, FriendRequestPO.STATUS_PENDING)
                .orderByDesc(FriendRequestPO::getCreated));
    }

    default Optional<FriendRequestPO> findPendingBetween(String a, String b) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<FriendRequestPO>()
                .eq(FriendRequestPO::getStatus, FriendRequestPO.STATUS_PENDING)
                .and(w -> w.eq(FriendRequestPO::getFromUser, a).eq(FriendRequestPO::getToUser, b))
                .last("LIMIT 1")));
    }

    default Optional<FriendRequestPO> findAnyPendingBetween(String a, String b) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<FriendRequestPO>()
                .eq(FriendRequestPO::getStatus, FriendRequestPO.STATUS_PENDING)
                .and(w -> w
                        .and(w1 -> w1.eq(FriendRequestPO::getFromUser, a).eq(FriendRequestPO::getToUser, b))
                        .or(w2 -> w2.eq(FriendRequestPO::getFromUser, b).eq(FriendRequestPO::getToUser, a)))
                .last("LIMIT 1")));
    }
}
