package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F321 道歉质检数据访问。 */
@Mapper
public interface CoupleSorryReviewMapper extends BaseMapperCompat<CoupleSorryReview> {

    /** 待对方验货的道歉（新→旧）。 */
    default List<CoupleSorryReview> findPending(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSorryReview>()
                .eq(CoupleSorryReview::getSpaceId, spaceId)
                .eq(CoupleSorryReview::getStatus, CoupleSorryReview.STATUS_VERIFY)
                .orderByDesc(CoupleSorryReview::getCreated));
    }

    /** 空间全部道歉质检（新→旧）。 */
    default List<CoupleSorryReview> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSorryReview>()
                .eq(CoupleSorryReview::getSpaceId, spaceId)
                .orderByDesc(CoupleSorryReview::getCreated));
    }

    /** 某人写过的道歉（新→旧）。 */
    default List<CoupleSorryReview> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleSorryReview>()
                .eq(CoupleSorryReview::getSpaceId, spaceId)
                .eq(CoupleSorryReview::getFromUser, fromUser)
                .orderByDesc(CoupleSorryReview::getCreated));
    }
}
