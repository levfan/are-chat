package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

/** F269 称呼日数据访问。 */
@Mapper
public interface CoupleNameDayMapper extends BaseMapperCompat<CoupleNameDay> {

    /** 当天行。 */
    default CoupleNameDay find(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleNameDay>()
                .eq(CoupleNameDay::getSpaceId, spaceId)
                .eq(CoupleNameDay::getDay, day));
    }
}
