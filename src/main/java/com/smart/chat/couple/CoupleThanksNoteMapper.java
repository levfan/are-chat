package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleThanksNoteMapper extends BaseMapperCompat<CoupleThanksNote> {

    /** 全部感恩便签（按创建倒序）。 */
    default List<CoupleThanksNote> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleThanksNote>()
                .eq(CoupleThanksNote::getSpaceId, spaceId)
                .orderByDesc(CoupleThanksNote::getCreated));
    }
}
