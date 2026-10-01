package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSyncTapMapper extends BaseMapperCompat<CoupleSyncTap> {

    /** 某天的同频记录。 */
    default CoupleSyncTap find(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleSyncTap>()
                .eq(CoupleSyncTap::getSpaceId, spaceId)
                .eq(CoupleSyncTap::getDay, day));
    }

    /** 全部记录（按天倒序，用于排行榜）。 */
    default List<CoupleSyncTap> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSyncTap>()
                .eq(CoupleSyncTap::getSpaceId, spaceId)
                .orderByDesc(CoupleSyncTap::getDay));
    }
}
