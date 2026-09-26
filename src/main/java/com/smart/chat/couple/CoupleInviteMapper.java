package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CoupleInviteMapper extends BaseMapperCompat<CoupleInvite> {

    default List<CoupleInvite> findPendingTo(String toUser) {
        return selectList(new LambdaQueryWrapper<CoupleInvite>()
                .eq(CoupleInvite::getToUser, toUser)
                .eq(CoupleInvite::getStatus, CoupleInvite.STATUS_PENDING)
                .orderByDesc(CoupleInvite::getCreated));
    }

    default List<CoupleInvite> findPendingFrom(String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleInvite>()
                .eq(CoupleInvite::getFromUser, fromUser)
                .eq(CoupleInvite::getStatus, CoupleInvite.STATUS_PENDING)
                .orderByDesc(CoupleInvite::getCreated));
    }

    /** 双方之间（任意方向）是否已有待处理邀请。 */
    default Optional<CoupleInvite> findPendingBetween(String left, String right) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<CoupleInvite>()
                .eq(CoupleInvite::getStatus, CoupleInvite.STATUS_PENDING)
                .and(w -> w
                        .and(x -> x.eq(CoupleInvite::getFromUser, left).eq(CoupleInvite::getToUser, right))
                        .or()
                        .and(x -> x.eq(CoupleInvite::getFromUser, right).eq(CoupleInvite::getToUser, left)))
                .last("LIMIT 1")));
    }

    /** 84 注销清理：删除与该用户相关的所有情侣邀请。 */
    default void deleteAllInvolving(String username) {
        delete(new LambdaQueryWrapper<CoupleInvite>()
                .and(w -> w.eq(CoupleInvite::getFromUser, username).or().eq(CoupleInvite::getToUser, username)));
    }
}
