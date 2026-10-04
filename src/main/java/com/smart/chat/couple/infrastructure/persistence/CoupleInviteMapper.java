package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CoupleInviteMapper extends BaseMapperCompat<CoupleInvitePO> {

    default List<CoupleInvitePO> findPendingTo(String toUser) {
        return selectList(new LambdaQueryWrapper<CoupleInvitePO>()
                .eq(CoupleInvitePO::getToUser, toUser)
                .eq(CoupleInvitePO::getStatus, CoupleInvitePO.STATUS_PENDING)
                .orderByDesc(CoupleInvitePO::getCreated));
    }

    default List<CoupleInvitePO> findPendingFrom(String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleInvitePO>()
                .eq(CoupleInvitePO::getFromUser, fromUser)
                .eq(CoupleInvitePO::getStatus, CoupleInvitePO.STATUS_PENDING)
                .orderByDesc(CoupleInvitePO::getCreated));
    }

    /** 双方之间（任意方向）是否已有待处理邀请。 */
    default Optional<CoupleInvitePO> findPendingBetween(String left, String right) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<CoupleInvitePO>()
                .eq(CoupleInvitePO::getStatus, CoupleInvitePO.STATUS_PENDING)
                .and(w -> w
                        .and(x -> x.eq(CoupleInvitePO::getFromUser, left).eq(CoupleInvitePO::getToUser, right))
                        .or()
                        .and(x -> x.eq(CoupleInvitePO::getFromUser, right).eq(CoupleInvitePO::getToUser, left)))
                .last("LIMIT 1")));
    }

    /** 84 注销清理：删除与该用户相关的所有情侣邀请。 */
    default void deleteAllInvolving(String username) {
        delete(new LambdaQueryWrapper<CoupleInvitePO>()
                .and(w -> w.eq(CoupleInvitePO::getFromUser, username).or().eq(CoupleInvitePO::getToUser, username)));
    }
}
