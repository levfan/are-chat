package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleCipherNoteMapper extends BaseMapperCompat<CoupleCipherNote> {

    /** 全部密码情书（未解码在前，按创建倒序）。 */
    default List<CoupleCipherNote> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCipherNote>()
                .eq(CoupleCipherNote::getSpaceId, spaceId)
                .orderByDesc(CoupleCipherNote::getCreated));
    }
}
