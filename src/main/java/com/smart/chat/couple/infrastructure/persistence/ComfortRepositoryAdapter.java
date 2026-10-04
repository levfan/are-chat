package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.comfort.ComfortRepository;
import com.smart.chat.couple.domain.comfort.ComfortRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link ComfortRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 取行复用 {@link CoupleComfortMapper} 已有的 default 方法（Mapper 不感知领域类型），
 * 看板的「新→旧」排序口径因此与改造前逐字相同。
 * <p>
 * 写回沿用 {@code CoupleSpaceRepositoryAdapter} 确立的纪律：<b>只回写聚合纳管的四列</b>
 * （{@code feeling}/{@code handled}/{@code handled_note}/{@code handled_at}）。
 * 「已被接住」以 {@code handled} 一列为准——改写感受时 NOT_NULL 更新策略不会清空那句话与时刻，
 * 库里会留残值，拿 {@code heldAt} 反推会把旧的一次接住误读成还没被接。
 * 空间、求的人、日期、创建时刻发布即冻结，所以更新分支先 {@code selectById} 取回原行、改完再写回。
 */
@Component
public class ComfortRepositoryAdapter implements ComfortRepository {

    private final CoupleComfortMapper comfortMapper;

    public ComfortRepositoryAdapter(CoupleComfortMapper comfortMapper) {
        this.comfortMapper = comfortMapper;
    }

    @Override
    public Optional<ComfortRequest> findBySpaceAndUserOn(String spaceId, String username, String day) {
        return Optional.ofNullable(comfortMapper.find(spaceId, username, day))
                .map(ComfortRepositoryAdapter::toDomain);
    }

    @Override
    public List<ComfortRequest> listBySpace(String spaceId) {
        return comfortMapper.findBySpace(spaceId).stream().map(ComfortRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(ComfortRequest request) {
        CoupleComfortPO existing = request.id() == null ? null : comfortMapper.selectById(request.id());
        if (existing == null) {
            comfortMapper.insert(toPo(request));
            return;
        }
        applyOwnedFields(existing, request);
        comfortMapper.updateById(existing);
    }

    /** 聚合负责维护的列：感受、有没有被接住、那句话、接住时刻。 */
    private static void applyOwnedFields(CoupleComfortPO po, ComfortRequest request) {
        po.setFeeling(request.feeling());
        po.setHandled(request.handled());
        po.setHandledNote(request.note());
        po.setHandledAt(request.heldAt());
    }

    private static CoupleComfortPO toPo(ComfortRequest request) {
        CoupleComfortPO po = new CoupleComfortPO();
        po.setId(request.id());
        po.setSpaceId(request.spaceId());
        po.setFromUser(request.by());
        po.setDay(request.day());
        po.setFeeling(request.feeling());
        po.setHandled(request.handled());
        po.setHandledNote(request.note());
        po.setHandledAt(request.heldAt());
        po.setCreated(request.created());
        return po;
    }

    private static ComfortRequest toDomain(CoupleComfortPO po) {
        return ComfortRequest.restore(po.getId(), po.getFromUser(), po.getFeeling(), po.getHandledNote(),
                po.getHandledAt(), po.isHandled(), po.getSpaceId(), po.getDay(), po.getCreated());
    }
}
