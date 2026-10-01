package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleReunionLetterMapper extends BaseMapperCompat<CoupleReunionLetter> {

    /** 空间的见面信（未拆的在前）。 */
    default List<CoupleReunionLetter> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleReunionLetter>()
                .eq(CoupleReunionLetter::getSpaceId, spaceId)
                .orderByAsc(CoupleReunionLetter::getStatus)
                .orderByDesc(CoupleReunionLetter::getCreated)
                .last("LIMIT 50"));
    }
}
