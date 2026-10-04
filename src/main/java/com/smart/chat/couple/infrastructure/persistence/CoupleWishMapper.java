package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleWishMapper extends BaseMapperCompat<CoupleWishPO> {

    /** 某空间的全部愿望：未实现的在前，同组内新记录的在前。 */
    default List<CoupleWishPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWishPO>()
                .eq(CoupleWishPO::getSpaceId, spaceId)
                .orderByAsc(CoupleWishPO::getStatus)
                .orderByDesc(CoupleWishPO::getCreated));
    }

    /** 某空间已实现的愿望条数（双方合计，百日回顾用）。 */
    default long countFulfilled(String spaceId) {
        return selectCount(new LambdaQueryWrapper<CoupleWishPO>()
                .eq(CoupleWishPO::getSpaceId, spaceId)
                .eq(CoupleWishPO::getStatus, CoupleWishPO.STATUS_FULFILLED));
    }

    /** 某个许愿人已实现的愿望条数。 */
    default long countFulfilledBy(String spaceId, String ownerUser) {
        return selectCount(new LambdaQueryWrapper<CoupleWishPO>()
                .eq(CoupleWishPO::getSpaceId, spaceId)
                .eq(CoupleWishPO::getOwnerUser, ownerUser)
                .eq(CoupleWishPO::getStatus, CoupleWishPO.STATUS_FULFILLED));
    }

    /** 同一个许愿人的同名愿望（查重用，大小写敏感交给列的 collate）。 */
    default boolean existsSameTitle(String spaceId, String ownerUser, String title) {
        return selectCount(new LambdaQueryWrapper<CoupleWishPO>()
                .eq(CoupleWishPO::getSpaceId, spaceId)
                .eq(CoupleWishPO::getOwnerUser, ownerUser)
                .eq(CoupleWishPO::getTitle, title)) > 0;
    }
}
