package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F255 节气手账数据访问。 */
@Mapper
public interface CoupleTermNoteMapper extends BaseMapperCompat<CoupleTermNote> {

    /** 某人某年某节气的一笔。 */
    default CoupleTermNote find(String spaceId, String term, String year, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleTermNote>()
                .eq(CoupleTermNote::getSpaceId, spaceId)
                .eq(CoupleTermNote::getTerm, term)
                .eq(CoupleTermNote::getYear, year)
                .eq(CoupleTermNote::getFromUser, fromUser));
    }

    /** 某年全部手账。 */
    default List<CoupleTermNote> findByYear(String spaceId, String year) {
        return selectList(new LambdaQueryWrapper<CoupleTermNote>()
                .eq(CoupleTermNote::getSpaceId, spaceId)
                .eq(CoupleTermNote::getYear, year));
    }
}
