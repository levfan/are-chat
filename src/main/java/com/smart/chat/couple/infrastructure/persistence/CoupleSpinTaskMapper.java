package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F270 家务轮盘数据访问。 */
@Mapper
public interface CoupleSpinTaskMapper extends BaseMapperCompat<CoupleSpinTask> {

    /** 某周全部任务。 */
    default List<CoupleSpinTask> findByWeek(String spaceId, String week) {
        return selectList(new LambdaQueryWrapper<CoupleSpinTask>()
                .eq(CoupleSpinTask::getSpaceId, spaceId)
                .eq(CoupleSpinTask::getWeek, week));
    }

    /** 某周某事一条。 */
    default CoupleSpinTask find(String spaceId, String week, String item) {
        return selectOne(new LambdaQueryWrapper<CoupleSpinTask>()
                .eq(CoupleSpinTask::getSpaceId, spaceId)
                .eq(CoupleSpinTask::getWeek, week)
                .eq(CoupleSpinTask::getItem, item));
    }
}
