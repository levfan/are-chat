package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleArtGalleryMapper extends BaseMapperCompat<CoupleArtGallery> {

    /** 空间的画廊（新的在前）。 */
    default List<CoupleArtGallery> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleArtGallery>()
                .eq(CoupleArtGallery::getSpaceId, spaceId)
                .orderByDesc(CoupleArtGallery::getCreated)
                .last("LIMIT 30"));
    }
}
