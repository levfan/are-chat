package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleFoodNoteMapper extends BaseMapperCompat<CoupleFoodNote> {

    /** 空间的美食记录（新的在前）。 */
    default List<CoupleFoodNote> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleFoodNote>()
                .eq(CoupleFoodNote::getSpaceId, spaceId)
                .orderByDesc(CoupleFoodNote::getCreated)
                .last("LIMIT 50"));
    }
}
