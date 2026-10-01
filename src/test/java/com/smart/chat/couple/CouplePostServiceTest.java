package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * 明日邮局（F290-F299）单测：新年卡改写与到期放行、大事重名/全步 both 与放弃归属、
 * 拍卖自接禁止/认领排期/完成/逾期自动下架、梦想家年度版、退休档位与双写、
 * 井答双答 both、接龙在途限三与到点拆封、解梦一案一断与盖章归属、
 * 愿望当年不可盖与 owner 盖章、未来卡期限与兑现逾期结算、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CouplePostServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CouplePostOathMapper oathMapper;
    @Mock
    private CoupleBucketMapper bucketMapper;
    @Mock
    private CoupleBucketStepMapper stepMapper;
    @Mock
    private CoupleSomedayMapper somedayMapper;
    @Mock
    private CoupleDreamHomeMapper homeMapper;
    @Mock
    private CoupleRetirePlanMapper retireMapper;
    @Mock
    private CoupleWellQaMapper wellMapper;
    @Mock
    private CoupleRelayCapsuleMapper relayMapper;
    @Mock
    private CoupleDreamCaseMapper dreamMapper;
    @Mock
    private CoupleAnnivWishMapper wishMapper;
    @Mock
    private CoupleFutureCreditMapper creditMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CouplePostService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String YEAR = String.valueOf(LocalDate.now().getYear());
    private static final String LAST_YEAR = String.valueOf(LocalDate.now().getYear() - 1);

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

    // ========== F290 新年卡 ==========

    @Test
    void oathRewritableUntilDeliveredThenAutoRelease() {
        stubSpace();
        List<CouplePostOath> rows = new ArrayList<>();
        lenient().when(oathMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));
        lenient().when(oathMapper.findDue(eq("s1"), any())).thenAnswer(inv -> {
            String today = inv.getArgument(1);
            return rows.stream().filter(o -> "SEALED".equals(o.getStatus())
                    && o.getDeliverDay().compareTo(today) <= 0).toList();
        });
        when(oathMapper.insert(any(CouplePostOath.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.oath("alice", "五年后我们要还在一张桌上吃火锅");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getDeliverDay()).startsWith(String.valueOf(LocalDate.now().getYear() + 5));
        service.oath("alice", "改写：加一条还在同一张床上打呼");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getContent()).contains("打呼");

        rows.get(0).setDeliverDay(DAY);
        service.post("bob");
        assertThat(rows.get(0).getStatus()).isEqualTo("SENT");
        verify(push).pushCoupleEventBoth(eq("post-oath-opened"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.oath("alice", "再改"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("寄出去了");
    }

    // ========== F291 人生大事 ==========

    @Test
    void bucketGuardsProgressAndOwnerOnlyAbandon() {
        stubSpace();
        List<CoupleBucket> buckets = new ArrayList<>();
        List<CoupleBucketStep> steps = new ArrayList<>();
        lenient().when(bucketMapper.findOpen("s1")).thenAnswer(inv -> buckets.stream()
                .filter(b -> CoupleBucket.STATUS_OPEN.equals(b.getStatus())).toList());
        lenient().when(bucketMapper.findName("s1", "买房")).thenAnswer(inv -> buckets.stream()
                .filter(b -> b.getName().equals("买房")).findFirst().orElse(null));
        lenient().when(bucketMapper.selectById(any())).thenAnswer(inv -> buckets.stream()
                .filter(b -> b.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        when(bucketMapper.insert(any(CoupleBucket.class))).thenAnswer(inv -> {
            buckets.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(stepMapper.findByBucket(any())).thenAnswer(inv -> steps.stream()
                .filter(s -> s.getBucketId().equals(inv.getArgument(0))).toList());
        when(stepMapper.insert(any(CoupleBucketStep.class))).thenAnswer(inv -> {
            steps.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(stepMapper.selectById(any())).thenAnswer(inv -> steps.stream()
                .filter(s -> s.getId().equals(inv.getArgument(0))).findFirst().orElse(null));

        service.bucketAdd("alice", "买房", "", "");
        assertThatThrownBy(() -> service.bucketAdd("bob", "买房", "", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经在册");
        service.bucketStepAdd("alice", buckets.get(0).getId(), "选定城市");
        service.bucketStepAdd("alice", buckets.get(0).getId(), "凑首付");
        service.bucketStepDone("alice", steps.get(0).getId());
        verify(push).pushCoupleEvent(eq("post-step-done"), eq("alice"), eq("bob"), any());
        service.bucketStepDone("bob", steps.get(1).getId());
        verify(push).pushCoupleEventBoth(eq("post-bucket-done"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(buckets.get(0).getStatus()).isEqualTo(CoupleBucket.STATUS_DONE);
        assertThatThrownBy(() -> service.bucketAbandon("bob", buckets.get(0).getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("资格鸽");
    }

    // ========== F292 改天拍卖 ==========

    @Test
    void somedayFlowAndStaleShelfExpiry() {
        stubSpace();
        List<CoupleSomeday> rows = new ArrayList<>();
        lenient().when(somedayMapper.findAll("s1")).thenAnswer(inv -> rows.stream()
                .toList());
        lenient().when(somedayMapper.findStaleShelf(eq("s1"), any(Long.class))).thenAnswer(inv -> {
            long cutoff = inv.getArgument(1);
            return rows.stream().filter(s -> "SHELF".equals(s.getStatus()) && s.getCreated() < cutoff).toList();
        });
        lenient().when(somedayMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(s -> s.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        when(somedayMapper.insert(any(CoupleSomeday.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.somedayShelf("alice", "去一次内蒙看星星");
        CoupleSomeday item = rows.get(0);
        assertThatThrownBy(() -> service.somedayTake("alice", item.getId(), LocalDate.now().plusDays(3).toString()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不能自己接");
        assertThatThrownBy(() -> service.somedayTake("bob", item.getId(), LocalDate.now().minusDays(1).toString()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("之后");
        service.somedayTake("bob", item.getId(), LocalDate.now().plusDays(3).toString());
        verify(push).pushCoupleEventBoth(eq("post-taken"), eq("bob"), eq("alice"), eq("bob"), any());
        service.somedayDone("alice", item.getId());
        assertThatThrownBy(() -> service.somedayDone("alice", item.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没认领");
        verify(push).pushCoupleEventBoth(eq("post-someday-done"), eq("alice"), eq("alice"), eq("bob"), any());

        CoupleSomeday stale = CoupleSomeday.of("s1", "早没人要的旗", "alice");
        stale.setCreated(System.currentTimeMillis() - 8L * 24 * 3600 * 1000);
        rows.add(stale);
        service.post("alice");
        assertThat(stale.getStatus()).isEqualTo("EXPIRED");
        verify(push).pushCoupleEvent(eq("post-shelf-expired"), eq("alice"), eq("bob"), any());
    }

    // ========== F293/F294 梦想家与退休计划 ==========

    @Test
    void homeAndRetireDoubleWritePushes() {
        stubSpace();
        List<CoupleDreamHome> homes = new ArrayList<>();
        lenient().when(homeMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(homes));
        lenient().when(homeMapper.find("s1", YEAR, "alice")).thenAnswer(inv -> homes.stream()
                .filter(h -> h.getFromUser().equals("alice")).findFirst().orElse(null));
        lenient().when(homeMapper.find("s1", YEAR, "bob")).thenReturn(null);
        when(homeMapper.insert(any(CoupleDreamHome.class))).thenAnswer(inv -> {
            homes.add(inv.getArgument(0));
            return 1;
        });
        service.dreamHome("alice", null, "一间书房一间衣帽间", "窗外有海", "木质香", "共用的大书桌");
        assertThat(homes).hasSize(1);
        service.dreamHome("alice", null, "改成两间书房", "", "", "");
        assertThat(homes).hasSize(1);
        assertThat(homes.get(0).getRooms()).contains("两间");

        List<CoupleRetirePlan> plans = new ArrayList<>();
        lenient().when(retireMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> plans.stream()
                .filter(p -> p.getAgeBand().equals(inv.getArgument(1)) && p.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(retireMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(plans));
        when(retireMapper.insert(any(CoupleRetirePlan.class))).thenAnswer(inv -> {
            plans.add(inv.getArgument(0));
            return 1;
        });
        assertThatThrownBy(() -> service.retire("alice", "60", "x"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("30 / 40 / 50");
        service.retire("alice", "40", "开一家只在周末营业的小店");
        verify(push).pushCoupleEvent(eq("post-retire"), eq("alice"), eq("bob"), any());
        service.retire("bob", "40", "同样的店，他掌勺我记账");
        verify(push).pushCoupleEventBoth(eq("post-retire-both"), eq("bob"), eq("alice"), eq("bob"), any());
    }

    // ========== F295 许愿井 ==========

    @Test
    void wellBothAnswerPushesOnce() {
        stubSpace();
        List<CoupleWellQa> rows = new ArrayList<>();
        String week = LocalDate.now().with(DayOfWeek.MONDAY).toString();
        lenient().when(wellMapper.find(eq("s1"), eq(week), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getFromUser().equals(inv.getArgument(2))).findFirst().orElse(null));
        lenient().when(wellMapper.findYear(eq("s1"), any(), any())).thenAnswer(inv -> List.copyOf(rows));
        when(wellMapper.insert(any(CoupleWellQa.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.wellAnswer("alice", "先把露营装备配齐");
        verify(push).pushCoupleEvent(eq("post-well"), eq("alice"), eq("bob"), any());
        service.wellAnswer("bob", "好，帐篷你背");
        verify(push).pushCoupleEventBoth(eq("post-well-both"), eq("bob"), eq("alice"), eq("bob"), any());
        service.wellAnswer("bob", "改成：都行");
        verify(push, times(1)).pushCoupleEventBoth(eq("post-well-both"), any(), any(), any(), any());
        CouplePostService.PostVO vo = service.post("alice");
        assertThat(vo.well().bothIn()).isTrue();
        assertThat(vo.well().myAnswer()).contains("露营");
        assertThat(vo.wellYear()).hasSize(1);
    }

    // ========== F296 胶囊接龙 ==========

    @Test
    void relayInflightCapAndOpenGuards() {
        stubSpace();
        List<CoupleRelayCapsule> rows = new ArrayList<>();
        lenient().when(relayMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));
        lenient().when(relayMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        when(relayMapper.insert(any(CoupleRelayCapsule.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        assertThatThrownBy(() -> service.relaySeal("alice", "给明年的你", 5))
                .isInstanceOf(BusinessException.class).hasMessageContaining("1 / 2 / 3 年");
        service.relaySeal("alice", "给明年的你", 1);
        service.relaySeal("bob", "给明年的你", 1);
        CoupleRelayCapsule toOpen = rows.get(1);
        assertThatThrownBy(() -> service.relayOpen("alice", toOpen.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没到开启日");
        assertThatThrownBy(() -> service.relayOpen("alice", rows.get(0).getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不归你拆");
        toOpen.setOpenDay(DAY);
        rows.get(0).setOpenDay(LocalDate.now().plusYears(1).toString());
        service.relayOpen("alice", toOpen.getId());
        assertThat(toOpen.getStatus()).isEqualTo("OPENED");
        verify(push).pushCoupleEvent(eq("post-relay-opened"), eq("alice"), eq("bob"), any());
    }

    // ========== F297 解梦局 ==========

    @Test
    void dreamCaseFlowOwnership() {
        stubSpace();
        List<CoupleDreamCase> rows = new ArrayList<>();
        lenient().when(dreamMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));
        lenient().when(dreamMapper.find("s1", DAY, "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getDay().equals(DAY) && r.getDreamerUser().equals("alice")).findFirst().orElse(null));
        lenient().when(dreamMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        when(dreamMapper.insert(any(CoupleDreamCase.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.dreamAdd("alice", "梦见我们的猫会做饭");
        assertThatThrownBy(() -> service.dreamAdd("alice", "又做了一个"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("投过了");
        CoupleDreamCase c = rows.get(0);
        assertThatThrownBy(() -> service.dreamJudge("alice", c.getId(), true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没有解梦");
        assertThatThrownBy(() -> service.dreamRead("alice", c.getId(), "我自己解"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("那叫日记");
        service.dreamRead("bob", c.getId(), "猫掌勺，说明你近期有人投喂，财运即口欲");
        assertThatThrownBy(() -> service.dreamRead("bob", c.getId(), "再解一次"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已结");
        assertThatThrownBy(() -> service.dreamJudge("bob", c.getId(), true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只能我盖");
        service.dreamJudge("alice", c.getId(), true);
        assertThat(c.getGood()).isEqualTo(1);
    }

    // ========== F298 周年愿望 ==========

    @Test
    void wishVerdictYearAndOwnershipGuards() {
        stubSpace();
        List<CoupleAnnivWish> rows = new ArrayList<>();
        lenient().when(wishMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));
        lenient().when(wishMapper.find("s1", LAST_YEAR, "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getYear().equals(LAST_YEAR)).findFirst().orElse(null));
        lenient().when(wishMapper.find("s1", YEAR, "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getYear().equals(YEAR)).findFirst().orElse(null));
        lenient().when(wishMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        when(wishMapper.insert(any(CoupleAnnivWish.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.wish("alice", LAST_YEAR, "去年：学会做红烧肉");
        CoupleAnnivWish last = rows.get(0);
        last.setYear(LAST_YEAR);
        assertThatThrownBy(() -> service.wishVerdict("bob", last.getId(), true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("TA 自己盖章");
        service.wishVerdict("alice", last.getId(), false);
        assertThat(last.getVerdict()).isEqualTo("PIGEON");
        assertThatThrownBy(() -> service.wish("alice", LAST_YEAR, "还想改愿望"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("盖过章");
        service.wish("alice", YEAR, "今年：把红烧肉圆上");
        CoupleAnnivWish thisYear = rows.get(1);
        assertThatThrownBy(() -> service.wishVerdict("alice", thisYear.getId(), true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没到期");
    }

    // ========== F299 未来信用卡 ==========

    @Test
    void creditPromiseKeepAndOverdueSettle() {
        stubSpace();
        List<CoupleFutureCredit> rows = new ArrayList<>();
        lenient().when(creditMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));
        lenient().when(creditMapper.findOpen("s1")).thenAnswer(inv -> rows.stream()
                .filter(c -> "OPEN".equals(c.getStatus())).toList());
        lenient().when(creditMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(c -> c.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        when(creditMapper.insert(any(CoupleFutureCredit.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        assertThatThrownBy(() -> service.creditPromise("alice", "这周洗碗", DAY))
                .isInstanceOf(BusinessException.class).hasMessageContaining("未来");
        service.creditPromise("alice", "这周的碗我包了", LocalDate.now().plusDays(5).toString());
        CoupleFutureCredit p = rows.get(0);
        assertThatThrownBy(() -> service.creditKeep("bob", p.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("谁立");
        service.creditKeep("alice", p.getId());
        assertThat(p.getStatus()).isEqualTo("KEPT");
        verify(push).pushCoupleEventBoth(eq("post-credit-kept"), eq("alice"), eq("alice"), eq("bob"), any());

        CoupleFutureCredit doomed = CoupleFutureCredit.of("s1", "明天的碗", LocalDate.now().minusDays(1).toString(), "bob");
        rows.add(doomed);
        service.post("alice");
        assertThat(doomed.getStatus()).isEqualTo("BROKEN");
        verify(push).pushCoupleEventBoth(eq("post-credit-break"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(CouplePostBank.creditTier(12)).contains("VIP");
        assertThat(CouplePostBank.creditTier(0)).contains("口头");
    }

    // ========== 兜底 ==========

    @Test
    void noSpaceLeadsTo404() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.post("solo"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没有建立情侣空间");
    }
}
