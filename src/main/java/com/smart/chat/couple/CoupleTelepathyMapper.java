package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleTelepathyMapper extends BaseMapperCompat<CoupleTelepathy> {

    /** 空间全部感应轮（新→旧）。 */
    default List<CoupleTelepathy> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTelepathy>()
                .eq(CoupleTelepathy::getSpaceId, spaceId)
                .orderByDesc(CoupleTelepathy::getCreated));
    }

    /** 某天已开的轮次数。 */
    default long countByDay(String spaceId, String day) {
        return selectCount(new LambdaQueryWrapper<CoupleTelepathy>()
                .eq(CoupleTelepathy::getSpaceId, spaceId)
                .eq(CoupleTelepathy::getDay, day));
    }
}
