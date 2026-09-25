package com.smart.chat.im;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface FriendRequestMapper extends BaseMapperCompat<FriendRequest> {

    /**
     * 收到的待处理申请（只返回 PENDING）。
     * 已同意/已拒绝的记录不再出现在列表里，否则前端会一直显示未读徽标和处理按钮。
     */
    default List<FriendRequest> findIncoming(String me) {
        return selectList(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getToUser, me)
                .eq(FriendRequest::getStatus, FriendRequest.STATUS_PENDING)
                .orderByDesc(FriendRequest::getCreated));
    }

    /** 我发出的待处理申请（只返回 PENDING，对应「等待对方同意」） */
    default List<FriendRequest> findOutgoing(String me) {
        return selectList(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getFromUser, me)
                .eq(FriendRequest::getStatus, FriendRequest.STATUS_PENDING)
                .orderByDesc(FriendRequest::getCreated));
    }

    default Optional<FriendRequest> findPendingBetween(String a, String b) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getStatus, FriendRequest.STATUS_PENDING)
                .and(w -> w.eq(FriendRequest::getFromUser, a).eq(FriendRequest::getToUser, b))
                .last("LIMIT 1")));
    }

    default Optional<FriendRequest> findAnyPendingBetween(String a, String b) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<FriendRequest>()
                .eq(FriendRequest::getStatus, FriendRequest.STATUS_PENDING)
                .and(w -> w
                        .and(w1 -> w1.eq(FriendRequest::getFromUser, a).eq(FriendRequest::getToUser, b))
                        .or(w2 -> w2.eq(FriendRequest::getFromUser, b).eq(FriendRequest::getToUser, a)))
                .last("LIMIT 1")));
    }
}
