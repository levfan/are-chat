package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouplePactMapper extends BaseMapperCompat<CouplePact> {

    /** 某空间全部条约（新→旧）。 */
    default List<CouplePact> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePact>()
                .eq(CouplePact::getSpaceId, spaceId)
                .orderByDesc(CouplePact::getCreated));
    }

    /** 84 注销清理。 */
    default void deleteBySpace(String spaceId) {
        delete(new LambdaQueryWrapper<CouplePact>().eq(CouplePact::getSpaceId, spaceId));
    }
}
