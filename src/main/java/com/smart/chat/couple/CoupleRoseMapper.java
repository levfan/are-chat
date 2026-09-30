package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleRoseMapper extends BaseMapperCompat<CoupleRose> {

    /** 某天空间里收到的全部玫瑰。 */
    default List<CoupleRose> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleRose>()
                .eq(CoupleRose::getSpaceId, spaceId)
                .eq(CoupleRose::getDay, day)
                .orderByAsc(CoupleRose::getCreated));
    }

    /** 我某天已送的玫瑰数（每天限量校验）。 */
    default long countByUserAndDay(String spaceId, String fromUser, String day) {
        return selectCount(new LambdaQueryWrapper<CoupleRose>()
                .eq(CoupleRose::getSpaceId, spaceId)
                .eq(CoupleRose::getFromUser, fromUser)
                .eq(CoupleRose::getDay, day));
    }

    /** 最近 N 天的玫瑰（新→旧）。 */
    default List<CoupleRose> findRecent(String spaceId, int limit) {
        return selectList(new LambdaQueryWrapper<CoupleRose>()
                .eq(CoupleRose::getSpaceId, spaceId)
                .orderByDesc(CoupleRose::getCreated))
                .stream()
                .limit(limit)
                .toList();
    }
}
