package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F348 周年抽奖箱数据访问。 */
@Mapper
public interface CoupleLegacyDrawMapper extends BaseMapperCompat<CoupleLegacyDraw> {

    /** 某年的抽奖箱（uk(space_id,year) 保证最多一条）。 */
    default CoupleLegacyDraw findByYear(String spaceId, String year) {
        return selectOne(new LambdaQueryWrapper<CoupleLegacyDraw>()
                .eq(CoupleLegacyDraw::getSpaceId, spaceId)
                .eq(CoupleLegacyDraw::getYear, year));
    }

    /** 空间全部抽奖箱（年份新→旧）。 */
    default List<CoupleLegacyDraw> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLegacyDraw>()
                .eq(CoupleLegacyDraw::getSpaceId, spaceId)
                .orderByDesc(CoupleLegacyDraw::getYear));
    }
}
