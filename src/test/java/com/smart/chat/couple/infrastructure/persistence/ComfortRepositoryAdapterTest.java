package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.comfort.ComfortRequest;
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
 * 求抱抱仓储适配器：验证「只回写聚合纳管的列」这条纪律。
 * 空间、求的人、发生日、创建时刻都不归聚合改，用聚合重建整行会把它们静默改写；
 * 另外锁住 handled/heldAt 的翻译——库里改写感受后可能留着上一次的接住残值，
 * 只有 handled 一列说了准。
 */
@ExtendWith(MockitoExtension.class)
class ComfortRepositoryAdapterTest {

    @Mock
    private CoupleComfortMapper comfortMapper;

    @InjectMocks
    private ComfortRepositoryAdapter adapter;

    @Test
    void newRequestIsInsertedWithAllColumns() {
        when(comfortMapper.selectById(any())).thenReturn(null);
        ComfortRequest request = ComfortRequest.askOn("s1", "alice", "2026-10-05", "SAD");

        adapter.save(request);

        ArgumentCaptor<CoupleComfortPO> captor = ArgumentCaptor.forClass(CoupleComfortPO.class);
        verify(comfortMapper).insert(captor.capture());
        CoupleComfortPO po = captor.getValue();
        assertThat(po.getId()).isEqualTo(request.id());
        assertThat(po.getSpaceId()).isEqualTo("s1");
        assertThat(po.getFromUser()).isEqualTo("alice");
        assertThat(po.getDay()).isEqualTo("2026-10-05");
        assertThat(po.getFeeling()).isEqualTo("SAD");
        assertThat(po.isHandled()).isFalse();
        assertThat(po.getCreated()).isNotNull();
        verify(comfortMapper, never()).updateById(any(CoupleComfortPO.class));
    }

    @Test
    void updateOnlyWritesFeelingHandledNoteAndHandledAt() {
        CoupleComfortPO stored = row("c1", "alice", "SAD", false, null, null);
        when(comfortMapper.selectById("c1")).thenReturn(stored);
        ComfortRequest request = ComfortRequest.restore("c1", "alice", "SAD", null, null, false,
                "s1", "2026-10-05", 111L);
        request.hold("我在", 222L);

        adapter.save(request);

        ArgumentCaptor<CoupleComfortPO> captor = ArgumentCaptor.forClass(CoupleComfortPO.class);
        verify(comfortMapper).updateById(captor.capture());
        CoupleComfortPO written = captor.getValue();
        assertThat(written.isHandled()).isTrue();
        assertThat(written.getHandledNote()).isEqualTo("我在");
        assertThat(written.getHandledAt()).isEqualTo(222L);
        assertThat(written.getCreated()).as("创建时刻不归聚合维护").isEqualTo(111L);
        assertThat(written.getFromUser()).isEqualTo("alice");
        assertThat(written.getDay()).isEqualTo("2026-10-05");
        verify(comfortMapper, never()).insert(any(CoupleComfortPO.class));
    }

    @Test
    void reaskWritesHandledFalseEvenWhenTheOldNoteIsStillInRow() {
        CoupleComfortPO stored = row("c2", "alice", "SAD", true, "上次的话", 999L);
        when(comfortMapper.selectById("c2")).thenReturn(stored);
        ComfortRequest request = ComfortRequest.restore("c2", "alice", "SAD", "上次的话", 999L, true,
                "s1", "2026-10-05", 111L);
        request.reask("TIRED");

        adapter.save(request);

        ArgumentCaptor<CoupleComfortPO> captor = ArgumentCaptor.forClass(CoupleComfortPO.class);
        verify(comfortMapper).updateById(captor.capture());
        assertThat(captor.getValue().isHandled()).as("再说一次就回到没被接住").isFalse();
        assertThat(captor.getValue().getFeeling()).isEqualTo("TIRED");
    }

    @Test
    void rowsMapBackIntoAggregatesKeepingTheHandledColumn() {
        CoupleComfortPO held = row("c3", "bob", "EMO", true, "接住了", 555L);
        held.setCreated(7L);
        when(comfortMapper.find("s1", "bob", "2026-10-05")).thenReturn(held);
        when(comfortMapper.findBySpace("s1")).thenReturn(List.of(held));

        Optional<ComfortRequest> found = adapter.findBySpaceAndUserOn("s1", "bob", "2026-10-05");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().handled()).isTrue();
        assertThat(found.orElseThrow().feelingLabel()).isEqualTo("emo");
        assertThat(found.orElseThrow().feelingEmoji()).isEqualTo("🌧️");
        assertThat(found.orElseThrow().created()).isEqualTo(7L);
        assertThat(adapter.listBySpace("s1")).extracting(ComfortRequest::id).containsExactly("c3");
        assertThat(adapter.findBySpaceAndUserOn("s1", "ghost", "2026-10-05")).isEmpty();
    }

    @Test
    void feelingCatalogStaysByteIdenticalToThePoDirectory() {
        // 感受目录是前端在用的契约：PO 那份不许动，领域这份是照搬，逐个字符对齐（含未知键的兜底）。
        for (String feeling : new String[]{"SAD", "WRONGED", "TIRED", "ANXIOUS", "EMO", "HUNGRY"}) {
            ComfortRequest request = ComfortRequest.restore("c-" + feeling, "alice", feeling, null, null);
            assertThat(request.feelingLabel()).isEqualTo(CoupleComfortPO.feelingLabel(feeling));
            assertThat(request.feelingEmoji()).isEqualTo(CoupleComfortPO.feelingEmoji(feeling));
        }
    }

    private CoupleComfortPO row(String id, String fromUser, String feeling, boolean handled, String note,
                                Long handledAt) {
        CoupleComfortPO po = new CoupleComfortPO();
        po.setId(id);
        po.setSpaceId("s1");
        po.setFromUser(fromUser);
        po.setDay("2026-10-05");
        po.setFeeling(feeling);
        po.setHandled(handled);
        po.setHandledNote(note);
        po.setHandledAt(handledAt);
        po.setCreated(111L);
        return po;
    }
}
