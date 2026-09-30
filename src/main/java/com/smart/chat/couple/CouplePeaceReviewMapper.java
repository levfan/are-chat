package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouplePeaceReviewMapper extends BaseMapperCompat<CouplePeaceReview> {

    /** 某天的复盘（双方各一份）。 */
    default List<CouplePeaceReview> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CouplePeaceReview>()
                .eq(CouplePeaceReview::getSpaceId, spaceId)
                .eq(CouplePeaceReview::getDay, day));
    }

    /** 空间全部复盘（按日期新→旧）。 */
    default List<CouplePeaceReview> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePeaceReview>()
                .eq(CouplePeaceReview::getSpaceId, spaceId)
                .orderByDesc(CouplePeaceReview::getDay)
                .orderByAsc(CouplePeaceReview::getCreated));
    }
}
