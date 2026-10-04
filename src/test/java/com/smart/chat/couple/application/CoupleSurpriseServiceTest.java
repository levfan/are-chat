package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.persistence.CoupleMysteryBox;
import com.smart.chat.couple.infrastructure.persistence.CoupleMysteryBoxMapper;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedger;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleScratch;
import com.smart.chat.couple.infrastructure.persistence.CoupleScratchMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpace;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 刮刮乐与盲盒（保留卡 `couple-surprise`）单测：周卡懒生成与归属校验、核销只给送券人记一次分、
 * 盲盒到日才能拆且装盒人不能自拆。
 * 心动闹钟/思念速递/藏宝图/告白重现 随功能下线，对应用例一并删除。
 */
@ExtendWith(MockitoExtension.class)
class CoupleSurpriseServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;

    @Mock
    private CoupleScratchMapper scratchMapper;

    @Mock
    private CoupleMysteryBoxMapper boxMapper;

    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleSurpriseService service;

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        return space;
    }

    private void stubSpace(String me) {
        lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(space()));
    }

    @Test
    void myScratchesLazilyCreatesTwoWeekCards() {
        stubSpace("alice");
        when(scratchMapper.findByWeek(eq("s1"), any())).thenReturn(List.of());
        when(scratchMapper.findByOwner("s1", "alice")).thenReturn(List.of());

        service.myScratches("alice");

        ArgumentCaptor<CoupleScratch> captor = ArgumentCaptor.forClass(CoupleScratch.class);
        verify(scratchMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        List<CoupleScratch> cards = captor.getAllValues();
        // 双方各一张：一张 alice 收（bob 送），一张 bob 收（alice 送）
        assertThat(cards).extracting(CoupleScratch::getOwner).containsExactlyInAnyOrder("alice", "bob");
        assertThat(cards).allSatisfy(c -> {
            assertThat(c.getWeekKey()).matches("\\d{4}-W\\d{2}");
            assertThat(c.getPrizeText()).isNotBlank();
        });
    }

    @Test
    void scratchRejectsNonOwnerAndKeepsPrizeHiddenUntilScratched() {
        stubSpace("alice");
        CoupleScratch card = CoupleScratch.of("s1", "2026-W40", "alice", "bob", "hug", "一个抱抱");
        when(scratchMapper.selectById("sc1")).thenReturn(card);

        // alice 是送券人，不能替 bob 刮
        assertThatThrownBy(() -> service.scratch("alice", "sc1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能替 TA 刮");

        // bob 刮开后券面揭晓并推送通知
        stubSpace("bob");
        service.scratch("bob", "sc1");
        assertThat(card.isScratched()).isTrue();
        assertThat(card.getScratchedAt()).isNotNull();
        verify(push).pushCoupleEvent(eq("scratch-scratched"), eq("bob"), eq("alice"), any());
    }

    @Test
    void redeemPaysTheGiverOnce() {
        stubSpace("alice");
        CoupleScratch card = CoupleScratch.of("s1", "2026-W40", "alice", "bob", "hug", "一个抱抱");
        card.setScratched(true);
        card.setScratchedAt(System.currentTimeMillis());
        java.util.List<CouplePointLedger> ledger = new java.util.ArrayList<>();
        when(scratchMapper.selectById(any())).thenReturn(card);
        when(scratchMapper.updateById(any(CoupleScratch.class))).thenReturn(1);
        when(ledgerMapper.insert(any(CouplePointLedger.class))).thenAnswer(inv -> {
            ledger.add(inv.getArgument(0));
            return 1;
        });

        service.redeemScratch("alice", card.getId());

        // 券是送的人兑现的，分记在送券人 alice 头上，且必须真插进台账
        assertThat(ledger).hasSize(1);
        assertThat(ledger.get(0).getFromUser()).isEqualTo("alice");
        assertThat(ledger.get(0).getType()).isEqualTo(CouplePointLedger.TYPE_EARN);
        assertThat(ledger.get(0).getPoints()).isEqualTo(CoupleSurpriseService.SCRATCH_POINTS);
        assertThat(ledger.get(0).getItem()).startsWith(CoupleSurpriseService.SCRATCH_REASON_PREFIX);

        // 重复点核销： redeemedAt 闸门挡住，不再补分
        service.redeemScratch("alice", card.getId());
        assertThat(ledger).hasSize(1);
    }

    @Test
    void boxCannotBeOpenedBeforeOpenDayOrBySealer() {
        stubSpace("bob");
        String tomorrow = LocalDate.now().plusDays(1).toString();
        CoupleMysteryBox box = CoupleMysteryBox.of("s1", "alice", CoupleMysteryBox.KIND_WHISPER, "喜欢你", LocalDate.now().plusDays(1));
        when(boxMapper.selectById("b1")).thenReturn(box);

        // 未到开箱日
        assertThatThrownBy(() -> service.openBox("bob", "b1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还没到开箱日");

        // 到了开箱日也不能自己拆
        stubSpace("alice");
        CoupleMysteryBox ready = CoupleMysteryBox.of("s1", "alice", CoupleMysteryBox.KIND_WHISPER, "喜欢你", LocalDate.now());
        when(boxMapper.selectById("b1")).thenReturn(ready);
        assertThatThrownBy(() -> service.openBox("alice", "b1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己装的盒子");
    }
}
