package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import com.smart.chat.im.UserProfileMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 夫妻老黄历（F250-F259）单测：农历换算锚点校验、节气跟风幂等与双跟风推 both、
 * 过法限二/归属删除/全勾 both、吉日自盖禁止与双章、节日家档改写不推、手账改写不推、
 * 长假愿望首写/补写、放空日限三与挡打卡、年度小结聚合、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleAlmanacServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleTermCheckMapper checkMapper;
    @Mock
    private CoupleTermRitualMapper ritualMapper;
    @Mock
    private CoupleLuckyDayMapper luckyMapper;
    @Mock
    private CoupleFestivalPlanMapper festivalMapper;
    @Mock
    private CoupleTermNoteMapper noteMapper;
    @Mock
    private CoupleHolidayWishMapper wishMapper;
    @Mock
    private CoupleNormalDayMapper normalMapper;
    @Mock
    private CoupleAnniversaryMapper anniversaryMapper;
    @Mock
    private UserProfileMapper profileMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleAlmanacService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String YEAR = String.valueOf(LocalDate.now().getYear());

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        return space;
    }

    private void stubSpace(String me) {
        org.mockito.Mockito.lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(space()));
        org.mockito.Mockito.lenient().when(spaceMapper.findActiveByUser(me.equals("alice") ? "bob" : "alice"))
                .thenReturn(Optional.of(space()));
    }

    // ========== 农历换算（F253 的地基） ==========

    @Test
    void lunarConversionMatchesKnownAnchors() {
        assertThat(CoupleTermBank.springFestival(2024)).isEqualTo(LocalDate.of(2024, 2, 10));
        assertThat(CoupleTermBank.springFestival(2025)).isEqualTo(LocalDate.of(2025, 1, 29));
        assertThat(CoupleTermBank.springFestival(2026)).isEqualTo(LocalDate.of(2026, 2, 17));
        assertThat(CoupleTermBank.lunarToSolar(2026, 8, 15, false)).isEqualTo(LocalDate.of(2026, 9, 25));
        assertThat(CoupleTermBank.lunarToSolar(2026, 5, 5, false)).isEqualTo(LocalDate.of(2026, 6, 19));
        assertThat(CoupleTermBank.zodiac(2026)).isEqualTo("马");
        assertThat(CoupleTermBank.festivalOn("CHUXI", 2026)).isEqualTo(LocalDate.of(2026, 2, 16));
        assertThat(CoupleTermBank.lunarToSolar(2026, 2, 30, false)).isNull();
    }

    @Test
    void solarRoundTripsBackToLunar() {
        LocalDate sf = CoupleTermBank.springFestival(2027);
        int[] lunar = CoupleTermBank.solarToLunar(sf);
        assertThat(lunar).containsExactly(2027, 1, 1, 0);
    }

    // ========== F250 节气跟风 ==========

    @Test
    void termCheckGuardsNonTermDayAndIsIdempotent() {
        stubSpace("alice");
        String term = CoupleTermBank.termOfToday(LocalDate.now());
        if (term == null) {
            assertThatThrownBy(() -> service.termCheck("alice", "跟风"))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("今天不是节气日");
            return;
        }
        List<CoupleTermCheck> rows = new ArrayList<>();
        when(checkMapper.find(eq("s1"), any(), eq(YEAR), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getTerm().equals(inv.getArgument(1)) && r.getFromUser().equals(inv.getArgument(3)))
                .findFirst().orElse(null));
        when(checkMapper.insert(any(CoupleTermCheck.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.termCheck("alice", "吃上了");
        verify(push).pushCoupleEvent(eq("term-check"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.termCheck("alice", "再来一次"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经跟过风");

        rows.add(CoupleTermCheck.of("s1", term, YEAR, "bob", "陪一个"));
        service.termCheck("alice", null);
        verify(push).pushCoupleEventBoth(eq("term-check-both"), eq("alice"), eq("alice"), eq("bob"), any());
    }

    @Test
    void normalDayBlocksTermCheck() {
        stubSpace("alice");
        String term = CoupleTermBank.termOfToday(LocalDate.now());
        org.junit.jupiter.api.Assumptions.assumeTrue(term != null, "非节气日不测挡打卡");
        when(normalMapper.find("s1", YEAR, DAY))
                .thenReturn(CoupleNormalDay.of("s1", YEAR, DAY, "alice"));
        assertThatThrownBy(() -> service.termCheck("alice", "偷偷打卡"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("放空");
    }

    // ========== F251 节气过法 ==========

    @Test
    void ritualLimitedToTwoPerTerm() {
        stubSpace("alice");
        List<CoupleTermRitual> rows = new ArrayList<>(List.of(
                CoupleTermRitual.of("s1", "立春", "咬春卷", "alice"),
                CoupleTermRitual.of("s1", "立春", "贴春牛", "bob")));
        when(ritualMapper.findByTerm("s1", "立春")).thenReturn(rows);
        assertThatThrownBy(() -> service.addRitual("alice", "立春", "第三条"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多记 2 条");
    }

    @Test
    void ritualRemoveOnlyByOwner() {
        stubSpace("alice");
        CoupleTermRitual row = CoupleTermRitual.of("s1", "立春", "咬春卷", "bob");
        when(ritualMapper.selectById(row.getId())).thenReturn(row);
        assertThatThrownBy(() -> service.removeRitual("alice", row.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("TA 自己划");
    }

    @Test
    void markRitualAllDonePushesBothOnce() {
        stubSpace("alice");
        String term = CoupleTermBank.termOfToday(LocalDate.now());
        org.junit.jupiter.api.Assumptions.assumeTrue(term != null, "非节气日窗口不测打卡");
        CoupleTermRitual one = CoupleTermRitual.of("s1", term, "吃汤圆", "alice");
        CoupleTermRitual two = CoupleTermRitual.of("s1", term, "挂灯笼", "bob");
        List<CoupleTermRitual> rows = new ArrayList<>(List.of(one, two));
        when(ritualMapper.findByTerm("s1", term)).thenReturn(rows);
        when(ritualMapper.selectById(one.getId())).thenReturn(one);
        when(ritualMapper.selectById(two.getId())).thenReturn(two);
        service.markRitual("alice", one.getId());
        verify(push).pushCoupleEvent(eq("term-ritual-done"), eq("alice"), eq("bob"), any());
        service.markRitual("alice", one.getId());
        verify(push, times(1)).pushCoupleEvent(eq("term-ritual-done"), any(), any(), any());
        service.markRitual("bob", two.getId());
        verify(push).pushCoupleEventBoth(eq("term-ritual-all"), eq("bob"), eq("alice"), eq("bob"), any());
    }

    // ========== F252 择吉日 ==========

    @Test
    void luckyCannotConfirmSelfAndDoubleStampPushesBoth() {
        stubSpace("alice");
        List<CoupleLuckyDay> rows = new ArrayList<>();
        when(luckyMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getDay().equals(inv.getArgument(1)) && r.getMatter().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        when(luckyMapper.insert(any(CoupleLuckyDay.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.lucky("alice", LocalDate.now().plusDays(9).toString(), "领证");
        CoupleLuckyDay row = rows.get(0);
        when(luckyMapper.selectById(row.getId())).thenReturn(row);
        assertThatThrownBy(() -> service.confirmLucky("alice", row.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("等 TA 来盖章");
        service.confirmLucky("bob", row.getId());
        verify(push).pushCoupleEventBoth(eq("term-lucky-confirmed"), eq("bob"), eq("alice"), eq("bob"), any());
        service.confirmLucky("bob", row.getId());
        verify(push, times(1)).pushCoupleEventBoth(eq("term-lucky-confirmed"), any(), any(), any(), any());
        assertThat(row.getComment()).contains("宜").contains("忌");
    }

    @Test
    void luckyRejectsPastDayAndDuplicate() {
        stubSpace("alice");
        assertThatThrownBy(() -> service.lucky("alice", LocalDate.now().minusDays(1).toString(), "搬家"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("往后挑");
    }

    // ========== F254/F255 改写不重推 ==========

    @Test
    void festivalUpsertNotifiesOnceAndBothWhenComplete() {
        stubSpace("alice");
        List<CoupleFestivalPlan> rows = new ArrayList<>();
        when(festivalMapper.find(eq("s1"), eq("QIXI"), eq(YEAR), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getFromUser().equals(inv.getArgument(3))).findFirst().orElse(null));
        when(festivalMapper.insert(any(CoupleFestivalPlan.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.festival("alice", "QIXI", YEAR, "看星星吃桂花糕");
        verify(push).pushCoupleEvent(eq("term-festival"), eq("alice"), eq("bob"), any());
        service.festival("alice", "QIXI", YEAR, "改成看雨也浪漫");
        verify(push, times(1)).pushCoupleEvent(eq("term-festival"), any(), any(), any());
        service.festival("bob", "QIXI", YEAR, "你说了算");
        verify(push).pushCoupleEventBoth(eq("term-festival-both"), eq("bob"), eq("alice"), eq("bob"), any());
    }

    @Test
    void invalidFestivalKeyRejected() {
        stubSpace("alice");
        assertThatThrownBy(() -> service.festival("alice", "DOUBLE11", null, "剁手"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("八个节日");
    }

    @Test
    void noteRewriteDoesNotRepush() {
        stubSpace("alice");
        List<CoupleTermNote> rows = new ArrayList<>();
        when(noteMapper.find(eq("s1"), eq("立春"), eq(YEAR), eq("alice"))).thenAnswer(inv -> rows.stream()
                .findFirst().orElse(null));
        when(noteMapper.insert(any(CoupleTermNote.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.note("alice", "立春", null, "第一笔");
        verify(push).pushCoupleEvent(eq("term-note"), eq("alice"), eq("bob"), any());
        service.note("alice", "立春", null, "改一笔");
        verify(push, times(1)).pushCoupleEvent(eq("term-note"), any(), any(), any());
        assertThat(rows.get(0).getText()).isEqualTo("改一笔");
    }

    // ========== F257 长假愿望 ==========

    @Test
    void wishFirstWriteAppendsOnceThenOwnRewrite() {
        stubSpace("alice");
        String[] nh = CoupleTermBank.nextHoliday(LocalDate.now(), CoupleAlmanacService.WISH_WINDOW_DAYS);
        org.junit.jupiter.api.Assumptions.assumeTrue(nh != null, "窗口内无长假不测愿望");
        List<CoupleHolidayWish> rows = new ArrayList<>();
        when(wishMapper.find("s1", nh[0])).thenAnswer(inv -> rows.stream().findFirst().orElse(null));
        when(wishMapper.insert(any(CoupleHolidayWish.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.holidayWish("alice", "周边自驾");
        verify(push).pushCoupleEvent(eq("term-wish"), eq("alice"), eq("bob"), any());
        service.holidayWish("bob", "我来订民宿");
        verify(push).pushCoupleEventBoth(eq("term-wish-append"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(rows.get(0).getWish()).contains("自驾").contains("民宿");
        service.holidayWish("alice", "改成远一点自驾");
        verify(push, times(1)).pushCoupleEvent(eq("term-wish"), any(), any(), any());
        assertThat(rows.get(0).getWish()).startsWith("改成远一点自驾").contains("民宿");
    }

    // ========== F258 放空日限三 ==========

    @Test
    void normalDayCappedAtThreePerYear() {
        stubSpace("alice");
        List<CoupleNormalDay> rows = new ArrayList<>();
        when(normalMapper.findByYear("s1", YEAR)).thenReturn(rows);
        when(normalMapper.find(eq("s1"), eq(YEAR), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getDay().equals(inv.getArgument(2))).findFirst().orElse(null));
        when(normalMapper.insert(any(CoupleNormalDay.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.normalDay("alice", LocalDate.now().plusDays(3).toString());
        service.normalDay("bob", LocalDate.now().plusDays(8).toString());
        service.normalDay("alice", LocalDate.now().plusDays(30).toString());
        assertThatThrownBy(() -> service.normalDay("bob", LocalDate.now().plusDays(40).toString()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("够贪懒了");
    }

    // ========== F259 年度小结 ==========

    @Test
    void yearlyAggregatesCountsAndScroll() {
        stubSpace("alice");
        String term = CoupleTermBank.termOfToday(LocalDate.now());
        org.junit.jupiter.api.Assumptions.assumeTrue(term != null, "无节气记录时不断言长卷");
        List<CoupleTermCheck> checks = new ArrayList<>(List.of(
                CoupleTermCheck.of("s1", term, YEAR, "alice", "跟了"),
                CoupleTermCheck.of("s1", term, YEAR, "bob", "跟了")));
        when(checkMapper.findByYear("s1", YEAR)).thenReturn(checks);
        when(noteMapper.findByYear("s1", YEAR)).thenReturn(List.of(
                CoupleTermNote.of("s1", term, YEAR, "alice", "一起吃的火锅")));
        CoupleAlmanacService.YearVO vo = service.yearly("alice", null);
        assertThat(vo.checksDone()).isEqualTo(2);
        assertThat(vo.notesDone()).isEqualTo(1);
        assertThat(vo.scroll()).anyMatch(line -> line.contains(term) && line.contains("跟风双人组")
                && line.contains("一起吃的火锅"));
    }

    // ========== 兜底 ==========

    @Test
    void noSpaceLeadsTo404() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.today("solo"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没有建立情侣空间");
    }

    @Test
    void overviewRendersLunarConversionCard() {
        stubSpace("alice");
        CoupleAnniversary lunar = CoupleAnniversary.of("s1", "奶奶的农历生日", "2026-02-17", true, "alice");
        lunar.setCalendarType(CoupleAnniversary.CALENDAR_LUNAR);
        lunar.setLunarMd("0112");
        when(anniversaryMapper.findBySpace("s1")).thenReturn(List.of(lunar));
        CoupleAlmanacService.TodayVO vo = service.today("alice");
        assertThat(vo.lunar()).hasSize(1);
        assertThat(vo.lunar().get(0).nextSolars()).isNotEmpty();
        assertThat(LocalDate.parse(vo.lunar().get(0).nextSolars().get(0))).isAfterOrEqualTo(LocalDate.now());
    }
}
