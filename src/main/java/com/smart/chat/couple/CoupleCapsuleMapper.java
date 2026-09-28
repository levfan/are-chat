package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleCapsuleMapper extends BaseMapperCompat<CoupleCapsule> {

    /** 某空间全部胶囊（新→旧）。 */
    default List<CoupleCapsule> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCapsule>()
                .eq(CoupleCapsule::getSpaceId, spaceId)
                .orderByDesc(CoupleCapsule::getCreated));
    }
}
