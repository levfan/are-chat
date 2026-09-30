package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleMysteryBoxMapper extends BaseMapperCompat<CoupleMysteryBox> {

    /** 空间的盲盒列表（新→旧）。 */
    default List<CoupleMysteryBox> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleMysteryBox>()
                .eq(CoupleMysteryBox::getSpaceId, spaceId)
                .orderByDesc(CoupleMysteryBox::getCreated));
    }
}
