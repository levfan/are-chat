package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F274 叫醒服务数据访问。 */
@Mapper
public interface CoupleWakeWordMapper extends BaseMapperCompat<CoupleWakeWord> {

    /** 本周某人一条。 */
    default CoupleWakeWord find(String spaceId, String week, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleWakeWord>()
                .eq(CoupleWakeWord::getSpaceId, spaceId)
                .eq(CoupleWakeWord::getWeek, week)
                .eq(CoupleWakeWord::getFromUser, fromUser));
    }

    /** 全部在途叫醒卡（本周）。 */
    default List<CoupleWakeWord> findByWeek(String spaceId, String week) {
        return selectList(new LambdaQueryWrapper<CoupleWakeWord>()
                .eq(CoupleWakeWord::getSpaceId, spaceId)
                .eq(CoupleWakeWord::getWeek, week));
    }
}
