package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSongMapper extends BaseMapperCompat<CoupleSong> {

    /** 空间的歌单（新→旧）。 */
    default List<CoupleSong> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSong>()
                .eq(CoupleSong::getSpaceId, spaceId)
                .orderByDesc(CoupleSong::getCreated));
    }
}
