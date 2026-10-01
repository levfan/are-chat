package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoupleWeekHostMapper extends BaseMapperCompat<CoupleWeekHost> {

    /** 某周的轮值记录。 */
    default CoupleWeekHost find(String spaceId, String week) {
        return selectOne(new LambdaQueryWrapper<CoupleWeekHost>()
                .eq(CoupleWeekHost::getSpaceId, spaceId)
                .eq(CoupleWeekHost::getWeek, week));
    }
}
