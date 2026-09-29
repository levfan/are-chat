package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleChoreMapper extends BaseMapperCompat<CoupleChore> {

    /** 某空间全部家务（新→旧）。 */
    default List<CoupleChore> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleChore>()
                .eq(CoupleChore::getSpaceId, spaceId)
                .orderByDesc(CoupleChore::getCreated));
    }
}
