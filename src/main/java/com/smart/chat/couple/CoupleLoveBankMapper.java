package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleLoveBankMapper extends BaseMapperCompat<CoupleLoveBank> {

    /** 某人存入的情话（新→旧）。 */
    default List<CoupleLoveBank> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleLoveBank>()
                .eq(CoupleLoveBank::getSpaceId, spaceId)
                .eq(CoupleLoveBank::getFromUser, fromUser)
                .orderByDesc(CoupleLoveBank::getCreated));
    }

    /** 空间里全部未投递的情话（利息推送扫描用）。 */
    default List<CoupleLoveBank> findUndelivered(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLoveBank>()
                .eq(CoupleLoveBank::getSpaceId, spaceId)
                .eq(CoupleLoveBank::isDelivered, false)
                .orderByAsc(CoupleLoveBank::getCreated));
    }
}
