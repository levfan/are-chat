package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleAnniversaryMapper extends BaseMapperCompat<CoupleAnniversary> {

    default List<CoupleAnniversary> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleAnniversary>()
                .eq(CoupleAnniversary::getSpaceId, spaceId)
                .orderByAsc(CoupleAnniversary::getEventDate)
                .orderByAsc(CoupleAnniversary::getCreated));
    }

    /** 84 注销清理。 */
    default void deleteBySpace(String spaceId) {
        delete(new LambdaQueryWrapper<CoupleAnniversary>().eq(CoupleAnniversary::getSpaceId, spaceId));
    }
}
