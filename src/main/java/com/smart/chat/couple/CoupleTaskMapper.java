package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleTaskMapper extends BaseMapperCompat<CoupleTask> {

    /** 某人某天的任务卡（无则空）。 */
    default CoupleTask find(String spaceId, String taskDay, String username) {
        return selectOne(new LambdaQueryWrapper<CoupleTask>()
                .eq(CoupleTask::getSpaceId, spaceId)
                .eq(CoupleTask::getTaskDay, taskDay)
                .eq(CoupleTask::getUsername, username));
    }

    /** 某空间全部任务卡（月报用，日期新→旧）。 */
    default List<CoupleTask> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTask>()
                .eq(CoupleTask::getSpaceId, spaceId)
                .orderByDesc(CoupleTask::getTaskDay));
    }

    /** 某空间已完成的任务数（成就/心动值用）。 */
    default long countDone(String spaceId) {
        return selectCount(new LambdaQueryWrapper<CoupleTask>()
                .eq(CoupleTask::getSpaceId, spaceId)
                .eq(CoupleTask::getStatus, CoupleTask.STATUS_DONE));
    }
}
