package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F346 我们的一年数据访问。 */
@Mapper
public interface CoupleLegacyReviewMapper extends BaseMapperCompat<CoupleLegacyReview> {

    /** 某年的年度盘点（uk(space_id,year) 保证最多一条）。 */
    default CoupleLegacyReview findByYear(String spaceId, String year) {
        return selectOne(new LambdaQueryWrapper<CoupleLegacyReview>()
                .eq(CoupleLegacyReview::getSpaceId, spaceId)
                .eq(CoupleLegacyReview::getYear, year));
    }

    /** 空间全部年度盘点（年份新→旧）。 */
    default List<CoupleLegacyReview> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLegacyReview>()
                .eq(CoupleLegacyReview::getSpaceId, spaceId)
                .orderByDesc(CoupleLegacyReview::getYear));
    }
}
