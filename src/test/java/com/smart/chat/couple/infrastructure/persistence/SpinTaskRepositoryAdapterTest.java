package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.chore.SpinTask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 家务轮盘仓储适配器：验证「只回写聚合纳管的列」这条纪律。
 * 聚合没建模到的列（周锚、事项、分到谁、创建时刻）必须原样留着——
 * 拿聚合重建整行会把这些列静默清空，而这类 bug 编译与业务用例都照不出来。
 */
@ExtendWith(MockitoExtension.class)
class SpinTaskRepositoryAdapterTest {

    @Mock
    private CoupleSpinTaskMapper spinMapper;

    @InjectMocks
    private SpinTaskRepositoryAdapter adapter;

    @Test
    void newTaskIsInsertedWithAllColumns() {
        when(spinMapper.selectById(any())).thenReturn(null);
        SpinTask task = SpinTask.draw("s1", "2026-10-05", "洗碗,拖地", "alice", "bob", 0L, false).get(0);

        adapter.save(task);

        ArgumentCaptor<CoupleSpinTaskPO> captor = ArgumentCaptor.forClass(CoupleSpinTaskPO.class);
        verify(spinMapper).insert(captor.capture());
        CoupleSpinTaskPO po = captor.getValue();
        assertThat(po.getId()).isEqualTo(task.id());
        assertThat(po.getSpaceId()).isEqualTo("s1");
        assertThat(po.getWeek()).isEqualTo("2026-10-05");
        assertThat(po.getItem()).isEqualTo("洗碗");
        assertThat(po.getAssignedUser()).isEqualTo(task.assignedUser());
        assertThat(po.getConfirmed()).isZero();
        assertThat(po.getDone()).isZero();
        verify(spinMapper, never()).updateById(any(CoupleSpinTaskPO.class));
    }

    @Test
    void updateOnlyWritesSignaturesAndDoneAt() {
        CoupleSpinTaskPO stored = new CoupleSpinTaskPO();
        stored.setId("t1");
        stored.setSpaceId("s1");
        stored.setWeek("2026-10-05");
        stored.setItem("洗碗");
        stored.setAssignedUser("alice");
        stored.setConfirmed(0);
        stored.setDone(0);
        stored.setCreated(111L);
        when(spinMapper.selectById("t1")).thenReturn(stored);
        SpinTask task = SpinTask.restore("t1", "s1", "2026-10-05", "洗碗", "alice", false, false, null, 111L);
        task.confirmBy("bob");
        task.markDoneBy("alice");

        adapter.save(task);

        ArgumentCaptor<CoupleSpinTaskPO> captor = ArgumentCaptor.forClass(CoupleSpinTaskPO.class);
        verify(spinMapper).updateById(captor.capture());
        CoupleSpinTaskPO written = captor.getValue();
        assertThat(written.getConfirmed()).as("认账是聚合纳管的列").isEqualTo(1);
        assertThat(written.getDone()).as("打勾是聚合纳管的列").isEqualTo(1);
        assertThat(written.getDoneAt()).isNotNull();
        assertThat(written.getItem()).as("事项发布即冻结").isEqualTo("洗碗");
        assertThat(written.getWeek()).isEqualTo("2026-10-05");
        assertThat(written.getAssignedUser()).isEqualTo("alice");
        assertThat(written.getCreated()).isEqualTo(111L);
        verify(spinMapper, never()).insert(any(CoupleSpinTaskPO.class));
    }

    @Test
    void queriesMapRowsBackIntoTasks() {
        CoupleSpinTaskPO row = new CoupleSpinTaskPO();
        row.setId("t2");
        row.setSpaceId("s1");
        row.setWeek("2026-09-28");
        row.setItem("倒垃圾");
        row.setAssignedUser("bob");
        row.setConfirmed(1);
        row.setDone(0);
        when(spinMapper.findByWeek("s1", "2026-09-28")).thenReturn(List.of(row));
        when(spinMapper.selectById("t2")).thenReturn(row);

        assertThat(adapter.findByWeek("s1", "2026-09-28")).extracting(SpinTask::item).containsExactly("倒垃圾");
        assertThat(adapter.alreadySpun("s1", "2026-09-28")).isTrue();
        assertThat(adapter.alreadySpun("s1", "2026-10-05")).isFalse();

        Optional<SpinTask> found = adapter.findByIdIn("t2", "s1");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().confirmed()).isTrue();
        assertThat(found.orElseThrow().owed()).isTrue();
        // 别的空间的格子号在这里等同于不存在
        assertThat(adapter.findByIdIn("t2", "other")).isEmpty();
        assertThat(adapter.findByIdIn(null, "s1")).isEmpty();
    }
}
