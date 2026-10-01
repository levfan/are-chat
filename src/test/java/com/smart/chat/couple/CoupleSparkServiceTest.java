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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 默契亲密系（F170-F179）核心逻辑单测：爱语计分与重测覆盖、对照卡双结果门槛、
 * 「如果」双答互见与先答之星、同频按键窗口与命中推送、心动日历等级钳制、
 * 仪表盘与周报聚合、动作暗语。
 */
@ExtendWith(MockitoExtension.class)
class CoupleSparkServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleLoveLangMapper loveLangMapper;
    @Mock
    private CoupleHeartFlashMapper flashMapper;
    @Mock
    private CoupleWhatIfMapper whatIfMapper;
    @Mock
    private CoupleSecretSignalMapper signalMapper;
    @Mock
    private CoupleSyncTapMapper tapMapper;
    @Mock
    private CoupleHeartDayMapper heartDayMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleSparkService sparkService;

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

    // ========== F170 爱语测评 ==========

    @Test
    void loveLangScoringAndUpsert() {
        stubSpace("alice");
        List<String> answers = List.of("A", "A", "A", "A", "A", "A", "B", "B", "B", "B", "B", "B");
        // 手算：A 答案的 lang 分布 + B 的分布决定主爱语；断言 scores 合计 = 12
        when(loveLangMapper.find("s1", "alice")).thenReturn(null);

        CoupleSparkService.LoveLangVO vo = sparkService.submitLoveLang("alice", answers);
        int total = vo.scores().stream().mapToInt(Integer::intValue).sum();
        assertThat(total).isEqualTo(12);
        assertThat(CoupleSparkBank.LANGS.stream().map(CoupleSparkBank.LangMeta::code))
                .contains(vo.primaryLang());
        ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEvent(event.capture(), anyString(), anyString(), anyString());
        assertThat(event.getValue()).isEqualTo("love-lang-done");
    }

    @Test
    void loveLangRequiresFullAnswers() {
        stubSpace("alice");
        assertThatThrownBy(() -> sparkService.submitLoveLang("alice", List.of("A", "B")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("12 道题");
    }

    // ========== F171 爱语对照卡 ==========

    @Test
    void loveLangPairNeedsBothResults() {
        stubSpace("alice");
        when(loveLangMapper.find("s1", "alice")).thenReturn(CoupleLoveLang.of("s1", "alice", "WORDS"));
        when(loveLangMapper.find("s1", "bob")).thenReturn(null);
        assertThatThrownBy(() -> sparkService.loveLangPair("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("都完成测评");
    }

    // ========== F173 「如果」问答 ==========

    @Test
    void whatIfPartnerVisibleOnlyWhenBothAnswered() {
        String today = LocalDate.now().toString();
        stubSpace("alice");
        when(whatIfMapper.find("s1", today, "alice")).thenReturn(CoupleWhatIf.of("s1", today, "alice", "去海边"));
        when(whatIfMapper.find("s1", today, "bob")).thenReturn(null);

        CoupleSparkService.WhatIfVO before = sparkService.whatIf("alice");
        assertThat(before.question()).isNotBlank();
        assertThat(before.partner()).isNull();

        when(whatIfMapper.find("s1", today, "bob")).thenReturn(CoupleWhatIf.of("s1", today, "bob", "买书"));
        CoupleSparkService.WhatIfVO after = sparkService.whatIf("alice");
        assertThat(after.partner()).isNotNull();
        assertThat(after.bothAnswered()).isTrue();
        assertThat(after.firstStar()).isIn("alice", "bob");
    }

    // ========== F175 同频共振 ==========

    @Test
    void tapWindowPairsWithinTenSeconds() {
        stubSpace("alice");
        String today = LocalDate.now().toString();
        CoupleSyncTap row = CoupleSyncTap.of("s1", today);
        row.setLastTapUser("bob");
        row.setLastTapAt(System.currentTimeMillis() - 120); // 120ms 前按过
        when(tapMapper.find("s1", today)).thenReturn(row);

        CoupleSparkService.TapResultVO result = sparkService.tap("alice");
        assertThat(result.diffMs()).isNotNull();
        assertThat(result.hit()).isTrue(); // 120ms <= 500ms
        assertThat(row.getBestMs()).isNotNull();
        assertThat(row.getHits()).isEqualTo(1);
        ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEventBoth(event.capture(), anyString(), anyString(), anyString(), anyString());
        assertThat(event.getValue()).isEqualTo("sync-tap-hit");
    }

    @Test
    void tapWindowExpiredJustStoresPress() {
        stubSpace("alice");
        String today = LocalDate.now().toString();
        CoupleSyncTap row = CoupleSyncTap.of("s1", today);
        row.setLastTapUser("bob");
        row.setLastTapAt(System.currentTimeMillis() - 20_000); // 超窗
        when(tapMapper.find("s1", today)).thenReturn(row);

        CoupleSparkService.TapResultVO result = sparkService.tap("alice");
        assertThat(result.diffMs()).isNull();
        assertThat(row.getLastTapUser()).isEqualTo("alice");
        assertThat(row.getAttempts()).isZero();
        verify(push, never()).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void tapOwnRepeatDoesNotPair() {
        stubSpace("alice");
        String today = LocalDate.now().toString();
        CoupleSyncTap row = CoupleSyncTap.of("s1", today);
        row.setLastTapUser("alice");
        row.setLastTapAt(System.currentTimeMillis() - 100);
        when(tapMapper.find("s1", today)).thenReturn(row);

        CoupleSparkService.TapResultVO result = sparkService.tap("alice");
        assertThat(result.diffMs()).isNull();
        assertThat(row.getLastTapUser()).isEqualTo("alice");
    }

    // ========== F177 心动日历 ==========

    @Test
    void heartDayLevelClampedAndUpsert() {
        stubSpace("alice");
        String today = LocalDate.now().toString();
        when(heartDayMapper.find("s1", today, "alice")).thenReturn(null);
        when(heartDayMapper.findBySpace("s1")).thenReturn(List.of());

        sparkService.markHeartDay("alice", 9); // 钳到 3
        ArgumentCaptor<CoupleHeartDay> saved = ArgumentCaptor.forClass(CoupleHeartDay.class);
        verify(heartDayMapper).insert(saved.capture());
        assertThat(saved.getValue().getLevel()).isEqualTo(3);
    }

    // ========== F176 默契仪表盘 ==========

    @Test
    void dashboardAggregatesAllSignals() {
        stubSpace("alice");
        String today = LocalDate.now().toString();
        CoupleSyncTap tap = CoupleSyncTap.of("s1", today);
        tap.setBestMs(120L);
        when(tapMapper.findBySpace("s1")).thenReturn(List.of(tap));
        when(whatIfMapper.findBySpace("s1")).thenReturn(List.of(
                CoupleWhatIf.of("s1", today, "alice", "a"),
                CoupleWhatIf.of("s1", today, "bob", "b")));
        when(heartDayMapper.findBySpace("s1")).thenReturn(List.of(CoupleHeartDay.of("s1", today, "alice", 3)));
        when(signalMapper.findBySpace("s1")).thenReturn(List.of(CoupleSecretSignal.of("s1", "alice", "捏三下", "别怕")));

        CoupleSparkService.SparkDashboardVO dash = sparkService.dashboard("alice");
        assertThat(dash.bestMs()).isEqualTo(120L);
        assertThat(dash.whatIfBothDays()).isEqualTo(1);
        assertThat(dash.heartDays()).isEqualTo(1);
        assertThat(dash.signals()).isEqualTo(1);
        assertThat(dash.score()).isEqualTo(37); // 30(best<=250) + 3(双答1天) + 2(心动1) + 2(暗语1)
        assertThat(dash.label()).isEqualTo("培养中");
    }

    // ========== F178 同频排行榜 ==========

    @Test
    void syncRankSortedByBestAscAndSkipsNull() {
        stubSpace("alice");
        CoupleSyncTap slow = CoupleSyncTap.of("s1", "2026-01-01");
        slow.setBestMs(900L);
        CoupleSyncTap fast = CoupleSyncTap.of("s1", "2026-01-02");
        fast.setBestMs(80L);
        CoupleSyncTap none = CoupleSyncTap.of("s1", "2026-01-03");
        when(tapMapper.findBySpace("s1")).thenReturn(List.of(slow, none, fast));

        List<CoupleSparkService.SyncRankVO> rank = sparkService.syncRank("alice");
        assertThat(rank).hasSize(2);
        assertThat(rank.get(0).bestMs()).isEqualTo(80L);
        assertThat(rank.get(0).day()).isEqualTo("2026-01-02");
    }

    // ========== F179 默契周报 ==========

    @Test
    void weeklySummaryCountsThisWeekOnly() {
        stubSpace("alice");
        String monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString();
        List<CoupleWhatIf> answers = List.of(
                CoupleWhatIf.of("s1", monday, "alice", "a"),
                CoupleWhatIf.of("s1", monday, "bob", "b"),
                CoupleWhatIf.of("s1", "2020-01-01", "alice", "old"),
                CoupleWhatIf.of("s1", "2020-01-01", "bob", "old"));
        when(whatIfMapper.findBySpace("s1")).thenReturn(answers);
        when(heartDayMapper.findBySpace("s1")).thenReturn(List.of(CoupleHeartDay.of("s1", monday, "alice", 2)));
        CoupleSyncTap tap = CoupleSyncTap.of("s1", monday);
        tap.setAttempts(3);
        when(tapMapper.findBySpace("s1")).thenReturn(List.of(tap));

        CoupleSparkService.SparkWeeklyVO weekly = sparkService.weekly("alice");
        assertThat(weekly.whatIfBoth()).isEqualTo(1); // 2020 那天不计入本周
        assertThat(weekly.heartMarks()).isEqualTo(1);
        assertThat(weekly.syncAttempts()).isEqualTo(3);
        assertThat(weekly.summary()).contains("1 天").contains("3 次").contains("1 个");
    }

    // ========== F174 动作暗语 ==========

    @Test
    void addSignalRequiresBothFields() {
        stubSpace("alice");
        assertThatThrownBy(() -> sparkService.addSignal("alice", "捏三下手心", "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("都要写");
        verify(signalMapper, never()).insert(any(CoupleSecretSignal.class));

        when(signalMapper.findBySpace("s1")).thenReturn(List.of());
        sparkService.addSignal("alice", "捏三下手心", "别怕，我在");
        ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEvent(event.capture(), anyString(), anyString(), anyString());
        assertThat(event.getValue()).isEqualTo("signal-added");
    }
}
