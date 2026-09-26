package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouplePromiseMapper extends BaseMapperCompat<CouplePromise> {

    default List<CouplePromise> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePromise>()
                .eq(CouplePromise::getSpaceId, spaceId)
                .orderByDesc(CouplePromise::getCreated));
    }

    /** 待兑现且设置了截止时间，用于逾期提醒任务。 */
    default List<CouplePromise> findPendingWithDueBefore(long now) {
        return selectList(new LambdaQueryWrapper<CouplePromise>()
                .eq(CouplePromise::getStatus, CouplePromise.STATUS_PENDING)
                .isNotNull(CouplePromise::getDueAt)
                .lt(CouplePromise::getDueAt, now));
    }

    /** 84 注销清理：删除某空间下的全部约定。 */
    default void deleteBySpace(String spaceId) {
        delete(new LambdaQueryWrapper<CouplePromise>().eq(CouplePromise::getSpaceId, spaceId));
    }
}
