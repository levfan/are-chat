package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F224 冷暖互报数据访问。 */
@Mapper
public interface CoupleCozyWeatherMapper extends BaseMapperCompat<CoupleCozyWeather> {

    /** 某人某天的一条。 */
    default CoupleCozyWeather find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCozyWeather>()
                .eq(CoupleCozyWeather::getSpaceId, spaceId)
                .eq(CoupleCozyWeather::getDay, day)
                .eq(CoupleCozyWeather::getFromUser, fromUser));
    }
}
