package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleHiddenAchievementMapper extends BaseMapperCompat<CoupleHiddenAchievement> {

    /** 已解锁的隐藏成就。 */
    default List<CoupleHiddenAchievement> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleHiddenAchievement>()
                .eq(CoupleHiddenAchievement::getSpaceId, spaceId)
                .orderByDesc(CoupleHiddenAchievement::getCreated));
    }
}
