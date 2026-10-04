package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.safeword.Safeword;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 安全词仓储适配器：验证「只回写聚合纳管的列」这条纪律。
 * 词与说明可以改写，但归属（id、space、from_user）与 created 一旦落定就不该被 upsert 的更新分支清掉。
 */
@ExtendWith(MockitoExtension.class)
class SafewordRepositoryAdapterTest {

    @Mock
    private CoupleCatchSafewordMapper safewordMapper;

    @InjectMocks
    private SafewordRepositoryAdapter adapter;

    @Test
    void newWordIsInsertedWithAllColumns() {
        when(safewordMapper.find("s1", "alice")).thenReturn(null);

        adapter.save("s1", "alice", Safeword.agree("暂停", "给我十分钟"));

        ArgumentCaptor<CoupleCatchSafewordPO> captor = ArgumentCaptor.forClass(CoupleCatchSafewordPO.class);
        verify(safewordMapper).insert(captor.capture());
        CoupleCatchSafewordPO po = captor.getValue();
        assertThat(po.getSpaceId()).isEqualTo("s1");
        assertThat(po.getFromUser()).isEqualTo("alice");
        assertThat(po.getWord()).isEqualTo("暂停");
        assertThat(po.getNote()).isEqualTo("给我十分钟");
        verify(safewordMapper, never()).updateById(po);
    }

    @Test
    void rewriteOnlyTouchesWordNoteAndUpdatedAt() {
        CoupleCatchSafewordPO stored = new CoupleCatchSafewordPO();
        stored.setId("sw1");
        stored.setSpaceId("s1");
        stored.setFromUser("alice");
        stored.setWord("旧词");
        stored.setNote("旧备注");
        stored.setCreated(111L);
        stored.setUpdatedAt(111L);
        when(safewordMapper.find("s1", "alice")).thenReturn(stored);

        adapter.save("s1", "alice", Safeword.agree("新词", "新备注"));

        ArgumentCaptor<CoupleCatchSafewordPO> captor = ArgumentCaptor.forClass(CoupleCatchSafewordPO.class);
        verify(safewordMapper).updateById(captor.capture());
        CoupleCatchSafewordPO written = captor.getValue();
        assertThat(written.getWord()).as("词是聚合唯一真的改动的状态列").isEqualTo("新词");
        assertThat(written.getNote()).isEqualTo("新备注");
        assertThat(written.getUpdatedAt()).isNotEqualTo(111L);
        assertThat(written.getId()).isEqualTo("sw1");
        assertThat(written.getSpaceId()).isEqualTo("s1");
        assertThat(written.getFromUser()).isEqualTo("alice");
        assertThat(written.getCreated()).isEqualTo(111L);
        verify(safewordMapper, never()).insert(written);
    }

    @Test
    void queriesMapRowsBackIntoSafewords() {
        CoupleCatchSafewordPO row = new CoupleCatchSafewordPO();
        row.setId("sw2");
        row.setSpaceId("s1");
        row.setFromUser("bob");
        row.setWord("缓缓");
        row.setNote("先冷静");
        when(safewordMapper.findBySpace("s1")).thenReturn(List.of(row));
        when(safewordMapper.find("s1", "bob")).thenReturn(row);

        assertThat(adapter.listBySpace("s1")).extracting(Safeword::word).containsExactly("缓缓");
        Optional<Safeword> found = adapter.findBySpaceAndUser("s1", "bob");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().id()).isEqualTo("sw2");
        assertThat(found.orElseThrow().fromUser()).isEqualTo("bob");
    }
}
