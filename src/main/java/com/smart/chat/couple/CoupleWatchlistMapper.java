package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleWatchlistMapper extends BaseMapperCompat<CoupleWatchlist> {

    /** 空间的追剧清单（追剧中在前，完结在后）。 */
    default List<CoupleWatchlist> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWatchlist>()
                .eq(CoupleWatchlist::getSpaceId, spaceId)
                .orderByAsc(CoupleWatchlist::getStatus)
                .orderByDesc(CoupleWatchlist::getCreated));
    }
}
