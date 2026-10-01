package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F292 改天拍卖数据访问。 */
@Mapper
public interface CoupleSomedayMapper extends BaseMapperCompat<CoupleSomeday> {

    /** 在架/已认领的在途单（上架时间升序；全量过滤在服务层做）。 */
    default List<CoupleSomeday> findAll(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSomeday>()
                .eq(CoupleSomeday::getSpaceId, spaceId)
                .orderByAsc(CoupleSomeday::getCreated));
    }

    /** 上架时间早于截止点仍未认领的（惰性下架用）。 */
    default List<CoupleSomeday> findStaleShelf(String spaceId, long shelfBeforeMs) {
        return selectList(new LambdaQueryWrapper<CoupleSomeday>()
                .eq(CoupleSomeday::getSpaceId, spaceId)
                .eq(CoupleSomeday::getStatus, "SHELF")
                .lt(CoupleSomeday::getCreated, shelfBeforeMs));
    }
}
