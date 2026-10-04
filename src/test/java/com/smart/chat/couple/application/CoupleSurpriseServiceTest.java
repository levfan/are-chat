package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.points.PointEntry;
import com.smart.chat.couple.domain.points.PointLedgerRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.surprise.MysteryBox;
import com.smart.chat.couple.domain.surprise.MysteryBoxRepository;
import com.smart.chat.couple.domain.surprise.Scratch;
import com.smart.chat.couple.domain.surprise.ScratchRepository;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 刮刮乐与盲盒（保留卡 `couple-surprise`）单测：周卡懒生成与归属校验、核销只给送券人记一次分、
 * 盲盒到日才能拆且装盒人不能自拆。
 * 心动闹钟/思念速递/藏宝图/告白重现 随功能下线，对应用例一并删除。
 * <p>
 * 假表建在端口这一层（{@link ScratchRepository}／{@link MysteryBoxRepository}／
 * {@link PointLedgerRepository}），PO 与 Mapper 不出现在用例里；期望值与改造前逐字相同。
 */
@ExtendWith(MockitoExtension.class)
class CoupleSurpriseServiceTest {

    @Mock
    private CoupleSpaceRepository spaceRepository;

    @Mock
    private ScratchRepository scratchRepository;

    @Mock
    private MysteryBoxRepository boxRepository;

    @Mock
    private PointLedgerRepository ledgerRepository;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleSurpriseService service;

    private CoupleSpace space() {
        return CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 0L, null, null, null, null, null, null, null);
    }

    private void stubSpace(String me) {
        lenient().when(spaceRepository.findActiveByMember(me)).thenReturn(Optional.of(space()));
    }

    @Test
    void myScratchesLazilyCreatesTwoWeekCards() {
        stubSpace("alice");
        when(scratchRepository.findByWeek(eq("s1"), any())).thenReturn(List.of());
        when(scratchRepository.findByOwner("s1", "alice")).thenReturn(List.of());

        service.myScratches("alice");

        ArgumentCaptor<Scratch> captor = ArgumentCaptor.forClass(Scratch.class);
        verify(scratchRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<Scratch> cards = captor.getAllValues();
        // 双方各一张：一张 alice 收（bob 送），一张 bob 收（alice 送）
        assertThat(cards).extracting(Scratch::owner).containsExactlyInAnyOrder("alice", "bob");
        assertThat(cards).allSatisfy(c -> {
            assertThat(c.weekKey()).matches("\\d{4}-W\\d{2}");
            assertThat(c.prizeText()).isNotBlank();
        });
    }

    @Test
    void scratchRejectsNonOwnerAndKeepsPrizeHiddenUntilScratched() {
        stubSpace("alice");
        Scratch card = Scratch.issue("s1", "2026-W40", "alice", "bob", "hug", "一个抱抱");
        when(scratchRepository.findByIdIn("sc1", "s1")).thenReturn(Optional.of(card));

        // alice 是送券人，不能替 bob 刮
        assertThatThrownBy(() -> service.scratch("alice", "sc1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能替 TA 刮");

        // bob 刮开后券面揭晓并推送通知
        stubSpace("bob");
        service.scratch("bob", "sc1");
        assertThat(card.scratched()).isTrue();
        assertThat(card.scratchedAt()).isNotNull();
        verify(push).pushCoupleEvent(eq("scratch-scratched"), eq("bob"), eq("alice"), any());
    }

    @Test
    void redeemPaysTheGiverOnce() {
        stubSpace("alice");
        Scratch card = Scratch.issue("s1", "2026-W40", "alice", "bob", "hug", "一个抱抱");
        card.scratchBy("bob");
        List<PointEntry> ledger = new ArrayList<>();
        when(scratchRepository.findByIdIn(any(), any())).thenReturn(Optional.of(card));
        lenient().doAnswer(inv -> {
            ledger.add((PointEntry) inv.getArgument(0));
            return null;
        }).when(ledgerRepository).append(any(PointEntry.class));

        service.redeemScratch("alice", card.id());

        // 券是送的人兑现的，分记在送券人 alice 头上，且必须真插进台账
        assertThat(ledger).hasSize(1);
        assertThat(ledger.get(0).fromUser()).isEqualTo("alice");
        assertThat(ledger.get(0).type()).isEqualTo(PointEntry.TYPE_EARN);
        assertThat(ledger.get(0).points()).isEqualTo(CoupleSurpriseService.SCRATCH_POINTS);
        assertThat(ledger.get(0).item()).startsWith(CoupleSurpriseService.SCRATCH_REASON_PREFIX);

        // 重复点核销： redeemedAt 闸门挡住，不再补分
        service.redeemScratch("alice", card.id());
        assertThat(ledger).hasSize(1);
    }

    @Test
    void boxCannotBeOpenedBeforeOpenDayOrBySealer() {
        stubSpace("bob");
        String tomorrow = LocalDate.now().plusDays(1).toString();
        MysteryBox box = MysteryBox.restore("b1", "s1", "alice", MysteryBox.KIND_WHISPER, "喜欢你", tomorrow,
                false, null, 1L);
        when(boxRepository.findByIdIn("b1", "s1")).thenReturn(Optional.of(box));

        // 未到开箱日
        assertThatThrownBy(() -> service.openBox("bob", "b1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没到开箱日");

        // 到了开箱日也不能自己拆
        stubSpace("alice");
        MysteryBox ready = MysteryBox.restore("b1", "s1", "alice", MysteryBox.KIND_WHISPER, "喜欢你",
                LocalDate.now().toString(), false, null, 1L);
        when(boxRepository.findByIdIn("b1", "s1")).thenReturn(Optional.of(ready));
        assertThatThrownBy(() -> service.openBox("alice", "b1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己装的盒子");
    }
}
