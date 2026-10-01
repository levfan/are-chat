package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleMonthReviewMapper extends BaseMapperCompat<CoupleMonthReview> {

    /** 某人某月的互评。 */
    default CoupleMonthReview find(String spaceId, String month, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleMonthReview>()
                .eq(CoupleMonthReview::getSpaceId, spaceId)
                .eq(CoupleMonthReview::getMonth, month)
                .eq(CoupleMonthReview::getFromUser, fromUser));
    }

    /** 全部互评（按月倒序）。 */
    default List<CoupleMonthReview> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleMonthReview>()
                .eq(CoupleMonthReview::getSpaceId, spaceId)
                .orderByDesc(CoupleMonthReview::getMonth));
    }
}
