package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 二人制造厂（F270-F279）单测：轮盘一周一转与交替分配、认账/干完守卫与清空 both、
 * 采买买回推登记人与月榜、冰箱同名复活与临期聚合、快递自接禁止与送达感谢章入账、
 * 叫醒每日一卡、服药提醒链连续与本人报服、久坐同起 1h 窗口只推一次、
 * 垫付欠款方清账、战利品一猜一分、家安六项齐与双签 both、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleFactoryServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleSpinTaskMapper spinMapper;
    @Mock
    private CoupleShopItemMapper shopMapper;
    @Mock
    private CoupleStockMapper stockMapper;
    @Mock
    private CoupleParcelMapper parcelMapper;
    @Mock
    private CoupleWakeWordMapper wakeMapper;
    @Mock
    private CoupleMedicineMapper medMapper;
    @Mock
    private CoupleStandupMapper standMapper;
    @Mock
    private CoupleAdvanceMapper advanceMapper;
    @Mock
    private CoupleGroceryMapper groceryMapper;
    @Mock
    private CoupleHomeCheckMapper checkMapper;
    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleFactoryService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String WEEK = LocalDate.now().with(DayOfWeek.MONDAY).toString();
    private static final String MONTH = DAY.substring(0, 7);

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        return space;
    }

    private void stubSpace() {
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space()));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space()));
    }

    // ========== F270 轮盘 ==========

    @Test
    void spinOncePerWeekAlternatesAndClearPushesBoth() {
        stubSpace();
        List<CoupleSpinTask> rows = new ArrayList<>();
        lenient().when(spinMapper.findByWeek(eq("s1"), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getWeek().equals(inv.getArgument(1))).toList());
        when(spinMapper.insert(any(CoupleSpinTask.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(spinMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));

        service.spin("alice", "吸尘,洗碗,倒垃圾,擦桌子");
        assertThat(rows).hasSize(4);
        long aliceTasks = rows.stream().filter(r -> r.getAssignedUser().equals("alice")).count();
        assertThat(aliceTasks).isEqualTo(2);
        verify(push).pushCoupleEventBoth(eq("factory-spin-open"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.spin("bob", "拖地,擦窗"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("转过盘了");

        CoupleSpinTask aliceOne = rows.stream().filter(r -> r.getAssignedUser().equals("alice"))
                .findFirst().orElseThrow();
        assertThatThrownBy(() -> service.doneSpin("alice", aliceOne.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("认账");
        service.confirmSpin("bob", aliceOne.getId());
        assertThatThrownBy(() -> service.confirmSpin("alice", aliceOne.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己的活自己认");
        assertThatThrownBy(() -> service.doneSpin("bob", aliceOne.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不是你的");
        service.doneSpin("alice", aliceOne.getId());
        verify(push).pushCoupleEvent(eq("factory-spin-item-done"), eq("alice"), eq("bob"), any());
        for (CoupleSpinTask t : rows) {
            String doer = t.getAssignedUser();
            if (!t.isConfirmed()) {
                service.confirmSpin(doer.equals("alice") ? "bob" : "alice", t.getId());
            }
            if (!t.isDone()) {
                service.doneSpin(doer, t.getId());
            }
        }
        verify(push, times(1)).pushCoupleEventBoth(eq("factory-spin-clear"), any(), any(), any(), any());
    }

    // ========== F271 采买 ==========

    @Test
    void shopBoughtPushesRegistrantOnce() {
        stubSpace();
        List<CoupleShopItem> rows = new ArrayList<>();
        lenient().when(shopMapper.findOpen("s1")).thenAnswer(inv -> List.copyOf(rows));
        lenient().when(shopMapper.findDoneSince(eq("s1"), any(Long.class))).thenReturn(List.of());
        when(shopMapper.insert(any(CoupleShopItem.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(shopMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        service.shopAdd("alice", "酱油", "一瓶");
        CoupleShopItem item = rows.get(0);
        service.shopDone("bob", item.getId());
        verify(push).pushCoupleEvent(eq("factory-shop-bought"), eq("bob"), eq("alice"), any());
        service.shopDone("bob", item.getId());
        verify(push, times(1)).pushCoupleEvent(eq("factory-shop-bought"), any(), any(), any());
    }

    // ========== F272 冰箱 ==========

    @Test
    void stockReviveAndExpiringAggregate() {
        stubSpace();
        List<CoupleStock> rows = new ArrayList<>();
        lenient().when(stockMapper.findIn("s1")).thenAnswer(inv -> rows.stream()
                .filter(r -> CoupleStock.STATUS_IN.equals(r.getStatus())).toList());
        lenient().when(stockMapper.findItem("s1", "番茄")).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getItem().equals("番茄")).findFirst().orElse(null));
        when(stockMapper.insert(any(CoupleStock.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(stockMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        String soon = LocalDate.now().plusDays(2).toString();
        service.stockAdd("alice", "番茄", "3个", soon);
        CoupleStock row = rows.get(0);
        service.stockOut("bob", row.getId());
        assertThat(row.getStatus()).isEqualTo(CoupleStock.STATUS_OUT);
        service.stockAdd("bob", "番茄", "两把", soon);
        assertThat(rows).hasSize(1);
        assertThat(row.getStatus()).isEqualTo(CoupleStock.STATUS_IN);
        assertThat(service.board("alice").expiring()).anyMatch(line -> line.contains("番茄"));
    }

    // ========== F273 快递 ==========

    @Test
    void parcelFlowEarnsLedgerStamp() {
        stubSpace();
        List<CoupleParcel> rows = new ArrayList<>();
        lenient().when(parcelMapper.findOpen("s1")).thenAnswer(inv -> rows.stream()
                .filter(r -> !CoupleParcel.STATUS_DONE.equals(r.getStatus())).toList());
        when(parcelMapper.insert(any(CoupleParcel.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(parcelMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        service.parcelNew("alice", "3 号柜 大件");
        CoupleParcel p = rows.get(0);
        assertThatThrownBy(() -> service.parcelGrab("alice", p.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("乐于助人");
        service.parcelGrab("bob", p.getId());
        assertThatThrownBy(() -> service.parcelDone("alice", p.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("谁领的单");
        service.parcelDone("bob", p.getId());
        ArgumentCaptor<CouplePointLedger> cap = ArgumentCaptor.forClass(CouplePointLedger.class);
        verify(ledgerMapper).insert(cap.capture());
        assertThat(cap.getValue().getType()).isEqualTo(CouplePointLedger.TYPE_EARN);
        assertThat(cap.getValue().getPoints()).isEqualTo(CoupleFactoryBank.PARCEL_EARN_POINTS);
        verify(push).pushCoupleEventBoth(eq("factory-parcel-done"), eq("bob"), eq("alice"), eq("bob"), any());
    }

    // ========== F274 叫醒 ==========

    @Test
    void wakeGiveOncePerDayOnlyForPartnerWord() {
        stubSpace();
        List<CoupleWakeWord> rows = new ArrayList<>();
        lenient().when(wakeMapper.findByWeek("s1", WEEK)).thenAnswer(inv -> List.copyOf(rows));
        lenient().when(wakeMapper.find("s1", WEEK, "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getFromUser().equals("alice")).findFirst().orElse(null));
        lenient().when(wakeMapper.find("s1", WEEK, "bob")).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getFromUser().equals("bob")).findFirst().orElse(null));
        when(wakeMapper.insert(any(CoupleWakeWord.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        assertThatThrownBy(() -> service.wakeGive("bob"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没定叫醒词");
        service.wakeSet("alice", "太阳晒屁股啦");
        service.wakeGive("bob");
        verify(push).pushCoupleEvent(eq("factory-wake-given"), eq("bob"), eq("alice"), any());
        assertThatThrownBy(() -> service.wakeGive("bob"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("再睡会儿");
    }

    // ========== F275 服药链 ==========

    @Test
    void medicineChainGuarded() {
        stubSpace();
        List<CoupleMedicine> rows = new ArrayList<>();
        lenient().when(medMapper.findOngoing("s1")).thenAnswer(inv -> rows.stream()
                .filter(r -> CoupleMedicine.STATUS_ONGOING.equals(r.getStatus())).toList());
        lenient().when(medMapper.find("s1", "降压药", "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getName().equals("降压药") && r.getFromUser().equals("alice"))
                .findFirst().orElse(null));
        when(medMapper.insert(any(CoupleMedicine.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(medMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        service.medAdd("alice", "降压药", "早饭后");
        assertThatThrownBy(() -> service.medAdd("alice", "降压药", "早饭后"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经登记");
        CoupleMedicine med = rows.get(0);
        assertThatThrownBy(() -> service.medRemind("alice", med.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("对方做的事");
        service.medRemind("bob", med.getId());
        service.medRemind("bob", med.getId());
        verify(push, times(1)).pushCoupleEvent(eq("factory-med-remind"), any(), any(), any());
        assertThatThrownBy(() -> service.medTaken("bob", med.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("本人来报");
        service.medTaken("alice", med.getId());
        assertThat(med.getStreak()).isEqualTo(1);
        med.setLastTakenDay(LocalDate.now().minusDays(1).toString());
        med.setStreak(4);
        service.medTaken("alice", med.getId());
        assertThat(med.getStreak()).isEqualTo(5);
    }

    // ========== F276 久坐同起 ==========

    @Test
    void standupPairWithinHourPushesBothOnce() {
        stubSpace();
        List<CoupleStandup> rows = new ArrayList<>();
        lenient().when(standMapper.find(eq("s1"), eq(DAY), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getFromUser().equals(inv.getArgument(2))).findFirst().orElse(null));
        lenient().when(standMapper.findRecent("s1", WEEK)).thenAnswer(inv -> List.copyOf(rows));
        when(standMapper.insert(any(CoupleStandup.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.standup("alice");
        verify(push).pushCoupleEvent(eq("factory-standup-tap"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.standup("alice"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("拍过");
        service.standup("bob");
        verify(push).pushCoupleEventBoth(eq("factory-standup-both"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(rows).allMatch(CoupleStandup::isPaired);
    }

    // ========== F277 垫付本 ==========

    @Test
    void advanceSettleByDebtorOnly() {
        stubSpace();
        List<CoupleAdvance> rows = new ArrayList<>();
        lenient().when(advanceMapper.findOpen("s1")).thenAnswer(inv -> rows.stream()
                .filter(r -> CoupleAdvance.STATUS_OPEN.equals(r.getStatus())).toList());
        when(advanceMapper.insert(any(CoupleAdvance.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(advanceMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        service.advanceAdd("alice", "机票", 234500, "往返");
        CoupleAdvance a = rows.get(0);
        assertThatThrownBy(() -> service.advanceSettle("alice", a.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还钱的一方");
        assertThatThrownBy(() -> service.advanceAdd("bob", "坏金额", -1, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("金额");
        service.advanceSettle("bob", a.getId());
        verify(push).pushCoupleEventBoth(eq("factory-advance-settled"), eq("bob"), eq("alice"), eq("bob"), any());
    }

    // ========== F278 战利品 ==========

    @Test
    void groceryGuessOnceAndRateOnce() {
        stubSpace();
        List<CoupleGrocery> rows = new ArrayList<>();
        lenient().when(groceryMapper.findByWeek("s1", WEEK)).thenAnswer(inv -> List.copyOf(rows));
        lenient().when(groceryMapper.find("s1", WEEK, "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getFromUser().equals("alice")).findFirst().orElse(null));
        when(groceryMapper.insert(any(CoupleGrocery.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(groceryMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        service.grocery("alice", "枸杞,电动牙刷,电影票,蛋糕粉");
        CoupleGrocery g = rows.get(0);
        assertThatThrownBy(() -> service.groceryRate("alice", g.getId(), 3))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没交猜测");
        service.groceryGuess("bob", g.getId(), "养生+约会三件套");
        assertThatThrownBy(() -> service.groceryGuess("bob", g.getId(), "再猜一次"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("一次机会");
        assertThatThrownBy(() -> service.groceryRate("bob", g.getId(), 3))
                .isInstanceOf(BusinessException.class).hasMessageContaining("买家能给");
        service.groceryRate("alice", g.getId(), 4);
        verify(push).pushCoupleEventBoth(eq("factory-grocery-score"), eq("alice"), eq("alice"), eq("bob"), any());
        service.groceryRate("alice", g.getId(), 5);
        verify(push, times(1)).pushCoupleEventBoth(eq("factory-grocery-score"), any(), any(), any(), any());
    }

    // ========== F279 家安月检 ==========

    @Test
    void homeCheckNeedsAllSixAndBothPush() {
        stubSpace();
        List<CoupleHomeCheck> rows = new ArrayList<>();
        lenient().when(checkMapper.findByMonth("s1", MONTH)).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getMonth().equals(MONTH)).toList());
        lenient().when(checkMapper.find("s1", MONTH, "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getFromUser().equals("alice")).findFirst().orElse(null));
        lenient().when(checkMapper.find("s1", MONTH, "bob")).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getFromUser().equals("bob")).findFirst().orElse(null));
        when(checkMapper.insert(any(CoupleHomeCheck.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        assertThatThrownBy(() -> service.homeCheck("alice", "GAS,WATER,ELEC"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("六项都要勾");
        service.homeCheck("alice", "GAS,WATER,ELEC,WINDOW,LOCK,FIRSTAID");
        verify(push).pushCoupleEvent(eq("factory-homecheck-mine"), eq("alice"), eq("bob"), any());
        service.homeCheck("bob", "GAS,WATER,ELEC,WINDOW,LOCK,FIRSTAID");
        verify(push).pushCoupleEventBoth(eq("factory-homecheck-both"), eq("bob"), eq("alice"), eq("bob"), any());
        service.homeCheck("bob", "GAS,WATER,ELEC,WINDOW,LOCK,FIRSTAID");
        verify(push, times(1)).pushCoupleEventBoth(eq("factory-homecheck-both"), any(), any(), any(), any());
        assertThat(service.board("alice").check().bothIn()).isTrue();
    }

    // ========== 兜底 ==========

    @Test
    void noSpaceLeadsTo404() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("solo"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没有建立情侣空间");
    }
}
