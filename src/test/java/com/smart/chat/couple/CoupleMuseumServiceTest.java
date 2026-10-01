package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 时光博物馆系（F190-F199）核心逻辑单测：三幕完整性校验、展品日期格式、
 * bigram 切词与停用词过滤、隐藏成就自动解锁且只解锁一次、家规签字权限、
 * 免打扰时刻格式与跨零点窗口、问候引擎天数、去年今日跨年对照、年度目录空月占位。
 */
@ExtendWith(MockitoExtension.class)
class CoupleMuseumServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleDocSceneMapper sceneMapper;
    @Mock
    private CoupleExhibitMapper exhibitMapper;
    @Mock
    private CoupleHiddenAchievementMapper achievementMapper;
    @Mock
    private CoupleHouseRuleMapper ruleMapper;
    @Mock
    private CoupleDndSettingMapper dndMapper;
    @Mock
    private CoupleThanksNoteMapper thanksNoteMapper;
    @Mock
    private CoupleJournalMapper journalMapper;
    @Mock
    private CoupleHeartFlashMapper flashMapper;
    @Mock
    private CoupleWhatIfMapper whatIfMapper;
    @Mock
    private CoupleSecretSignalMapper signalMapper;
    @Mock
    private CoupleSyncTapMapper tapMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleMuseumService museumService;

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

    private static long tsOf(LocalDate day) {
        return day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    // ========== F190 纪录片分镜 ==========

    @Test
    void sceneRequiresAllThreeActs() {
        stubSpace("alice");
        assertThatThrownBy(() -> museumService.addScene("alice", "初见", "第一幕", "", "第三幕"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("三幕");

        when(sceneMapper.findBySpace("s1")).thenReturn(List.of());
        museumService.addScene("alice", "初见", "车站那一面", "同桌那三年", "如今的每个清晨");
        ArgumentCaptor<CoupleDocScene> captor = ArgumentCaptor.forClass(CoupleDocScene.class);
        verify(sceneMapper).insert(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("初见");
        verify(push).pushCoupleEvent(anyString(), anyString(), anyString(), anyString());
    }

    // ========== F191 博物馆展品 ==========

    @Test
    void exhibitValidatesNameAndDay() {
        stubSpace("alice");
        assertThatThrownBy(() -> museumService.addExhibit("alice", " ", "故事", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("馆内名称");
        assertThatThrownBy(() -> museumService.addExhibit("alice", "票根", "故事", "2024-13-40"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("日期");

        when(exhibitMapper.findBySpace("s1")).thenReturn(List.of());
        museumService.addExhibit("alice", "第一场电影票根", "爆米花很咸", null);
        ArgumentCaptor<CoupleExhibit> captor = ArgumentCaptor.forClass(CoupleExhibit.class);
        verify(exhibitMapper).insert(captor.capture());
        assertThat(captor.getValue().getStory()).isEqualTo("爆米花很咸");
        assertThat(captor.getValue().getObtainedDay()).isNull();
    }

    // ========== F194 恋爱高频词 ==========

    @Test
    void bigramsSplitOnCjkRunsOnly() {
        List<String> bg = CoupleMuseumService.bigrams("你好，世界abc天气");
        assertThat(bg).contains("你好", "世界", "天气");
        assertThat(bg).doesNotContain("好世");
    }

    @Test
    void topWordsKeepsFrequentAndDropsStopwords() {
        stubSpace("alice");
        when(thanksNoteMapper.findBySpace("s1")).thenReturn(List.of(
                CoupleThanksNote.of("s1", "alice", "谢谢你的火锅"),
                CoupleThanksNote.of("s1", "bob", "火锅真好吃"),
                CoupleThanksNote.of("s1", "alice", "一起吃火锅吧")));
        when(journalMapper.findBySpace("s1")).thenReturn(List.of());
        when(flashMapper.findBySpace("s1")).thenReturn(List.of());
        when(whatIfMapper.findBySpace("s1")).thenReturn(List.of());

        List<CoupleMuseumService.WordVO> words = museumService.topWords("alice");
        assertThat(words).anyMatch(w -> w.word().equals("火锅") && w.count() >= 2);
        assertThat(words).noneMatch(w -> w.word().equals("什么"));
    }

    // ========== F195 隐藏彩蛋成就 ==========

    @Test
    void achievementAutoUnlocksOnceAndPushesBoth() {
        stubSpace("alice");
        List<CoupleThanksNote> ten = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ten.add(CoupleThanksNote.of("s1", "alice", "谢谢你" + i));
        }
        when(thanksNoteMapper.findBySpace("s1")).thenReturn(ten);
        when(journalMapper.findBySpace("s1")).thenReturn(List.of());
        when(flashMapper.findBySpace("s1")).thenReturn(List.of());
        when(whatIfMapper.findBySpace("s1")).thenReturn(List.of());
        when(signalMapper.findBySpace("s1")).thenReturn(List.of());
        when(tapMapper.findBySpace("s1")).thenReturn(List.of());
        CoupleHiddenAchievement unlocked = CoupleHiddenAchievement.of("s1", "THANKS_10", "alice");
        when(achievementMapper.findBySpace("s1")).thenReturn(List.of(), List.of(unlocked));

        List<CoupleMuseumService.AchievementVO> list = museumService.achievements("alice");
        assertThat(list).anyMatch(a -> a.code().equals("THANKS_10") && a.unlocked());
        verify(achievementMapper).insert(any(CoupleHiddenAchievement.class));
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());

        // 已解锁后不再重复插入
        stubSpace("bob");
        museumService.achievements("bob");
        verify(achievementMapper, times(1)).insert(any(CoupleHiddenAchievement.class));
    }

    // ========== F196 家规宪法 ==========

    @Test
    void ruleSignPermissionAndIdempotent() {
        stubSpace("bob");
        CoupleHouseRule row = CoupleHouseRule.of("s1", CoupleHouseRule.KIND_RULE, null, "睡前不吵架", "alice");
        when(ruleMapper.selectById(row.getId())).thenReturn(row);
        when(ruleMapper.findBySpace("s1")).thenReturn(List.of(row));

        stubSpace("alice");
        assertThatThrownBy(() -> museumService.signRule("alice", row.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("对方签字");

        stubSpace("bob");
        museumService.signRule("bob", row.getId());
        assertThat(row.getSigned()).isEqualTo(1);
        assertThat(row.getSignedBy()).isEqualTo("bob");
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());

        museumService.signRule("bob", row.getId());
        verify(push, times(1)).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void ruleEmptyContentRejected() {
        stubSpace("alice");
        assertThatThrownBy(() -> museumService.addRule("alice", "RULE", null, " "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("家规要写清楚");
    }

    // ========== F197 免打扰 ==========

    @Test
    void dndValidatesFormatAndEquality() {
        stubSpace("alice");
        assertThatThrownBy(() -> museumService.saveDnd("alice", "23:00", "7am", true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("结束时间");
        assertThatThrownBy(() -> museumService.saveDnd("alice", "23:00", "23:00", true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("全天静音");
    }

    @Test
    void dndWindowCrossesMidnight() {
        CoupleDndSetting row = CoupleDndSetting.of("s1", "alice", "23:00", "07:00", 1);
        assertThat(row.covers("02:00")).isTrue();
        assertThat(row.covers("23:30")).isTrue();
        assertThat(row.covers("08:00")).isFalse();
        row.setEnabled(0);
        assertThat(row.covers("02:00")).isFalse();
    }

    // ========== F198 首页问候引擎 ==========

    @Test
    void greetingCountsDaysTogetherAndQuietNow() {
        stubSpace("alice");
        CoupleSpace space = space();
        space.setCreated(tsOf(LocalDate.now().minusDays(100)));
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        when(dndMapper.find("s1", "alice")).thenReturn(CoupleDndSetting.of("s1", "alice", "00:00", "23:59", 1));

        CoupleMuseumService.GreetingVO vo = museumService.greeting("alice");
        assertThat(vo.daysTogether()).isEqualTo(100);
        assertThat(vo.text()).contains("100");
        assertThat(vo.quietNow()).isTrue();
    }

    // ========== F192 去年今日对比镜 ==========

    @Test
    void lastYearMirrorCountsByCalendarYear() {
        stubSpace("alice");
        int y = LocalDate.now().getYear();
        CoupleThanksNote oldNote = CoupleThanksNote.of("s1", "alice", "去年今天的感谢");
        oldNote.setCreated(tsOf(LocalDate.of(y - 1, 6, 15)));
        CoupleJournal freshJournal = CoupleJournal.of("s1", LocalDate.of(y, 6, 15).toString(), "alice", "🌸", "今年的手账");
        when(thanksNoteMapper.findBySpace("s1")).thenReturn(List.of(oldNote));
        when(journalMapper.findBySpace("s1")).thenReturn(List.of(freshJournal));
        when(flashMapper.findBySpace("s1")).thenReturn(List.of());

        CoupleMuseumService.LastYearMirrorVO vo = museumService.lastYearMirror("alice");
        assertThat(vo.lastYear().thanks()).isEqualTo(1);
        assertThat(vo.thisYear().journal()).isEqualTo(1);
        assertThat(vo.summary()).isNotBlank();
    }

    // ========== F199 年度记忆书目录 ==========

    @Test
    void annualBookHasTwelveChaptersWithEmptyPlaceholder() {
        stubSpace("alice");
        when(thanksNoteMapper.findBySpace("s1")).thenReturn(List.of());
        when(flashMapper.findBySpace("s1")).thenReturn(List.of());
        when(journalMapper.findBySpace("s1")).thenReturn(List.of(
                CoupleJournal.of("s1", YearMonth.now() + "-01", "alice", "🎬", "本月的手账")));

        CoupleMuseumService.AnnualBookVO book = museumService.annualBook("alice");
        assertThat(book.chapters()).hasSize(12);
        assertThat(book.chapters().get(0).line()).isEqualTo("空白页，等你们落笔");
        String currentMonth = YearMonth.now().toString();
        assertThat(book.chapters()).anyMatch(c -> c.month().equals(currentMonth) && c.line().contains("手账 1 页"));
    }

    // ========== 工具引用检查 ==========

    @Test
    void silverLineStablePerDay() {
        stubSpace("alice");
        CoupleMuseumService.SilverLineVO first = museumService.silverLine("alice");
        CoupleMuseumService.SilverLineVO second = museumService.silverLine("alice");
        assertThat(first.line()).isEqualTo(second.line()).isNotBlank();
        verify(achievementMapper, never()).insert(any(CoupleHiddenAchievement.class));
    }
}
