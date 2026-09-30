package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouplePassbookMapper extends BaseMapperCompat<CouplePassbook> {

    /** 某人某天的存款（每人每天一笔）。 */
    default CouplePassbook find(String spaceId, String fromUser, String day) {
        return selectOne(new LambdaQueryWrapper<CouplePassbook>()
                .eq(CouplePassbook::getSpaceId, spaceId)
                .eq(CouplePassbook::getFromUser, fromUser)
                .eq(CouplePassbook::getDay, day));
    }

    /** 空间全部存款（新→旧）。 */
    default List<CouplePassbook> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePassbook>()
                .eq(CouplePassbook::getSpaceId, spaceId)
                .orderByDesc(CouplePassbook::getDay));
    }
}
