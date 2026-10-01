package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDelayTaskMapper extends BaseMapperCompat<CoupleDelayTask> {

    /** 空间内拖延事（进行中优先，按创建倒序）。 */
    default List<CoupleDelayTask> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDelayTask>()
                .eq(CoupleDelayTask::getSpaceId, spaceId)
                .orderByDesc(CoupleDelayTask::getCreated));
    }
}
