package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.surprise.Scratch;
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
 * 刮刮乐仓储适配器：验证「只回写聚合纳管的列」这条纪律。
 * 聚合没纳管的列（券面、送券人、收券人、周锚、创建时刻）必须原样留着——
 * 拿聚合重建整行会把这些列静默清空，而这类 bug 编译与业务用例都照不出来。
 */
@ExtendWith(MockitoExtension.class)
class ScratchRepositoryAdapterTest {

    @Mock
    private CoupleScratchMapper scratchMapper;

    @InjectMocks
    private ScratchRepositoryAdapter adapter;

    @Test
    void newCardIsInsertedWithAllColumns() {
        when(scratchMapper.selectById(any())).thenReturn(null);
        Scratch card = Scratch.issue("s1", "2026-W40", "alice", "bob", "hug", "一个抱抱");

        adapter.save(card);

        ArgumentCaptor<CoupleScratchPO> captor = ArgumentCaptor.forClass(CoupleScratchPO.class);
        verify(scratchMapper).insert(captor.capture());
        CoupleScratchPO po = captor.getValue();
        assertThat(po.getId()).isEqualTo(card.id());
        assertThat(po.getSpaceId()).isEqualTo("s1");
        assertThat(po.getWeekKey()).isEqualTo("2026-W40");
        assertThat(po.getFromUser()).isEqualTo("alice");
        assertThat(po.getOwner()).isEqualTo("bob");
        assertThat(po.getPrizeKind()).isEqualTo("hug");
        assertThat(po.getPrizeText()).isEqualTo("一个抱抱");
        assertThat(po.isScratched()).isFalse();
        assertThat(po.getScratchedAt()).isNull();
        assertThat(po.getRedeemedAt()).isNull();
        verify(scratchMapper, never()).updateById(any(CoupleScratchPO.class));
    }

    @Test
    void updateOnlyWritesScratchAndRedemption() {
        CoupleScratchPO stored = new CoupleScratchPO();
        stored.setId("sc1");
        stored.setSpaceId("s1");
        stored.setWeekKey("2026-W40");
        stored.setFromUser("alice");
        stored.setOwner("bob");
        stored.setPrizeKind("hug");
        stored.setPrizeText("一个抱抱");
        stored.setScratched(false);
        stored.setCreated(111L);
        when(scratchMapper.selectById("sc1")).thenReturn(stored);
        Scratch card = Scratch.restore("sc1", "s1", "2026-W40", "alice", "bob", "hug", "一个抱抱",
                false, null, null, 111L);
        card.scratchBy("bob");
        card.redeemBy("alice");

        adapter.save(card);

        ArgumentCaptor<CoupleScratchPO> captor = ArgumentCaptor.forClass(CoupleScratchPO.class);
        verify(scratchMapper).updateById(captor.capture());
        CoupleScratchPO written = captor.getValue();
        assertThat(written.isScratched()).as("刮开与两个时刻是聚合纳管的列").isTrue();
        assertThat(written.getScratchedAt()).isNotNull();
        assertThat(written.getRedeemedAt()).isNotNull();
        assertThat(written.getPrizeText()).as("券面发出去就冻结").isEqualTo("一个抱抱");
        assertThat(written.getPrizeKind()).isEqualTo("hug");
        assertThat(written.getFromUser()).isEqualTo("alice");
        assertThat(written.getOwner()).isEqualTo("bob");
        assertThat(written.getWeekKey()).isEqualTo("2026-W40");
        assertThat(written.getCreated()).isEqualTo(111L);
        verify(scratchMapper, never()).insert(any(CoupleScratchPO.class));
    }

    @Test
    void queriesMapRowsBackIntoCards() {
        CoupleScratchPO row = new CoupleScratchPO();
        row.setId("sc2");
        row.setSpaceId("s1");
        row.setWeekKey("2026-W40");
        row.setFromUser("bob");
        row.setOwner("alice");
        row.setPrizeKind("movie");
        row.setPrizeText("一场电影");
        row.setScratched(true);
        row.setRedeemedAt(null);
        when(scratchMapper.findByWeek("s1", "2026-W40")).thenReturn(List.of(row));
        when(scratchMapper.findByOwner("s1", "alice")).thenReturn(List.of(row));
        when(scratchMapper.selectById("sc2")).thenReturn(row);

        assertThat(adapter.findByWeek("s1", "2026-W40")).extracting(Scratch::weekKey).containsExactly("2026-W40");
        assertThat(adapter.findByOwner("s1", "alice")).extracting(Scratch::owner).containsExactly("alice");
        Optional<Scratch> found = adapter.findByIdIn("sc2", "s1");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().scratched()).isTrue();
        assertThat(found.orElseThrow().redeemed()).isFalse();
        // 别的空间的券号与空券号都等同于不存在
        assertThat(adapter.findByIdIn("sc2", "other")).isEmpty();
        assertThat(adapter.findByIdIn(null, "s1")).isEmpty();
    }
}
