package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.deed.Deed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 好事簿仓储适配器：验证「只回写聚合纳管的列」这条纪律。
 * 聚合没建模到的列（发生日、内容、记录人、创建时刻）必须原样留着——
 * 用聚合重建整行会把这些列静默清空，而这类 bug 编译与业务用例都照不出来。
 */
@ExtendWith(MockitoExtension.class)
class DeedRepositoryAdapterTest {

    @Mock
    private CoupleEchoDeedMapper deedMapper;

    @InjectMocks
    private DeedRepositoryAdapter adapter;

    @Test
    void newDeedIsInsertedWithAllColumns() {
        when(deedMapper.selectById(any())).thenReturn(null);
        Deed deed = Deed.record("s1", "alice", "下雨天绕路来接我", null, LocalDate.parse("2026-10-05"));

        adapter.save(deed);

        ArgumentCaptor<CoupleEchoDeedPO> captor = ArgumentCaptor.forClass(CoupleEchoDeedPO.class);
        verify(deedMapper).insert(captor.capture());
        CoupleEchoDeedPO po = captor.getValue();
        assertThat(po.getSpaceId()).isEqualTo("s1");
        assertThat(po.getFromUser()).isEqualTo("alice");
        assertThat(po.getContent()).isEqualTo("下雨天绕路来接我");
        assertThat(po.getDay()).isEqualTo("2026-10-05");
        assertThat(po.getStarred()).isZero();
        verify(deedMapper, never()).updateById(any(CoupleEchoDeedPO.class));
    }

    @Test
    void updateOnlyWritesStarAndUpdatedAt() {
        CoupleEchoDeedPO stored = new CoupleEchoDeedPO();
        stored.setId("d1");
        stored.setSpaceId("s1");
        stored.setFromUser("alice");
        stored.setContent("接我下班");
        stored.setDay("2026-10-01");
        stored.setStarred(0);
        stored.setCreated(111L);
        stored.setUpdatedAt(111L);
        when(deedMapper.selectById("d1")).thenReturn(stored);
        Deed deed = Deed.restore("d1", "s1", "alice", "接我下班", "2026-10-01", 0, 111L, null);
        deed.starBy("alice");

        adapter.save(deed);

        ArgumentCaptor<CoupleEchoDeedPO> captor = ArgumentCaptor.forClass(CoupleEchoDeedPO.class);
        verify(deedMapper).updateById(captor.capture());
        CoupleEchoDeedPO written = captor.getValue();
        assertThat(written.getStarred()).as("加星是聚合唯一真的改动的状态列").isEqualTo(1);
        assertThat(written.getUpdatedAt()).isNotEqualTo(111L);
        assertThat(written.getContent()).isEqualTo("接我下班");
        assertThat(written.getDay()).isEqualTo("2026-10-01");
        assertThat(written.getCreated()).isEqualTo(111L);
        verify(deedMapper, never()).insert(any(CoupleEchoDeedPO.class));
    }

    @Test
    void queriesMapRowsBackIntoDeeds() {
        CoupleEchoDeedPO row = new CoupleEchoDeedPO();
        row.setId("d2");
        row.setSpaceId("s1");
        row.setFromUser("bob");
        row.setContent("TA 做饭");
        row.setDay("2026-10-04");
        row.setStarred(1);
        when(deedMapper.findByUser("s1", "bob")).thenReturn(List.of(row));
        when(deedMapper.findByDayContent("s1", "bob", "2026-10-04", "TA 做饭")).thenReturn(row);
        when(deedMapper.selectById("d2")).thenReturn(row);

        assertThat(adapter.listByRecorder("s1", "bob")).extracting(Deed::content).containsExactly("TA 做饭");
        assertThat(adapter.alreadyRecorded("s1", "bob", "2026-10-04", "TA 做饭")).isTrue();
        assertThat(adapter.alreadyRecorded("s1", "bob", "2026-10-04", "别的")).isFalse();
        Optional<Deed> found = adapter.findById("d2");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().starred()).isTrue();
    }
}
