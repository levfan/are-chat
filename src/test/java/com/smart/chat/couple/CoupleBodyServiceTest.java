package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 身体通知系统（F310-F319）单测：体征超线只在首报推 TA、空报 400；呼噜档位与震感点评归属；
 * 周期标记与照顾卡只能给 TA；互助营开营查重/破戒幂等/安慰词只能对方说/结营归档；
 * 运动链 30 分钟窗口双报才算接上；不适 SOS 在途一条且自己接不住自己；忌口红线查重与饭桌撞标、
 * 谁登记谁才能划；体检陪同只能对方到、报告只能本人写；情绪药友按周 upsert、回话只能给对方；
 * 早睡军令状双签才生效且违约率读 F220 熄灯数据；无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleBodyServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleBodyMetricMapper metricMapper;
    @Mock
    private CoupleBodySnoreMapper snoreMapper;
    @Mock
    private CoupleBodyCycleMapper cycleMapper;
    @Mock
    private CoupleBodyQuitMapper quitMapper;
    @Mock
    private CoupleBodyFitMapper fitMapper;
    @Mock
    private CoupleBodySosMapper sosMapper;
    @Mock
    private CoupleBodyRedlineMapper redlineMapper;
    @Mock
    private CoupleBodyCheckupMapper checkupMapper;
    @Mock
    private CoupleBodyMedMapper medMapper;
    @Mock
    private CoupleBodyOathMapper oathMapper;
    @Mock
    private CoupleCozyLightoutMapper lightoutMapper;
    @Mock
    private CoupleDineTicketMapper dineMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleBodyService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String WEEK = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString();

    private final List<CoupleBodyMetric> metrics = new ArrayList<>();
    private final List<CoupleBodySnore> snores = new ArrayList<>();
    private final List<CoupleBodyCycle> cycles = new ArrayList<>();
    private final List<CoupleBodyQuit> camps = new ArrayList<>();
    private final List<CoupleBodyFit> fits = new ArrayList<>();
    private final List<CoupleBodySos> soss = new ArrayList<>();
    private final List<CoupleBodyRedline> redlines = new ArrayList<>();
    private final List<CoupleBodyCheckup> checkups = new ArrayList<>();
    private final List<CoupleBodyMed> meds = new ArrayList<>();
    private final List<CoupleBodyOath> oaths = new ArrayList<>();
    private final List<CoupleCozyLightout> lightouts = new ArrayList<>();
    private final List<CoupleDineTicket> tickets = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        lenient().when(metricMapper.findByDayUser(eq("s1"), any(), any())).thenAnswer(inv -> metrics.stream()
                .filter(m -> m.getDay().equals(inv.getArgument(1)) && m.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(metricMapper.findRecent(eq("s1"), any())).thenAnswer(inv -> List.copyOf(metrics));
        lenient().when(metricMapper.insert(any(CoupleBodyMetric.class))).thenAnswer(inv -> {
            metrics.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(snoreMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> snores.stream()
                .filter(s -> s.getDay().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(snoreMapper.insert(any(CoupleBodySnore.class))).thenAnswer(inv -> {
            snores.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(cycleMapper.findByDayUser(eq("s1"), any(), any())).thenAnswer(inv -> cycles.stream()
                .filter(c -> c.getDay().equals(inv.getArgument(1)) && c.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(cycleMapper.findRecent(eq("s1"), any())).thenAnswer(inv -> List.copyOf(cycles));
        lenient().when(cycleMapper.insert(any(CoupleBodyCycle.class))).thenAnswer(inv -> {
            cycles.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(quitMapper.findOwnerName(eq("s1"), any(), any())).thenAnswer(inv -> camps.stream()
                .filter(q -> q.getOwnerUser().equals(inv.getArgument(1)) && q.getName().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(quitMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(camps));
        lenient().when(quitMapper.findOpen("s1")).thenAnswer(inv -> camps.stream()
                .filter(q -> CoupleBodyQuit.STATUS_OPEN.equals(q.getStatus())).toList());
        lenient().when(quitMapper.insert(any(CoupleBodyQuit.class))).thenAnswer(inv -> {
            camps.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(fitMapper.findByDayKind(eq("s1"), any(), any())).thenAnswer(inv -> fits.stream()
                .filter(f -> f.getDay().equals(inv.getArgument(1)) && f.getKind().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(fitMapper.findRecent(eq("s1"), any())).thenAnswer(inv -> List.copyOf(fits));
        lenient().when(fitMapper.insert(any(CoupleBodyFit.class))).thenAnswer(inv -> {
            fits.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(sosMapper.findOpen("s1")).thenAnswer(inv -> soss.stream()
                .filter(s -> CoupleBodySos.STATUS_SENT.equals(s.getStatus())).toList());
        lenient().when(sosMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(soss));
        lenient().when(sosMapper.insert(any(CoupleBodySos.class))).thenAnswer(inv -> {
            soss.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(redlineMapper.findByItem(eq("s1"), any())).thenAnswer(inv -> redlines.stream()
                .filter(r -> r.getItem().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(redlineMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(redlines));
        lenient().when(redlineMapper.insert(any(CoupleBodyRedline.class))).thenAnswer(inv -> {
            redlines.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(redlineMapper.deleteById(any(java.io.Serializable.class))).thenAnswer(inv -> {
            redlines.removeIf(r -> r.getId().equals(inv.getArgument(0)));
            return 1;
        });

        lenient().when(checkupMapper.findByDayUser(eq("s1"), any(), any())).thenAnswer(inv -> checkups.stream()
                .filter(c -> c.getDay().equals(inv.getArgument(1)) && c.getOwnerUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(checkupMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(checkups));
        lenient().when(checkupMapper.insert(any(CoupleBodyCheckup.class))).thenAnswer(inv -> {
            checkups.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(medMapper.findByWeekUser(eq("s1"), any(), any())).thenAnswer(inv -> meds.stream()
                .filter(m -> m.getWeek().equals(inv.getArgument(1)) && m.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(medMapper.findRecent(eq("s1"), any())).thenAnswer(inv -> List.copyOf(meds));
        lenient().when(medMapper.insert(any(CoupleBodyMed.class))).thenAnswer(inv -> {
            meds.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(oathMapper.findByWeek(eq("s1"), any())).thenAnswer(inv -> oaths.stream()
                .filter(o -> o.getWeek().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(oathMapper.insert(any(CoupleBodyOath.class))).thenAnswer(inv -> {
            oaths.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(lightoutMapper.findRange(eq("s1"), any(), any())).thenAnswer(inv -> lightouts.stream()
                .filter(l -> l.getDay().compareTo(inv.getArgument(1)) >= 0
                        && l.getDay().compareTo(inv.getArgument(2)) <= 0).toList());

        lenient().when(dineMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> tickets.stream()
                .filter(t -> t.getDay().equals(inv.getArgument(1))).toList());
    }

    // ========== F310 体征互报 ==========

    @Test
    void metricAlertOnlyOnFirstReportAndBlankRejected() {
        service.metric("alice", "38.5", "", "", "37.3", "", "头疼");
        verify(push).pushCoupleEvent(eq("body-metric-alert"), eq("alice"), eq("bob"), any());
        assertThat(service.body("bob").metrics()).hasSize(1);
        assertThat(service.body("bob").metrics().get(0).warn()).isTrue();
        assertThat(service.body("bob").metrics().get(0).warnText()).contains("体温");

        service.metric("alice", "39.0", "", "", "37.3", "", "改：更难受了");
        verify(push, times(1)).pushCoupleEvent(eq("body-metric-alert"), eq("alice"), eq("bob"), any());
        assertThat(metrics).hasSize(1);

        assertThatThrownBy(() -> service.metric("bob", "", "", "", "", "", "只想说一句"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("至少报一项");
        service.metric("bob", "", "62.5", "4.5", "", "7", "熬夜了");
        CoupleBodyService.MetricVO sleepVo = service.body("alice").metrics().stream()
                .filter(m -> "62.5".equals(m.weight())).findFirst().orElseThrow();
        assertThat(sleepVo.warnText()).contains("只睡了 4.5");
        assertThat(sleepVo.mine()).isFalse();
    }

    // ========== F311 呼噜自报 ==========

    @Test
    void snoreLevelsAreClampedAndShakeGoesToOtherSide() {
        service.snore("alice", "MID");
        assertThat(snores.get(0).getLevelA()).isEqualTo("MID");
        verify(push).pushCoupleEvent(eq("body-snore"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.snore("bob", "LOUD"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("NONE/TINY/MID/HEAVY");

        service.snore("bob", "HEAVY");
        service.snoreShake("bob", "像有人在床头开拖拉机");
        CoupleBodyService.SnoreVO forAlice = service.body("alice").snore();
        assertThat(forAlice.partnerLevel()).isEqualTo("HEAVY");
        assertThat(forAlice.partnerShake()).contains("拖拉机");
        assertThat(forAlice.partnerScore()).isEqualTo(3);
        // 只能给报过的那方补震感：alice 自己没报也能给 bob 写点评（shakeA=B 给 A？此处 alice 写的是给 bob 的）
        service.snoreShake("alice", "我这边的答案是：习惯了");
        assertThat(service.body("bob").snore().partnerShake()).contains("习惯了");
    }

    // ========== F312 周期共览 ==========

    @Test
    void cycleMarkRewritableAndCareOnlyForPartner() {
        service.cycleMark("alice", DAY, "MENSTRUATING", "腰酸");
        verify(push).pushCoupleEvent(eq("body-cycle"), eq("alice"), eq("bob"), any());
        assertThat(service.body("bob").cycles().get(0).phaseLabel()).isEqualTo("进行中");
        assertThat(service.body("bob").cycles().get(0).iCanCare()).isTrue();

        service.cycleCare("bob", DAY, "热水袋 + 我抱着，30 分钟不打扰。");
        verify(push).pushCoupleEvent(eq("body-care"), eq("bob"), eq("alice"), any());
        assertThat(service.body("alice").cycles().get(0).careCard()).contains("热水袋");

        service.cycleMark("alice", DAY, "AFTER", "");
        verify(push, times(1)).pushCoupleEvent(eq("body-cycle"), eq("alice"), eq("bob"), any());
        assertThat(cycles).hasSize(1);

        assertThatThrownBy(() -> service.cycleMark("alice", DAY, "WHATEVER", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("阶段只有");
        assertThatThrownBy(() -> service.cycleCare("bob", LocalDate.now().minusDays(3).toString(), "空递"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("没标");
    }

    // ========== F313 互助营 ==========

    @Test
    void quitCampGuardsOwnerCheerAndBreakIdempotence() {
        service.quitStart("alice", "戒奶茶", 21, "");
        verify(push).pushCoupleEvent(eq("body-quit"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.quitStart("alice", "戒奶茶", 21, ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("开过了");
        assertThatThrownBy(() -> service.quitStart("alice", "戒可乐", 365, ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("营期天数");

        service.quitBroke("alice", camps.get(0).getId(), DAY);
        service.quitBroke("alice", camps.get(0).getId(), DAY);
        assertThat(camps.get(0).brokeCount()).isEqualTo(1);
        verify(push, times(1)).pushCoupleEvent(eq("body-quit-broke"), eq("alice"), eq("bob"), any());

        // 安慰词只能由陪绑的人说
        assertThatThrownBy(() -> service.quitCheer("alice", camps.get(0).getId(), "我很棒"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("陪绑的人");
        service.quitCheer("bob", camps.get(0).getId(), "破一次不算塌方");
        assertThat(service.body("alice").quits().get(0).cheerBy()).isEqualTo("bob");

        // 未满营期结营 = GONE
        service.quitClose("alice", camps.get(0).getId());
        assertThat(camps.get(0).getStatus()).isEqualTo(CoupleBodyQuit.STATUS_GONE);
        verify(push).pushCoupleEventBoth(eq("body-quit-close"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.quitClose("alice", camps.get(0).getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("结过了");
    }

    @Test
    void quitCampMilestoneOnSeventhDay() {
        camps.add(CoupleBodyQuit.of("s1", "戒糖", "alice", 21, LocalDate.now().minusDays(6).toString()));
        CoupleBodyService.QuitVO vo = service.body("alice").quits().get(0);
        assertThat(vo.campDays()).isEqualTo(7);
        assertThat(vo.milestone()).contains("营龄满 7 天");
    }

    // ========== F314 运动链 ==========

    @Test
    void fitChainLinksOnlyWithinThirtyMinutes() {
        service.fit("alice", "PUSHUP", 20);
        verify(push).pushCoupleEvent(eq("body-fit"), eq("alice"), eq("bob"), any());
        assertThat(service.body("bob").fits().get(0).linked()).isFalse();
        assertThat(service.body("bob").fits().get(0).kindLabel()).isEqualTo("俯卧撑");

        service.fit("bob", "squat", 30);
        assertThat(fits).hasSize(2);

        service.fit("bob", "PUSHUP", 15);
        verify(push).pushCoupleEventBoth(eq("body-fit-link"), eq("bob"), eq("alice"), eq("bob"), any());
        CoupleBodyService.FitVO linked = service.body("alice").fits().get(0);
        assertThat(linked.linked()).isTrue();
        assertThat(linked.myCount()).isEqualTo(20);
        assertThat(linked.partnerCount()).isEqualTo(15);

        // 已接上后再改数不再重推
        service.fit("bob", "PUSHUP", 18);
        verify(push, times(1)).pushCoupleEventBoth(eq("body-fit-link"), any(), any(), any(), any());

        assertThatThrownBy(() -> service.fit("bob", "SWIM", 1))
                .isInstanceOf(BusinessException.class).hasMessageContaining("PUSHUP/SQUAT/PLANK/RUN/STRETCH");
        assertThatThrownBy(() -> service.fit("bob", "RUN", -3))
                .isInstanceOf(BusinessException.class).hasMessageContaining("0-9999");
    }

    // ========== F315 不适 SOS ==========

    @Test
    void sosInflightOneAndOnlyPartnerCanHold() {
        service.sos("alice", "胃疼", "昨晚十点起");
        verify(push).pushCoupleEvent(eq("body-sos"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.sos("alice", "又疼了", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没被接住");
        assertThatThrownBy(() -> service.sosHold("alice", soss.get(0).getId(), "自己抱抱"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己接不住");

        CoupleBodyService.SosVO open = service.body("bob").soss().get(0);
        assertThat(open.options()).isNotEmpty();

        service.sosHold("bob", soss.get(0).getId(), "我去倒杯热水，坐在旁边不说话了。");
        assertThat(soss.get(0).getStatus()).isEqualTo(CoupleBodySos.STATUS_HELD);
        verify(push).pushCoupleEvent(eq("body-sos-held"), eq("bob"), eq("alice"), any());
        assertThatThrownBy(() -> service.sosHold("bob", soss.get(0).getId(), "再接一次"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("有人接过");

        service.sos("bob", "头疼", "");
        assertThatThrownBy(() -> service.sosHold("bob", "no-such", "x"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不存在");
    }

    // ========== F316 忌口红线本 ==========

    @Test
    void redlineDedupeHitDiningTicketsAndRemoverOwnership() {
        service.redlineAdd("alice", "香菜", "ALLERGY", "吃过会起疹子");
        verify(push).pushCoupleEventBoth(eq("body-redline"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.redlineAdd("bob", "香菜", "AVOID", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经在红线本上");
        assertThatThrownBy(() -> service.redlineAdd("bob", "辣", "SPICY", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("过敏 ALLERGY 和忌口 AVOID");

        tickets.add(CoupleDineTicket.of("s1", DAY, "bob", "香菜牛肉", "想吃"));
        tickets.add(CoupleDineTicket.of("s1", DAY, "alice", "番茄炒蛋", "清淡"));
        CoupleBodyService.RedlineHitVO hit = service.body("alice").redlineHits().get(0);
        assertThat(hit.hits()).isEqualTo(1);
        assertThat(hit.line()).contains("香菜").contains("先撤了它");

        assertThatThrownBy(() -> service.redlineRemove("bob", redlines.get(0).getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("谁登记的谁才能划掉");
        service.redlineRemove("alice", redlines.get(0).getId());
        assertThat(redlines).isEmpty();
    }

    // ========== F317 体检陪同 ==========

    @Test
    void checkupCompanyBelongsToPartnerAndReportToOwner() {
        String next = LocalDate.now().plusDays(3).toString();
        service.checkupPlan("alice", next, "血常规");
        verify(push).pushCoupleEvent(eq("body-checkup"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.checkupPlan("alice", next, "再约一次"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("约过一次");

        assertThatThrownBy(() -> service.checkupCompany("alice", checkups.get(0).getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己的到场不算陪同");
        service.checkupCompany("bob", checkups.get(0).getId());
        verify(push).pushCoupleEvent(eq("body-companion"), eq("bob"), eq("alice"), any());
        service.checkupCompany("bob", checkups.get(0).getId());
        verify(push, times(1)).pushCoupleEvent(eq("body-companion"), eq("bob"), eq("alice"), any());

        assertThatThrownBy(() -> service.checkupReport("bob", checkups.get(0).getId(), "一切正常"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("本人能写");
        service.checkupReport("alice", checkups.get(0).getId(), "各项正常，就等下次");
        assertThat(checkups.get(0).getStatus()).isEqualTo(CoupleBodyCheckup.STATUS_REPORTED);
        verify(push).pushCoupleEventBoth(eq("body-report"), eq("alice"), eq("alice"), eq("bob"), any());

        CoupleBodyService.CheckupVO vo = service.body("bob").checkups().get(0);
        assertThat(vo.daysLeft()).isEqualTo(3);
        assertThat(vo.companionLine()).isNotBlank();
    }

    // ========== F318 情绪药友 ==========

    @Test
    void medLogIsWeeklyUpsertAndReplyOnlyForPartner() {
        service.medLog("alice", "HARD", "这周总睡不着");
        verify(push).pushCoupleEvent(eq("body-med"), eq("alice"), eq("bob"), any());
        service.medLog("alice", "STEADY", "改：好一些了");
        verify(push, times(1)).pushCoupleEvent(eq("body-med"), eq("alice"), eq("bob"), any());
        assertThat(meds).hasSize(1);

        assertThatThrownBy(() -> service.medLog("bob", "MAYBE", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("STEADY/HARD/NONE");
        assertThatThrownBy(() -> service.medReply("alice", WEEK, "自己回自己"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("没记");

        service.medReply("bob", WEEK, "难受不用举例说明");
        verify(push).pushCoupleEvent(eq("body-med-reply"), eq("bob"), eq("alice"), any());
        CoupleBodyService.MedVO vo = service.body("alice").meds().get(0);
        assertThat(vo.reply()).contains("举例");
        assertThat(vo.mine()).isTrue();
    }

    // ========== F319 早睡军令状 ==========

    @Test
    void sleepOathNeedsBothSignsAndBreachComesFromLightout() {
        service.oathSign("alice", "23:30");
        verify(push).pushCoupleEvent(eq("body-oath"), eq("alice"), eq("bob"), any());
        assertThat(service.body("alice").oath().bothSigned()).isFalse();

        service.oathSign("bob", "23:00");
        verify(push).pushCoupleEventBoth(eq("body-oath-sign"), eq("bob"), eq("alice"), eq("bob"), any());

        lightouts.add(lightout("alice", DAY, "23:50"));
        lightouts.add(lightout("bob", DAY, "22:55"));
        CoupleBodyService.OathVO vo = service.body("alice").oath();
        assertThat(vo.bothSigned()).isTrue();
        assertThat(vo.myBreach()).isEqualTo(1);
        assertThat(vo.myNights()).isEqualTo(1);
        assertThat(vo.partnerBreach()).isEqualTo(0);
        assertThat(vo.line()).contains("违约 50%");

        // 改写自己的线不再重推
        service.oathSign("alice", "23:40");
        verify(push, times(1)).pushCoupleEventBoth(eq("body-oath-sign"), any(), any(), any(), any());
        verify(push, times(1)).pushCoupleEvent(eq("body-oath"), any(), any(), any());

        assertThatThrownBy(() -> service.oathSign("bob", "11点"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("HH:mm");
    }

    // ========== 空间校验 ==========

    @Test
    void requiresActiveSpace() {
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.body("alice"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("先邀请一位好友");
    }

    private static CoupleCozyLightout lightout(String user, String day, String atTime) {
        CoupleCozyLightout l = new CoupleCozyLightout();
        l.setId("l-" + user + day);
        l.setSpaceId("s1");
        l.setDay(day);
        l.setFromUser(user);
        l.setAtTime(atTime);
        l.setCreated(System.currentTimeMillis());
        return l;
    }
}
