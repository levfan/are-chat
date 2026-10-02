package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F357 电量预报数据访问。 */
@Mapper
public interface CoupleEchoBatteryMapper extends BaseMapperCompat<CoupleEchoBattery> {

    /** 某人某天的电量格（uk(space_id,day,from_user) 最多一条，本人改写用）。 */
    default CoupleEchoBattery findByDayUser(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleEchoBattery>()
                .eq(CoupleEchoBattery::getSpaceId, spaceId)
                .eq(CoupleEchoBattery::getDay, day)
                .eq(CoupleEchoBattery::getFromUser, fromUser));
    }

    /** 某天双方电量格（总览「今日双格」用）。 */
    default List<CoupleEchoBattery> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleEchoBattery>()
                .eq(CoupleEchoBattery::getSpaceId, spaceId)
                .eq(CoupleEchoBattery::getDay, day)
                .orderByAsc(CoupleEchoBattery::getFromUser));
    }
}
