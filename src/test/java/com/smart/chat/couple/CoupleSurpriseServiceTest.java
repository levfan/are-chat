package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
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
 * 惊喜与期待核心逻辑单测：刮刮乐周卡懒生成与归属校验、盲盒开箱规则、思念速递限流。
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
    private CoupleSweetAlarmMapper alarmMapper;

    @Mock
    private CoupleMissExpressMapper missMapper;

    @Mock
    private CoupleTreasureMapper treasureMapper;

    @Mock
    private CoupleConfessionMapper confessionMapper;

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

    @Test
    void sendMissRejectedWhileOneInTransit() {
        stubSpace("alice");
        CoupleMissExpress inTransit = CoupleMissExpress.of("s1", "alice", System.currentTimeMillis() + 60_000);
        when(missMapper.findByUser("s1", "alice")).thenReturn(List.of(inTransit));

        assertThatThrownBy(() -> service.sendMiss("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还在路上");
        verify(missMapper, never()).insert(any(CoupleMissExpress.class));

        // 送达后可再寄
        inTransit.setDelivered(true);
        service.sendMiss("alice");
        ArgumentCaptor<CoupleMissExpress> captor = ArgumentCaptor.forClass(CoupleMissExpress.class);
        verify(missMapper).insert(captor.capture());
        assertThat(captor.getValue().getDeliverAt()).isGreaterThan(System.currentTimeMillis());
    }

    @Test
    void treasurePrizeHiddenFromDiggerUntilDone() {
        stubSpace("bob");
        CoupleTreasure treasure = CoupleTreasure.of("s1", "alice", "去阳台看看", "一个大拥抱");
        when(treasureMapper.findBySpace("s1")).thenReturn(List.of(treasure));

        // 未完成时对挖宝人隐藏奖品
        List<CoupleSurpriseService.TreasureVO> hidden = service.treasures("bob");
        assertThat(hidden.get(0).prizeText()).isNull();

        // 完成后揭晓
        stubSpace("bob");
        when(treasureMapper.selectById("t1")).thenReturn(treasure);
        CoupleSurpriseService.TreasureVO revealed = service.completeTreasure("bob", "t1");
        assertThat(revealed.prizeText()).isEqualTo("一个大拥抱");
        assertThat(treasure.getStatus()).isEqualTo(CoupleTreasure.STATUS_DONE);
        verify(push).pushCoupleEventBoth(eq("treasure-done"), eq("bob"), eq("alice"), eq("bob"), any());
    }
}
