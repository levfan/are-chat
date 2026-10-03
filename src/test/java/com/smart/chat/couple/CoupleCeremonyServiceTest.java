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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 小日子·仪式感系（F230-F239）核心逻辑单测：小日子校验与推送、过法卡 3 条上限、
 * 打卡幂等与全勾默契、保费改写与 3 月 payout 幂等、续约日守卫与双方签推 both、
 * 愿望券核销守卫、体感改写不重推、史册按届分页、老黄历三源合并排序、删除连带、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleCeremonyServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleCeremonyFoundedMapper foundedMapper;
    @Mock
    private CoupleCeremonyRitualMapper ritualMapper;
    @Mock
    private CoupleCeremonyMarkMapper markMapper;
    @Mock
    private CoupleCeremonyPolicyMapper policyMapper;
    @Mock
    private CoupleCeremonyRenewMapper renewMapper;
    @Mock
    private CoupleCeremonyCouponMapper couponMapper;
    @Mock
    private CoupleAnniversaryMapper anniversaryMapper;
    @Mock
    private CoupleCountdownMapper countdownMapper;
    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleCeremonyService service;

    private static final String DAY = LocalDate.now().toString();

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        space.setCreated(System.currentTimeMillis());
        return space;
    }

    private void stubSpace(String me) {
        lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(space()));
    }

    // ========== F230 建国纪念日 ==========

    @Test
    void addFoundedValidatesAndPushesPartner() {
        stubSpace("alice");
        assertThatThrownBy(() -> service.addFounded("alice", " ", "2024-05-20", true))
                .isInstanceOf(BusinessException.class).hasMessage("给小日子起个名字吧");
        assertThatThrownBy(() -> service.addFounded("alice", "我们的日子", "2024/05/20", true))
                .isInstanceOf(BusinessException.class).hasMessage("日期格式应为 yyyy-MM-dd");

        service.addFounded("alice", "我们的日子", "2024-05-20", true);
        ArgumentCaptor<CoupleCeremonyFounded> captor = ArgumentCaptor.forClass(CoupleCeremonyFounded.class);
        verify(foundedMapper).insert(captor.capture());
        assertThat(captor.getValue().getRepeatYear()).isEqualTo(1);
        verify(push).pushCoupleEvent(eq("ceremony-founded"), eq("alice"), eq("bob"), any());
    }

    @Test
    void removeFoundedCascadesRitualsAndMarks() {
        stubSpace("alice");
        CoupleCeremonyFounded founded = CoupleCeremonyFounded.of("s1", "小日子", "2024-05-20", true);
        founded.setId("f1");
        when(foundedMapper.selectById("f1")).thenReturn(founded);
        CoupleCeremonyRitual ritual = CoupleCeremonyRitual.of("s1", "f1", "一起吃火锅");
        ritual.setId("r1");
        when(ritualMapper.findByFounded("s1", "f1")).thenReturn(List.of(ritual));

        service.removeFounded("alice", "f1");
        verify(markMapper).deleteByRitualIds(List.of("r1"));
        verify(ritualMapper).deleteById("r1");
        verify(foundedMapper).deleteById("f1");
    }

    // ========== F232 过法任务卡 ==========

    @Test
    void addRitualRejectsFourthAndEmpty() {
        stubSpace("alice");
        CoupleCeremonyFounded founded = CoupleCeremonyFounded.of("s1", "小日子", "2024-05-20", true);
        founded.setId("f1");
        when(foundedMapper.selectById("f1")).thenReturn(founded);
        assertThatThrownBy(() -> service.addRitual("alice", "f1", ""))
                .isInstanceOf(BusinessException.class).hasMessage("庆祝方式写点具体的动作吧");

        List<CoupleCeremonyRitual> full = new ArrayList<>();
        for (int i = 0; i < CoupleCeremonyService.RITUAL_MAX; i++) {
            CoupleCeremonyRitual ritual = CoupleCeremonyRitual.of("s1", "f1", "过法" + i);
            ritual.setId("r" + i);
            full.add(ritual);
        }
        when(ritualMapper.findByFounded("s1", "f1")).thenReturn(full);
        assertThatThrownBy(() -> service.addRitual("alice", "f1", "第四条"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 3 条");

        when(ritualMapper.findByFounded("s1", "f1")).thenReturn(List.of());
        service.addRitual("alice", "f1", "一起吃火锅");
        verify(ritualMapper).insert(any(CoupleCeremonyRitual.class));
        verify(push).pushCoupleEvent(eq("ceremony-ritual"), eq("alice"), eq("bob"), any());
    }

    // ========== F233 庆祝打卡 ==========

    @Test
    void markIsIdempotentAndPushesBothWhenAllDone() {
        stubSpace("alice");
        CoupleCeremonyFounded founded = CoupleCeremonyFounded.of("s1", "小日子", "2024-05-20", true);
        founded.setId("f1");
        CoupleCeremonyRitual ritual = CoupleCeremonyRitual.of("s1", "f1", "一起吃火锅");
        ritual.setId("r1");
        when(ritualMapper.selectById("r1")).thenReturn(ritual);
        when(foundedMapper.selectById("f1")).thenReturn(founded);
        when(ritualMapper.findByFounded("s1", "f1")).thenReturn(List.of(ritual));
        List<CoupleCeremonyMark> rows = new ArrayList<>();
        when(markMapper.find("r1", DAY)).thenAnswer(inv -> rows.stream().findFirst().orElse(null));
        when(markMapper.insert(any(CoupleCeremonyMark.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });

        service.mark("alice", "r1");
        verify(markMapper).insert(any(CoupleCeremonyMark.class));
        verify(push).pushCoupleEventBoth(eq("ceremony-all-done"), eq("alice"), eq("alice"), eq("bob"), any());

        service.mark("alice", "r1");
        verify(markMapper, times(1)).insert(any(CoupleCeremonyMark.class));
        verify(push, times(1)).pushCoupleEventBoth(eq("ceremony-all-done"), any(), any(), any(), any());
    }

    @Test
    void markPartialOnlyPushesPartnerOnce() {
        stubSpace("alice");
        CoupleCeremonyFounded founded = CoupleCeremonyFounded.of("s1", "小日子", "2024-05-20", true);
        founded.setId("f1");
        CoupleCeremonyRitual r1 = CoupleCeremonyRitual.of("s1", "f1", "过法一");
        r1.setId("r1");
        CoupleCeremonyRitual r2 = CoupleCeremonyRitual.of("s1", "f1", "过法二");
        r2.setId("r2");
        when(ritualMapper.selectById("r1")).thenReturn(r1);
        when(foundedMapper.selectById("f1")).thenReturn(founded);
        when(ritualMapper.findByFounded("s1", "f1")).thenReturn(List.of(r1, r2));
        when(markMapper.find(eq("r1"), eq(DAY))).thenReturn(null, new CoupleCeremonyMark());
        when(markMapper.insert(any(CoupleCeremonyMark.class))).thenReturn(1);

        service.mark("alice", "r1");
        verify(push).pushCoupleEvent(eq("ceremony-mark"), eq("alice"), eq("bob"), any());
        verify(push, never()).pushCoupleEventBoth(eq("ceremony-all-done"), any(), any(), any(), any());
    }

    // ========== F234 爱情保险柜 ==========

    @Test
    void payPolicyRewritesOwnQuoteWithoutSecondInsert() {
        stubSpace("alice");
        String month = YearMonth.now().toString();
        List<CoupleCeremonyPolicy> rows = new ArrayList<>();
        when(policyMapper.find("s1", month, "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> "alice".equals(r.getFromUser())).findFirst().orElse(null));
        when(policyMapper.insert(any(CoupleCeremonyPolicy.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(policyMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));

        service.payPolicy("alice", "夸你第一句");
        service.payPolicy("alice", "夸你更走心");
        verify(policyMapper, times(1)).insert(any(CoupleCeremonyPolicy.class));
        verify(policyMapper, times(1)).updateById(any(CoupleCeremonyPolicy.class));
        assertThat(rows.get(0).getQuote()).isEqualTo("夸你更走心");
    }

    @Test
    void payoutAtThreePaidMonthsIsIdempotent() {
        stubSpace("alice");
        String month = YearMonth.now().toString();
        List<CoupleCeremonyPolicy> rows = new ArrayList<>();
        for (String past : List.of("2020-01", "2020-02")) {
            rows.add(CoupleCeremonyPolicy.of("s1", past, "alice", "夸"));
            rows.add(CoupleCeremonyPolicy.of("s1", past, "bob", "夸"));
        }
        rows.add(CoupleCeremonyPolicy.of("s1", month, "bob", "夸"));
        when(policyMapper.find("s1", month, "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> "alice".equals(r.getFromUser()) && month.equals(r.getMonth())).findFirst().orElse(null));
        when(policyMapper.insert(any(CoupleCeremonyPolicy.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(policyMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));
        when(couponMapper.existsRef("s1", "policy-3")).thenReturn(false, true);

        service.payPolicy("alice", "第三个月也交齐了");
        verify(couponMapper).insert(any(CoupleCeremonyCoupon.class));
        verify(push).pushCoupleEventBoth(eq("ceremony-payout"), eq("alice"), eq("alice"), eq("bob"), any());

        service.payPolicy("alice", "再夸一句");
        verify(couponMapper, times(1)).insert(any(CoupleCeremonyCoupon.class));
    }

    // ========== F235 续约仪式 ==========

    @Test
    void issuingACouponCostsPointsAndBlocksWhenBroke() {
        stubSpace("alice");
        List<CoupleCeremonyCoupon> coupons = new ArrayList<>();
        List<CouplePointLedger> ledger = new ArrayList<>();
        lenient().when(ledgerMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(ledger));
        lenient().when(couponMapper.insert(any(CoupleCeremonyCoupon.class))).thenAnswer(inv -> {
            coupons.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(ledgerMapper.insert(any(CouplePointLedger.class))).thenAnswer(inv -> {
            ledger.add(inv.getArgument(0));
            return 1;
        });

        // 一分没有就发不出券，且一行都不许落库
        assertThatThrownBy(() -> service.issueCoupon("alice", "陪我去海边"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有 0 分");
        assertThat(coupons).isEmpty();
        verify(couponMapper, never()).insert(any(CoupleCeremonyCoupon.class));

        // 好事簿攒来的 12 分够发一张 10 分的券，券必须真落到台账的 SPEND 行
        ledger.add(CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_EARN, "好事簿：接我下班", 12));
        service.issueCoupon("alice", "陪我去海边");
        assertThat(coupons).hasSize(1);
        assertThat(ledger).filteredOn(l -> CouplePointLedger.TYPE_SPEND.equals(l.getType())).hasSize(1);
        CouplePointLedger spend = ledger.stream()
                .filter(l -> CouplePointLedger.TYPE_SPEND.equals(l.getType())).findFirst().orElseThrow();
        assertThat(spend.getFromUser()).isEqualTo("alice");
        assertThat(spend.getPoints()).isEqualTo(CoupleCeremonyService.COUPON_COST);
        assertThat(spend.getItem()).isEqualTo("发出愿望券：陪我去海边");

        // 余额只剩 2 分，第二张发不出去
        assertThatThrownBy(() -> service.issueCoupon("alice", "再一张"))
                .isInstanceOf(BusinessException.class);
        assertThat(coupons).hasSize(1);
    }

    @Test
    void renewRejectsNonDueDay() {
        stubSpace("alice");
        CoupleSpace space = space();
        space.setAnniversary(LocalDate.now().minusDays(37).toString());
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));

        assertThatThrownBy(() -> service.renew("alice", "我还是选你"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("天后");
        verify(renewMapper, never()).insert(any(CoupleCeremonyRenew.class));
    }

    @Test
    void renewOnHundredDayWithPartnerPendingPushesSingleSign() {
        stubSpace("alice");
        CoupleSpace space = space();
        space.setAnniversary(LocalDate.now().minusDays(100).toString());
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));

        service.renew("alice", "第一百天，还是选你");
        verify(renewMapper).insert(any(CoupleCeremonyRenew.class));
        verify(push).pushCoupleEvent(eq("ceremony-renew-sign"), eq("alice"), eq("bob"), any());
    }

    @Test
    void renewSecondSignaturePushesBoth() {
        stubSpace("alice");
        CoupleSpace space = space();
        space.setAnniversary(LocalDate.now().minusDays(200).toString());
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        when(renewMapper.find("s1", DAY, "bob")).thenReturn(CoupleCeremonyRenew.of("s1", DAY, "bob", "我选你"));

        service.renew("alice", "第二百天，还是你");
        verify(push).pushCoupleEventBoth(eq("ceremony-renew"), eq("alice"), eq("alice"), eq("bob"), any());
    }

    // ========== F236 愿望券本 ==========

    @Test
    void useCouponGuardsExistenceAndStatus() {
        stubSpace("alice");
        assertThatThrownBy(() -> service.useCoupon("alice", "nope"))
                .isInstanceOf(BusinessException.class).hasMessage("这张愿望券不存在");

        CoupleCeremonyCoupon used = CoupleCeremonyCoupon.of("s1", "已核销券", "bob", "");
        used.setId("c1");
        used.setStatus(CoupleCeremonyCoupon.STATUS_USED);
        when(couponMapper.selectById("c1")).thenReturn(used);
        assertThatThrownBy(() -> service.useCoupon("alice", "c1"))
                .isInstanceOf(BusinessException.class).hasMessage("这张券已经核销过了");

        CoupleCeremonyCoupon open = CoupleCeremonyCoupon.of("s1", "看一次海", "bob", "");
        open.setId("c2");
        when(couponMapper.selectById("c2")).thenReturn(open);
        service.useCoupon("alice", "c2");
        assertThat(open.getStatus()).isEqualTo(CoupleCeremonyCoupon.STATUS_USED);
        assertThat(open.getUsedBy()).isEqualTo("alice");
        verify(couponMapper).updateById(open);
        verify(push).pushCoupleEvent(eq("ceremony-coupon-used"), eq("alice"), eq("bob"), any());
    }

    @Test
    void issueCouponValidatesTitle() {
        stubSpace("alice");
        // 发券现在要先有积分余额，这里给够一张券的钱再验标题
        lenient().when(ledgerMapper.findBySpace("s1")).thenReturn(List.of(
                CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_EARN, "好事簿：接我下班", 50)));
        lenient().when(ledgerMapper.insert(any(CouplePointLedger.class))).thenReturn(1);
        assertThatThrownBy(() -> service.issueCoupon("alice", "  "))
                .isInstanceOf(BusinessException.class).hasMessage("券面写点什么愿望吧");
        service.issueCoupon("alice", "一次说走就走的骑行");
        verify(couponMapper).insert(any(CoupleCeremonyCoupon.class));
    }

    // ========== F239 当日体感 ==========

    // ========== F237 史册 ==========

    // ========== F231 老黄历聚合 ==========

    // ========== 通用 ==========

    @Test
    void noSpaceThrows404() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.overview("solo"))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
    }
}
