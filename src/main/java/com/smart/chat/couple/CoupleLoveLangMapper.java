package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoupleLoveLangMapper extends BaseMapperCompat<CoupleLoveLang> {

    /** 某人的爱语测评结果。 */
    default CoupleLoveLang find(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleLoveLang>()
                .eq(CoupleLoveLang::getSpaceId, spaceId)
                .eq(CoupleLoveLang::getFromUser, fromUser));
    }
}
