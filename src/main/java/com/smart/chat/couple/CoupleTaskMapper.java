package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoupleTaskMapper extends BaseMapperCompat<CoupleTask> {

    /** 某人某天的任务卡（无则空）。 */
    default CoupleTask find(String spaceId, String taskDay, String username) {
        return selectOne(new LambdaQueryWrapper<CoupleTask>()
                .eq(CoupleTask::getSpaceId, spaceId)
                .eq(CoupleTask::getTaskDay, taskDay)
                .eq(CoupleTask::getUsername, username));
    }

    /** 某空间已完成的任务数（成就/心动值用）。 */
    default long countDone(String spaceId) {
        return selectCount(new LambdaQueryWrapper<CoupleTask>()
                .eq(CoupleTask::getSpaceId, spaceId)
                .eq(CoupleTask::getStatus, CoupleTask.STATUS_DONE));
    }
}
