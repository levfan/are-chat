package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.chore.SpinTask;
import com.smart.chat.couple.domain.chore.SpinTaskRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link SpinTaskRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 查询复用 {@link CoupleSpinTaskMapper} 已有的 default 方法（Mapper 不感知领域类型），
 * 排序与过滤口径因此与改造前逐字相同。
 * <p>
 * 写回沿用 {@code DeedRepositoryAdapter} 的纪律：<b>只回写聚合纳管的三列</b>
 * （{@code confirmed}／{@code done}／{@code done_at}）。事项、分到谁、周锚、创建时刻一旦转出就冻结，
 * 所以更新分支先 {@code selectById} 取回原行、改完再写回，绝不拿聚合重建整行。
 */
@Component
public class SpinTaskRepositoryAdapter implements SpinTaskRepository {

    private final CoupleSpinTaskMapper spinMapper;

    public SpinTaskRepositoryAdapter(CoupleSpinTaskMapper spinMapper) {
        this.spinMapper = spinMapper;
    }

    @Override
    public boolean alreadySpun(String spaceId, String week) {
        return !spinMapper.findByWeek(spaceId, week).isEmpty();
    }

    @Override
    public List<SpinTask> findByWeek(String spaceId, String week) {
        return spinMapper.findByWeek(spaceId, week).stream().map(SpinTaskRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<SpinTask> findByIdIn(String id, String spaceId) {
        if (id == null || spaceId == null) {
            return Optional.empty();
        }
        CoupleSpinTaskPO po = spinMapper.selectById(id);
        return po == null || !spaceId.equals(po.getSpaceId()) ? Optional.empty() : Optional.of(toDomain(po));
    }

    @Override
    public void save(SpinTask task) {
        CoupleSpinTaskPO existing = spinMapper.selectById(task.id());
        if (existing == null) {
            spinMapper.insert(toPo(task));
            return;
        }
        applyOwnedFields(existing, task);
        spinMapper.updateById(existing);
    }

    /** 聚合负责维护的列：双签与完成时刻。 */
    private static void applyOwnedFields(CoupleSpinTaskPO po, SpinTask task) {
        po.setConfirmed(task.confirmed() ? 1 : 0);
        po.setDone(task.done() ? 1 : 0);
        po.setDoneAt(task.doneAt());
    }

    private static CoupleSpinTaskPO toPo(SpinTask task) {
        CoupleSpinTaskPO po = new CoupleSpinTaskPO();
        po.setId(task.id());
        po.setSpaceId(task.spaceId());
        po.setWeek(task.week());
        po.setItem(task.item());
        po.setAssignedUser(task.assignedUser());
        po.setConfirmed(task.confirmed() ? 1 : 0);
        po.setDone(task.done() ? 1 : 0);
        po.setDoneAt(task.doneAt());
        po.setCreated(task.created());
        return po;
    }

    private static SpinTask toDomain(CoupleSpinTaskPO po) {
        return SpinTask.restore(po.getId(), po.getSpaceId(), po.getWeek(), po.getItem(), po.getAssignedUser(),
                po.confirmedFlag(), po.doneFlag(), po.getDoneAt(), po.getCreated());
    }
}
