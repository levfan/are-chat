package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.wish.Wish;
import com.smart.chat.couple.domain.wish.WishRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link WishRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 更新走「先取原行、只改聚合纳管的列、再写回」（沿用 CoupleSpaceRepositoryAdapter 的纪律）：
 * 愿望属于哪个空间、谁许的、谁记的、什么时候记的，这些列一旦写下就不再有用例去改。
 */
@Component
public class WishRepositoryAdapter implements WishRepository {

    private final CoupleWishMapper wishMapper;

    public WishRepositoryAdapter(CoupleWishMapper wishMapper) {
        this.wishMapper = wishMapper;
    }

    @Override
    public List<Wish> findBySpace(String spaceId) {
        return wishMapper.findBySpace(spaceId).stream().map(WishRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<Wish> findById(String wishId) {
        return Optional.ofNullable(wishMapper.selectById(wishId)).map(WishRepositoryAdapter::toDomain);
    }

    @Override
    public boolean sameTitleExists(String spaceId, String ownerUser, String title) {
        return wishMapper.existsSameTitle(spaceId, ownerUser, title);
    }

    @Override
    public long countFulfilled(String spaceId) {
        return wishMapper.countFulfilled(spaceId);
    }

    @Override
    public long countFulfilledBy(String spaceId, String ownerUser) {
        return wishMapper.countFulfilledBy(spaceId, ownerUser);
    }

    @Override
    public void save(Wish wish) {
        if (wish.id() == null) {
            wishMapper.insert(CoupleWishPO.of(wish.spaceId(), wish.ownerUser(), wish.creatorUser(),
                    wish.title(), wish.note()));
            return;
        }
        CoupleWishPO existing = wishMapper.selectById(wish.id());
        if (existing == null) {
            return;
        }
        existing.setStatus(wish.status());
        existing.setNote(wish.note());
        existing.setPreparedBy(wish.preparedBy());
        existing.setPreparedAt(wish.preparedAt());
        existing.setFulfilledAt(wish.fulfilledAt());
        // 改动时刻由写这一层落章：聚合不持有 updatedAt，也不该为了它假装持有
        existing.setUpdatedAt(System.currentTimeMillis());
        wishMapper.updateById(existing);
    }

    @Override
    public void deleteById(String id) {
        wishMapper.deleteById(id);
    }

    private static Wish toDomain(CoupleWishPO po) {
        return Wish.restore(po.getId(), po.getSpaceId(), po.getOwnerUser(), po.getCreatorUser(), po.getTitle(),
                po.getNote(), po.getStatus(), po.getPreparedBy(), po.getPreparedAt(), po.getFulfilledAt(),
                po.getCreated());
    }
}
