package com.smart.chat.couple.domain.coupon;

import java.util.List;
import java.util.Optional;

/** 愿望券的仓储端口。 */
public interface CouponRepository {

    /** 该空间的全部券（在途与已核销由调用方按状态分栏） */
    List<WishCoupon> findBySpace(String spaceId);

    /** 只认这张空间里的券——别人的券号在这里等同于不存在 */
    Optional<WishCoupon> findByIdIn(String id, String spaceId);

    /** 发一张新券：id 与发出时刻在适配器落库时生成，ref 现役恒为空串 */
    void issue(WishCoupon coupon, String ref);

    /** 回写核销结果（状态、核销人、核销时刻） */
    void save(WishCoupon coupon);
}
