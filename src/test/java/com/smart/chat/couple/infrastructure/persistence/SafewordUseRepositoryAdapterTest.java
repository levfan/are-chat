package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.safeword.SafewordUse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 暂停流水仓储适配器：验证「新喊停整行插入 / 复盘回填只写 reflect+updatedAt」这条纪律，
 * 以及归属闸门（id 属于别的空间时读不出来）。
 */
@ExtendWith(MockitoExtension.class)
class SafewordUseRepositoryAdapterTest {

    private static final String DAY = LocalDate.now().toString();

    @Mock
    private CoupleCatchSafewordUseMapper useMapper;

    @InjectMocks
    private SafewordUseRepositoryAdapter adapter;

    @Test
    void newPauseIsInsertedWithEmptyReflectAndAggregateId() {
        when(useMapper.selectById(any())).thenReturn(null);
        SafewordUse pause = SafewordUse.shout(DAY, "alice");

        adapter.save("s1", pause);

        ArgumentCaptor<CoupleCatchSafewordUsePO> captor = ArgumentCaptor.forClass(CoupleCatchSafewordUsePO.class);
        verify(useMapper).insert(captor.capture());
        CoupleCatchSafewordUsePO po = captor.getValue();
        assertThat(po.getId()).isEqualTo(pause.id());
        assertThat(po.getSpaceId()).isEqualTo("s1");
        assertThat(po.getDay()).isEqualTo(DAY);
        assertThat(po.getUserName()).isEqualTo("alice");
        assertThat(po.getReflect()).as("复盘登记即空串，与 PO.of 一致").isEmpty();
        assertThat(po.getCreated()).isEqualTo(po.getUpdatedAt());
        verify(useMapper, never()).updateById(any(CoupleCatchSafewordUsePO.class));
    }

    @Test
    void reflectUpdateOnlyWritesReflectAndUpdatedAt() {
        CoupleCatchSafewordUsePO stored = new CoupleCatchSafewordUsePO();
        stored.setId("u1");
        stored.setSpaceId("s1");
        stored.setDay(DAY);
        stored.setUserName("alice");
        stored.setReflect("");
        stored.setCreated(111L);
        stored.setUpdatedAt(111L);
        when(useMapper.selectById("u1")).thenReturn(stored);

        SafewordUse pause = SafewordUse.restore("u1", DAY, "alice", null);
        pause.reflectBy("alice", "当时卡在翻旧账");
        adapter.save("s1", pause);

        ArgumentCaptor<CoupleCatchSafewordUsePO> captor = ArgumentCaptor.forClass(CoupleCatchSafewordUsePO.class);
        verify(useMapper).updateById(captor.capture());
        CoupleCatchSafewordUsePO written = captor.getValue();
        assertThat(written.getReflect()).as("复盘是聚合唯一真的写回的状态列").isEqualTo("当时卡在翻旧账");
        assertThat(written.getUpdatedAt()).isNotEqualTo(111L);
        assertThat(written.getId()).isEqualTo("u1");
        assertThat(written.getSpaceId()).isEqualTo("s1");
        assertThat(written.getDay()).isEqualTo(DAY);
        assertThat(written.getUserName()).isEqualTo("alice");
        assertThat(written.getCreated()).isEqualTo(111L);
        verify(useMapper, never()).insert(any(CoupleCatchSafewordUsePO.class));
    }

    @Test
    void queriesMapRowsBackIntoUses() {
        CoupleCatchSafewordUsePO row = new CoupleCatchSafewordUsePO();
        row.setId("u2");
        row.setSpaceId("s1");
        row.setDay(DAY);
        row.setUserName("bob");
        row.setReflect("复盘");
        when(useMapper.findBySpace("s1")).thenReturn(List.of(row));
        when(useMapper.find("s1", DAY, "bob")).thenReturn(row);
        when(useMapper.selectById("u2")).thenReturn(row);

        assertThat(adapter.listBySpace("s1")).extracting(SafewordUse::by).containsExactly("bob");
        assertThat(adapter.findBySpaceAndDayAndUser("s1", "bob", DAY)).isPresent();
        assertThat(adapter.findByIdInSpace("s1", "u2")).isPresent();
    }

    @Test
    void foreignOrBlankIdReadsBackEmpty() {
        CoupleCatchSafewordUsePO other = new CoupleCatchSafewordUsePO();
        other.setId("u3");
        other.setSpaceId("s2");
        other.setDay(DAY);
        other.setUserName("carol");
        when(useMapper.selectById("u3")).thenReturn(other);

        assertThat(adapter.findByIdInSpace("s1", "u3")).isEmpty();
        assertThat(adapter.findByIdInSpace("s1", "  ")).isEmpty();
        assertThat(adapter.findByIdInSpace("s1", null)).isEmpty();
    }
}
