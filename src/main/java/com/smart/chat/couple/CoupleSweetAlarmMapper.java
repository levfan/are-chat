package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSweetAlarmMapper extends BaseMapperCompat<CoupleSweetAlarm> {

    /** 我设的闹钟（新→旧）。 */
    default List<CoupleSweetAlarm> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleSweetAlarm>()
                .eq(CoupleSweetAlarm::getSpaceId, spaceId)
                .eq(CoupleSweetAlarm::getFromUser, fromUser)
                .orderByDesc(CoupleSweetAlarm::getCreated));
    }

    /** 到点未触发的闹钟（定时任务扫描用）。 */
    default List<CoupleSweetAlarm> findDueUnfired(long now) {
        return selectList(new LambdaQueryWrapper<CoupleSweetAlarm>()
                .eq(CoupleSweetAlarm::isFired, false)
                .le(CoupleSweetAlarm::getFireAt, now)
                .orderByAsc(CoupleSweetAlarm::getFireAt));
    }
}
