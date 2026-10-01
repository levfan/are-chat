package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F318 情绪药友数据访问。 */
@Mapper
public interface CoupleBodyMedMapper extends BaseMapperCompat<CoupleBodyMed> {

    /** 本周本人一行（uk 保证最多一条）。 */
    default CoupleBodyMed findByWeekUser(String spaceId, String week, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleBodyMed>()
                .eq(CoupleBodyMed::getSpaceId, spaceId)
                .eq(CoupleBodyMed::getWeek, week)
                .eq(CoupleBodyMed::getFromUser, fromUser));
    }

    /** fromWeek 起的周记（旧→新，近 N 周回看用）。 */
    default List<CoupleBodyMed> findRecent(String spaceId, String fromWeek) {
        return selectList(new LambdaQueryWrapper<CoupleBodyMed>()
                .eq(CoupleBodyMed::getSpaceId, spaceId)
                .ge(CoupleBodyMed::getWeek, fromWeek)
                .orderByAsc(CoupleBodyMed::getWeek));
    }
}
