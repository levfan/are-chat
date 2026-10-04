package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CoupleSpaceMapper extends BaseMapperCompat<CoupleSpacePO> {

    /** 我当前生效（未解除）的情侣空间。 */
    default Optional<CoupleSpacePO> findActiveByUser(String username) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<CoupleSpacePO>()
                .eq(CoupleSpacePO::getStatus, CoupleSpacePO.STATUS_ACTIVE)
                .and(w -> w.eq(CoupleSpacePO::getUserA, username).or().eq(CoupleSpacePO::getUserB, username))
                .last("LIMIT 1")));
    }

    /** 全部生效中的情侣空间（逾期提醒任务过滤用）。 */
    default List<CoupleSpacePO> findAllActive() {
        return selectList(new LambdaQueryWrapper<CoupleSpacePO>()
                .eq(CoupleSpacePO::getStatus, CoupleSpacePO.STATUS_ACTIVE));
    }
}
