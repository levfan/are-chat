package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.coupon.CouponRepository;
import com.smart.chat.couple.domain.coupon.WishCoupon;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@link CouponRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只在这里。 */
@Component
public class CouponRepositoryAdapter implements CouponRepository {

    private final CoupleCeremonyCouponMapper couponMapper;

    public CouponRepositoryAdapter(CoupleCeremonyCouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    @Override
    public List<WishCoupon> findBySpace(String spaceId) {
        return couponMapper.findBySpace(spaceId).stream().map(CouponRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<WishCoupon> findByIdIn(String id, String spaceId) {
        CoupleCeremonyCouponPO po = couponMapper.selectById(id);
        if (po == null || !spaceId.equals(po.getSpaceId())) {
            return Optional.empty();
        }
        return Optional.of(toDomain(po));
    }

    @Override
    public void issue(WishCoupon coupon, String ref) {
        CoupleCeremonyCouponPO po = new CoupleCeremonyCouponPO();
        po.setId(UUID.randomUUID().toString());
        po.setSpaceId(coupon.spaceId());
        po.setTitle(coupon.title());
        po.setStatus(coupon.status());
        po.setRef(ref);
        po.setIssuer(coupon.grantedBy());
        po.setCreated(System.currentTimeMillis());
        couponMapper.insert(po);
    }

    @Override
    public void save(WishCoupon coupon) {
        CoupleCeremonyCouponPO existing = couponMapper.selectById(coupon.id());
        if (existing == null) {
            return;
        }
        existing.setStatus(coupon.status());
        existing.setUsedBy(coupon.usedBy());
        existing.setUsedAt(coupon.usedAt());
        couponMapper.updateById(existing);
    }

    private static WishCoupon toDomain(CoupleCeremonyCouponPO po) {
        return WishCoupon.restore(po.getId(), po.getSpaceId(), po.getTitle(), po.getIssuer(), po.getRef(),
                po.getCreated(), po.getStatus(), po.getUsedBy(), po.getUsedAt());
    }
}
