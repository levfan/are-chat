package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.surprise.Scratch;
import com.smart.chat.couple.domain.surprise.ScratchRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link ScratchRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 查询复用 {@link CoupleScratchMapper} 已有的 default 方法（Mapper 不感知领域类型），
 * 「新→旧」的排序口径因此与改造前逐字相同。
 * <p>
 * 写回沿用 {@code DeedRepositoryAdapter} 的纪律：<b>只回写聚合纳管的三列</b>
 * （{@code scratched}／{@code scratched_at}／{@code redeemed_at}）。券面、送券人、收券人、周锚
 * 是发券那一刻就定死的事实，所以更新分支先 {@code selectById} 取回原行、改完再写回。
 */
@Component
public class ScratchRepositoryAdapter implements ScratchRepository {

    private final CoupleScratchMapper scratchMapper;

    public ScratchRepositoryAdapter(CoupleScratchMapper scratchMapper) {
        this.scratchMapper = scratchMapper;
    }

    @Override
    public List<Scratch> findByWeek(String spaceId, String weekKey) {
        return scratchMapper.findByWeek(spaceId, weekKey).stream().map(ScratchRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Scratch> findByOwner(String spaceId, String owner) {
        return scratchMapper.findByOwner(spaceId, owner).stream().map(ScratchRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<Scratch> findByIdIn(String id, String spaceId) {
        if (id == null || spaceId == null) {
            return Optional.empty();
        }
        CoupleScratchPO po = scratchMapper.selectById(id);
        return po == null || !spaceId.equals(po.getSpaceId()) ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void save(Scratch card) {
        CoupleScratchPO existing = scratchMapper.selectById(card.id());
        if (existing == null) {
            scratchMapper.insert(toPo(card));
            return;
        }
        applyOwnedFields(existing, card);
        scratchMapper.updateById(existing);
    }

    /** 聚合负责维护的列：刮开标记与两个时刻。 */
    private static void applyOwnedFields(CoupleScratchPO po, Scratch card) {
        po.setScratched(card.scratched());
        po.setScratchedAt(card.scratchedAt());
        po.setRedeemedAt(card.redeemedAt());
    }

    private static CoupleScratchPO toPo(Scratch card) {
        CoupleScratchPO po = new CoupleScratchPO();
        po.setId(card.id());
        po.setSpaceId(card.spaceId());
        po.setWeekKey(card.weekKey());
        po.setFromUser(card.fromUser());
        po.setOwner(card.owner());
        po.setPrizeKind(card.prizeKind());
        po.setPrizeText(card.prizeText());
        po.setScratched(card.scratched());
        po.setScratchedAt(card.scratchedAt());
        po.setRedeemedAt(card.redeemedAt());
        po.setCreated(card.created());
        return po;
    }

    private static Scratch toDomain(CoupleScratchPO po) {
        return Scratch.restore(po.getId(), po.getSpaceId(), po.getWeekKey(), po.getFromUser(), po.getOwner(),
                po.getPrizeKind(), po.getPrizeText(), po.isScratched(), po.getScratchedAt(), po.getRedeemedAt(),
                po.getCreated());
    }
}
