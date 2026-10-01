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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 体温同步系（F220-F229）核心逻辑单测：熄灯幂等与 7 连击里程碑、睡眠星钳制与改单不推、
 * 数羊超时重置与双完推 both、喝水 3h 轻提醒判定、冷暖叮嘱守卫、陪伴卡一天一张、
 * 慢生活双提双打卡、对策本必填、抱抱里程碑跨越、月度小结聚合、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleCozyServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleCozyLightoutMapper lightoutMapper;
    @Mock
    private CoupleCozySleepMapper sleepMapper;
    @Mock
    private CoupleCozySheepMapper sheepMapper;
    @Mock
    private CoupleCozyWaterMapper waterMapper;
    @Mock
    private CoupleCozyWeatherMapper weatherMapper;
    @Mock
    private CoupleCozyLatenightMapper latenightMapper;
    @Mock
    private CoupleCozySlowMapper slowMapper;
    @Mock
    private CoupleCozyRemedyMapper remedyMapper;
    @Mock
    private CoupleCozyHugMapper hugMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleCozyService service;

    private static final String DAY = LocalDate.now().toString();

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

    // ========== F220 熄灯 ==========

    @Test
    void lightoutIsIdempotentAndNotifiesPartnerOnce() {
        stubSpace("alice");
        java.util.List<CoupleCozyLightout> rows = new java.util.ArrayList<>();
        when(lightoutMapper.find("s1", DAY, "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> "alice".equals(r.getFromUser())).findFirst().orElse(null));
        when(lightoutMapper.insert(any(CoupleCozyLightout.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });

        service.lightout("alice", "23:41");
        verify(push).pushCoupleEvent(eq("cozy-lightout"), any(), any(), any());

        service.lightout("alice", "23:42");
        assertThat(rows).hasSize(1);
        verify(push, times(1)).pushCoupleEvent(eq("cozy-lightout"), any(), any(), any());
    }

    @Test
    void sevenNightStreakPushesMilestone() {
        stubSpace("alice");
        LocalDate start = LocalDate.now().minusDays(6);
        when(lightoutMapper.find("s1", DAY, "alice")).thenReturn(null);
        when(lightoutMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> {
            String d = inv.getArgument(1);
            LocalDate day = LocalDate.parse(d);
            if (!day.isBefore(start) && !day.isAfter(LocalDate.now())) {
                return List.of(CoupleCozyLightout.of("s1", d, "alice", ""),
                        CoupleCozyLightout.of("s1", d, "bob", ""));
            }
            return List.of();
        });
        service.lightout("alice", null);
        verify(push).pushCoupleEventBoth(eq("cozy-lightout-week"), any(), any(), any(), any());
    }

    // ========== F221 睡眠单 ==========

    @Test
    void sleepStarsClampedAndReReportDoesNotPush() {
        stubSpace("alice");
        CoupleCozySleep existing = CoupleCozySleep.of("s1", DAY, "alice", 3, "旧梦");
        when(sleepMapper.find("s1", DAY, "alice")).thenReturn(null, existing);

        service.reportSleep("alice", DAY, 9, "梦到火锅");
        verify(sleepMapper).insert(any(CoupleCozySleep.class));
        verify(push).pushCoupleEvent(eq("cozy-sleep"), any(), any(), any());

        service.reportSleep("alice", DAY, 0, "改一下");
        verify(sleepMapper).updateById(existing);
        assertThat(existing.getStars()).isEqualTo(1);
        verify(push, times(1)).pushCoupleEvent(eq("cozy-sleep"), any(), any(), any());
    }

    @Test
    void sleepRejectsBadDay() {
        stubSpace("alice");
        assertThatThrownBy(() -> service.reportSleep("alice", "昨晚", 5, ""))
                .isInstanceOf(BusinessException.class);
    }

    // ========== F222 数羊 ==========

    @Test
    void sheepTapResetsAfterWindow() {
        stubSpace("alice");
        CoupleCozySheep old = CoupleCozySheep.of("s1", DAY, "alice");
        old.setTaps(5);
        old.setCreated(System.currentTimeMillis() - 120_000);
        old.setUpdatedAt(System.currentTimeMillis() - 120_000);
        when(sheepMapper.find("s1", DAY, "alice")).thenReturn(old);

        service.sheepTap("alice");
        assertThat(old.getTaps()).isEqualTo(1);
        assertThat(old.getDone()).isEqualTo(0);
    }

    @Test
    void sheepBothDonePushesTogether() {
        stubSpace("alice");
        CoupleCozySheep mine = CoupleCozySheep.of("s1", DAY, "alice");
        mine.setTaps(9);
        CoupleCozySheep partner = CoupleCozySheep.of("s1", DAY, "bob");
        partner.setTaps(10);
        partner.setDone(1);
        partner.setElapsedMs(45_000);
        partner.setUpdatedAt(System.currentTimeMillis() - 1);
        when(sheepMapper.find("s1", DAY, "alice")).thenReturn(mine);
        when(sheepMapper.find("s1", DAY, "bob")).thenReturn(partner);

        service.sheepTap("alice");

        assertThat(mine.getDone()).isEqualTo(1);
        verify(push).pushCoupleEventBoth(eq("cozy-sheep-done"), any(), any(), any(), any());
    }

    // ========== F223 喝水 ==========

    @Test
    void waterNudgeAfterThreeHoursSilence() {
        stubSpace("alice");
        CoupleCozyWater partner = CoupleCozyWater.of("s1", DAY, "bob");
        partner.setCups(3);
        partner.setUpdatedAt(System.currentTimeMillis() - 4 * 60 * 60 * 1000);
        when(waterMapper.find("s1", DAY, "bob")).thenReturn(partner);
        when(waterMapper.find("s1", DAY, "alice")).thenReturn(null);

        var today = service.today("alice");
        assertThat(today.water().nudge()).isTrue();
        assertThat(today.water().partner()).isEqualTo(3);
    }

    @Test
    void waterIncrementInsertThenUpdate() {
        stubSpace("alice");
        when(waterMapper.find("s1", DAY, "alice")).thenReturn(null);
        service.water("alice");
        verify(waterMapper).insert(any(CoupleCozyWater.class));

        CoupleCozyWater row = CoupleCozyWater.of("s1", DAY, "alice");
        row.setCups(2);
        when(waterMapper.find("s1", DAY, "alice")).thenReturn(row);
        service.water("alice");
        assertThat(row.getCups()).isEqualTo(3);
    }

    // ========== F224 冷暖 ==========

    @Test
    void weatherRequiresCityAndPartnerReportBeforeAdvise() {
        stubSpace("alice");
        assertThatThrownBy(() -> service.weather("alice", " ", "冷", ""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("城市");
        assertThatThrownBy(() -> service.adviseWeather("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还没");
    }

    @Test
    void adviseSetsOnceAndIdempotent() {
        stubSpace("alice");
        CoupleCozyWeather partner = CoupleCozyWeather.of("s1", DAY, "bob", "北京", "冷", "-3℃");
        when(weatherMapper.find("s1", DAY, "bob")).thenReturn(partner);

        service.adviseWeather("alice");
        assertThat(partner.getAdvisedBy()).isEqualTo("alice");
        verify(push).pushCoupleEvent(eq("cozy-advise"), any(), any(), any());

        service.adviseWeather("alice");
        verify(push, times(1)).pushCoupleEvent(eq("cozy-advise"), any(), any(), any());
    }

    // ========== F225 熬夜守护 ==========

    @Test
    void latenightCardOncePerDay() {
        stubSpace("alice");
        java.util.List<CoupleCozyLatenight> rows = new java.util.ArrayList<>();
        when(latenightMapper.find("s1", DAY, "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> "alice".equals(r.getFromUser())).findFirst().orElse(null));
        when(latenightMapper.insert(any(CoupleCozyLatenight.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });

        service.latenight("alice");
        verify(push).pushCoupleEvent(eq("cozy-latenight"), any(), any(), any());

        service.latenight("alice");
        assertThat(rows).hasSize(1);
        verify(push, times(1)).pushCoupleEvent(eq("cozy-latenight"), any(), any(), any());
    }

    // ========== F226 慢生活 ==========

    @Test
    void slowCheckRequiresThingFirst() {
        stubSpace("alice");
        String week = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString();
        when(slowMapper.find("s1", week, "alice")).thenReturn(null);
        assertThatThrownBy(() -> service.slowCheck("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("先提一件");
    }

    @Test
    void slowBothCheckedPushesRewind() {
        stubSpace("alice");
        String week = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString();
        CoupleCozySlow mine = CoupleCozySlow.of("s1", week, "alice", "晒一下午太阳");
        CoupleCozySlow partner = CoupleCozySlow.of("s1", week, "bob", "发呆");
        partner.setDoneDay(DAY);
        when(slowMapper.find("s1", week, "alice")).thenReturn(mine);
        when(slowMapper.findByWeek("s1", week)).thenReturn(List.of(mine, partner));

        service.slowCheck("alice");

        verify(push).pushCoupleEventBoth(eq("cozy-slow-done"), any(), any(), any(), any());
    }

    // ========== F227 疼痛对策 ==========

    @Test
    void remedyBodyRequiredAndComfortNeedsPartnerBook() {
        stubSpace("alice");
        assertThatThrownBy(() -> service.saveRemedy("alice", "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("正确做法");
        when(remedyMapper.find("s1", "bob")).thenReturn(null);
        assertThatThrownBy(() -> service.comfort("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("对策本");

        CoupleCozyRemedy book = CoupleCozyRemedy.of("s1", "bob", "热水袋+粥+别说话");
        when(remedyMapper.find("s1", "bob")).thenReturn(book);
        service.comfort("alice");
        verify(push).pushCoupleEvent(eq("cozy-comfort"), any(), any(), any());
    }

    // ========== F228 抱抱 ==========

    @Test
    void hugMilestoneCrossingPushesOnce() {
        stubSpace("alice");
        List<CoupleCozyHug> before = List.of(CoupleCozyHug.of("s1", DAY, "alice", 9, ""));
        when(hugMapper.findBySpace("s1")).thenReturn(before,
                List.of(before.get(0), CoupleCozyHug.of("s1", DAY, "alice", 2, "")));

        service.hug("alice", 2, "在地铁站");

        verify(hugMapper).insert(any(CoupleCozyHug.class));
        verify(push).pushCoupleEventBoth(eq("cozy-hug-milestone"), any(), any(), any(), any());
    }

    // ========== F229 月度小结 ==========

    @Test
    void monthlyAggregatesAndClampsIndex() {
        stubSpace("alice");
        String month = LocalDate.now().toString().substring(0, 7);
        when(lightoutMapper.findRange("s1", month + "-01", month + "-31"))
                .thenReturn(List.of(CoupleCozyLightout.of("s1", DAY, "alice", ""),
                        CoupleCozyLightout.of("s1", DAY, "bob", "")));
        when(lightoutMapper.findByDay("s1", DAY)).thenReturn(List.of(
                CoupleCozyLightout.of("s1", DAY, "alice", ""), CoupleCozyLightout.of("s1", DAY, "bob", "")));
        when(sleepMapper.findRange("s1", month + "-01", month + "-31"))
                .thenReturn(List.of(CoupleCozySleep.of("s1", DAY, "alice", 4, "")));
        when(sheepMapper.findRange("s1", month + "-01", month + "-31")).thenReturn(List.of());
        when(waterMapper.findRange("s1", month + "-01", month + "-31"))
                .thenReturn(List.of(CoupleCozyWater.of("s1", DAY, "alice")));

        CoupleCozyService.MonthlyVO vo = service.monthly("alice", null);

        assertThat(vo.month()).isEqualTo(month);
        assertThat(vo.bothLitNights()).isEqualTo(1);
        assertThat(vo.bestStreak()).isEqualTo(1);
        assertThat(vo.sleepReports()).isEqualTo(1);
        assertThat(vo.avgStars()).isEqualTo(4.0);
        assertThat(vo.index()).isEqualTo(2 + 2 + 0 + 0);
    }

    @Test
    void monthlyRejectsBadMonth() {
        stubSpace("alice");
        assertThatThrownBy(() -> service.monthly("alice", "20261"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("月份");
    }

    // ========== 无空间 ==========

    @Test
    void noSpaceThrows404() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.today("solo"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("情侣空间");
    }

    // ========== 今日总览结构 ==========

    @Test
    void todayAssemblesAllSections() {
        stubSpace("alice");
        var today = service.today("alice");
        assertThat(today.day()).isEqualTo(DAY);
        assertThat(today.sleeps()).isEmpty();
        assertThat(today.sheep().mineTaps()).isZero();
        assertThat(today.hug().total()).isZero();
        verify(hugMapper, atLeastOnce()).findBySpace("s1");
    }
}
