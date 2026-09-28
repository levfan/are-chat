package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoupleCycleMapper extends BaseMapperCompat<CoupleCycle> {

    /** 某人的生理期记录（无则空）。 */
    default CoupleCycle find(String spaceId, String username) {
        return selectOne(new LambdaQueryWrapper<CoupleCycle>()
                .eq(CoupleCycle::getSpaceId, spaceId)
                .eq(CoupleCycle::getUsername, username));
    }
}
