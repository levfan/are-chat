package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleItemMapper extends BaseMapperCompat<CoupleItem> {

    default List<CoupleItem> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleItem>()
                .eq(CoupleItem::getSpaceId, spaceId)
                .orderByAsc(CoupleItem::getDone)
                .orderByAsc(CoupleItem::getDueDate)
                .orderByDesc(CoupleItem::getCreated));
    }

    /** 84 注销清理。 */
    default void deleteBySpace(String spaceId) {
        delete(new LambdaQueryWrapper<CoupleItem>().eq(CoupleItem::getSpaceId, spaceId));
    }
}
