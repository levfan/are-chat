package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CoupleSpaceMapper extends BaseMapperCompat<CoupleSpace> {

    /** 我当前生效（未解除）的情侣空间。 */
    default Optional<CoupleSpace> findActiveByUser(String username) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<CoupleSpace>()
                .eq(CoupleSpace::getStatus, CoupleSpace.STATUS_ACTIVE)
                .and(w -> w.eq(CoupleSpace::getUserA, username).or().eq(CoupleSpace::getUserB, username))
                .last("LIMIT 1")));
    }

    /** 全部生效中的情侣空间（逾期提醒任务过滤用）。 */
    default List<CoupleSpace> findAllActive() {
        return selectList(new LambdaQueryWrapper<CoupleSpace>()
                .eq(CoupleSpace::getStatus, CoupleSpace.STATUS_ACTIVE));
    }
}
