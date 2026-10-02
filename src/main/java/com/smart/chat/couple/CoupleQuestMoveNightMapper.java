package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F375 新家第一晚数据访问（uk(space_id,day) 每晚一行，双方各点自己的列）。 */
@Mapper
public interface CoupleQuestMoveNightMapper extends BaseMapperCompat<CoupleQuestMoveNight> {

    /** 空间全部第一晚（新的在前，回看家用）。 */
    default List<CoupleQuestMoveNight> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestMoveNight>()
                .eq(CoupleQuestMoveNight::getSpaceId, spaceId)
                .orderByDesc(CoupleQuestMoveNight::getDay));
    }

    /** 那晚那行（双方共写一行）。 */
    default CoupleQuestMoveNight findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestMoveNight>()
                .eq(CoupleQuestMoveNight::getSpaceId, spaceId)
                .eq(CoupleQuestMoveNight::getDay, day));
    }
}
