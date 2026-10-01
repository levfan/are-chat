package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleJournalMapper extends BaseMapperCompat<CoupleJournal> {

    /** 全部手账（按日期倒序）。 */
    default List<CoupleJournal> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleJournal>()
                .eq(CoupleJournal::getSpaceId, spaceId)
                .orderByDesc(CoupleJournal::getDay)
                .orderByAsc(CoupleJournal::getCreated));
    }

    /** 某人某天的手账页。 */
    default CoupleJournal find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleJournal>()
                .eq(CoupleJournal::getSpaceId, spaceId)
                .eq(CoupleJournal::getDay, day)
                .eq(CoupleJournal::getFromUser, fromUser));
    }
}
