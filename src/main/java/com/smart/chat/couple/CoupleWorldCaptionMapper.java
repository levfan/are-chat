package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F334 文案代写数据访问。 */
@Mapper
public interface CoupleWorldCaptionMapper extends BaseMapperCompat<CoupleWorldCaption> {

    /** 某人某日第几条候选（uk 保证最多一条）。 */
    default CoupleWorldCaption findByDayUserSlot(String spaceId, String day, String fromUser, int slot) {
        return selectOne(new LambdaQueryWrapper<CoupleWorldCaption>()
                .eq(CoupleWorldCaption::getSpaceId, spaceId)
                .eq(CoupleWorldCaption::getDay, day)
                .eq(CoupleWorldCaption::getFromUser, fromUser)
                .eq(CoupleWorldCaption::getSlot, slot));
    }

    /** 某日全部候选（交稿时间老→新）。 */
    default List<CoupleWorldCaption> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleWorldCaption>()
                .eq(CoupleWorldCaption::getSpaceId, spaceId)
                .eq(CoupleWorldCaption::getDay, day)
                .orderByAsc(CoupleWorldCaption::getCreated));
    }

    /** 空间全部候选（求稿日新→旧）。 */
    default List<CoupleWorldCaption> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldCaption>()
                .eq(CoupleWorldCaption::getSpaceId, spaceId)
                .orderByDesc(CoupleWorldCaption::getDay));
    }
}
