package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F340 年度十问数据访问。 */
@Mapper
public interface CoupleLegacyTenMapper extends BaseMapperCompat<CoupleLegacyTen> {

    /** 某年十答（双方各一条，uk 保证最多两条，按人排）。 */
    default List<CoupleLegacyTen> findByYear(String spaceId, String year) {
        return selectList(new LambdaQueryWrapper<CoupleLegacyTen>()
                .eq(CoupleLegacyTen::getSpaceId, spaceId)
                .eq(CoupleLegacyTen::getYear, year)
                .orderByAsc(CoupleLegacyTen::getFromUser));
    }

    /** 某人某年的十答（uk(space_id,year,from_user) 保证最多一条）。 */
    default CoupleLegacyTen findByYearUser(String spaceId, String year, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleLegacyTen>()
                .eq(CoupleLegacyTen::getSpaceId, spaceId)
                .eq(CoupleLegacyTen::getYear, year)
                .eq(CoupleLegacyTen::getFromUser, fromUser));
    }

    /** 空间全部十答（年份新→旧，供跨年对比）。 */
    default List<CoupleLegacyTen> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLegacyTen>()
                .eq(CoupleLegacyTen::getSpaceId, spaceId)
                .orderByDesc(CoupleLegacyTen::getYear));
    }
}
