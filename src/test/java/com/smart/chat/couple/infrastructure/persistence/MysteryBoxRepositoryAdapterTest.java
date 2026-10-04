package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.surprise.MysteryBox;
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
 * 盲盒仓储适配器：验证「只回写聚合纳管的列」这条纪律。
 * 盒子里的话、装盒人、开箱日、创建时刻都是装盒那一刻定死的事实，
 * 更新分支必须原样留着它们——拿聚合重建整行会静默清空，业务用例照不出来。
 */
@ExtendWith(MockitoExtension.class)
class MysteryBoxRepositoryAdapterTest {

    @Mock
    private CoupleMysteryBoxMapper boxMapper;

    @InjectMocks
    private MysteryBoxRepositoryAdapter adapter;

    @Test
    void newBoxIsInsertedWithAllColumns() {
        when(boxMapper.selectById(any())).thenReturn(null);
        MysteryBox box = MysteryBox.pack("alice", MysteryBox.KIND_WHISPER, "喜欢你",
                LocalDate.now().plusDays(2).toString(), LocalDate.now()).intoSpace("s1");

        adapter.save(box);

        ArgumentCaptor<CoupleMysteryBoxPO> captor = ArgumentCaptor.forClass(CoupleMysteryBoxPO.class);
        verify(boxMapper).insert(captor.capture());
        CoupleMysteryBoxPO po = captor.getValue();
        assertThat(po.getId()).isEqualTo(box.id());
        assertThat(po.getSpaceId()).isEqualTo("s1");
        assertThat(po.getFromUser()).isEqualTo("alice");
        assertThat(po.getKind()).isEqualTo(MysteryBox.KIND_WHISPER);
        assertThat(po.getContent()).isEqualTo("喜欢你");
        assertThat(po.getOpenDay()).isEqualTo(LocalDate.now().plusDays(2).toString());
        assertThat(po.isOpened()).isFalse();
        assertThat(po.getOpenedAt()).isNull();
        verify(boxMapper, never()).updateById(any(CoupleMysteryBoxPO.class));
    }

    @Test
    void updateOnlyWritesOpenedAndOpenedAt() {
        CoupleMysteryBoxPO stored = new CoupleMysteryBoxPO();
        stored.setId("b1");
        stored.setSpaceId("s1");
        stored.setFromUser("alice");
        stored.setKind(MysteryBox.KIND_TASK);
        stored.setContent("一起散步");
        stored.setOpenDay(LocalDate.now().toString());
        stored.setOpened(false);
        stored.setCreated(111L);
        when(boxMapper.selectById("b1")).thenReturn(stored);
        MysteryBox box = MysteryBox.restore("b1", "s1", "alice", MysteryBox.KIND_TASK, "一起散步",
                LocalDate.now().toString(), false, null, 111L);
        box.openBy("bob", LocalDate.now());

        adapter.save(box);

        ArgumentCaptor<CoupleMysteryBoxPO> captor = ArgumentCaptor.forClass(CoupleMysteryBoxPO.class);
        verify(boxMapper).updateById(captor.capture());
        CoupleMysteryBoxPO written = captor.getValue();
        assertThat(written.isOpened()).as("拆没拆与拆解时刻是聚合唯一纳管的列").isTrue();
        assertThat(written.getOpenedAt()).isNotNull();
        assertThat(written.getContent()).as("盒子里的话装好即冻结").isEqualTo("一起散步");
        assertThat(written.getKind()).isEqualTo(MysteryBox.KIND_TASK);
        assertThat(written.getFromUser()).isEqualTo("alice");
        assertThat(written.getOpenDay()).isEqualTo(LocalDate.now().toString());
        assertThat(written.getCreated()).isEqualTo(111L);
        verify(boxMapper, never()).insert(any(CoupleMysteryBoxPO.class));
    }

    @Test
    void queriesMapRowsBackIntoBoxes() {
        CoupleMysteryBoxPO row = new CoupleMysteryBoxPO();
        row.setId("b2");
        row.setSpaceId("s1");
        row.setFromUser("bob");
        row.setKind(MysteryBox.KIND_WHISPER);
        row.setContent("老盒子");
        row.setOpenDay("2026-01-01");
        row.setOpened(true);
        row.setOpenedAt(7L);
        when(boxMapper.findBySpace("s1")).thenReturn(List.of(row));
        when(boxMapper.selectById("b2")).thenReturn(row);

        assertThat(adapter.findBySpace("s1")).extracting(MysteryBox::content).containsExactly("老盒子");
        Optional<MysteryBox> found = adapter.findByIdIn("b2", "s1");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().opened()).isTrue();
        assertThat(found.orElseThrow().openedAt()).isEqualTo(7L);
        // 别的空间的盒号与空盒号都等同于不存在
        assertThat(adapter.findByIdIn("b2", "other")).isEmpty();
        assertThat(adapter.findByIdIn(null, "s1")).isEmpty();
    }
}
