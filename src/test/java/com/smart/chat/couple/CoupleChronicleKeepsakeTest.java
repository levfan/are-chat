package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import com.smart.chat.im.UserProfile;
import com.smart.chat.im.UserProfileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回忆资产批次单测：编年史按年聚合、考古卡优先旧记录、问答机真实数据出题、
 * 周年报告区间统计、生日回顾聚合、语录/票根/歌单收藏规则、胶囊到期提醒 job。
 */
@ExtendWith(MockitoExtension.class)
class CoupleChronicleKeepsakeTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;

    @Mock
    private CoupleFirstMapper firstMapper;

    @Mock
    private CoupleAnniversaryMapper anniversaryMapper;

    @Mock
    private CoupleCapsuleMapper capsuleMapper;

    @Mock
    private CouplePromiseMapper promiseMapper;

    @Mock
    private CoupleTravelWishMapper travelMapper;

    @Mock
    private CoupleTruthMapper truthMapper;


    @Mock
    private CoupleMoodMapper moodMapper;

    @Mock
    private CoupleQuoteMapper quoteMapper;

    @Mock
    private CoupleTicketMapper ticketMapper;

    @Mock
    private CoupleSongMapper songMapper;

    @Mock
    private UserProfileMapper profileMapper;

    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleChronicleService chronicleService;

    @InjectMocks
    private CoupleKeepsakeService keepsakeService;

    @InjectMocks
    private CoupleMemoryJob memoryJob;

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        space.setAnniversary("2024-10-01");
        space.setCreated(1700000000000L);
        return space;
    }

    private void stubSpace(String me) {
        lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(space()));
    }

    // ========== F80 恋爱编年史 ==========

    @Test
    void chronicleGroupsEventsByYearDescending() {
        stubSpace("alice");
        CoupleFirst first = CoupleFirst.of("s1", "第一次一起看海", "2025-02-14", "风很大", "alice");
        when(firstMapper.findBySpace("s1")).thenReturn(List.of(first));
        CouplePromise done = CouplePromise.of("s1", "bob", "alice", "陪你看日出", null);
        done.setStatus(CouplePromise.STATUS_DONE);
        done.setDoneAt(System.currentTimeMillis());
        when(promiseMapper.findBySpace("s1")).thenReturn(List.of(done));
        when(anniversaryMapper.findBySpace("s1")).thenReturn(List.of());
        when(capsuleMapper.findBySpace("s1")).thenReturn(List.of());
        when(travelMapper.findBySpace("s1")).thenReturn(List.of());
        when(truthMapper.findBySpace("s1")).thenReturn(List.of());

        List<CoupleChronicleService.ChronicleYearVO> years = chronicleService.chronicle("alice");

        assertThat(years).isNotEmpty();
        // 年份倒序
        for (int i = 1; i < years.size(); i++) {
            assertThat(years.get(i - 1).year()).isGreaterThanOrEqualTo(years.get(i).year());
        }
        // 第一次清单进入编年史
        assertThat(years.stream().flatMap(y -> y.events().stream()))
                .anyMatch(e -> "第一次一起看海".equals(e.title()) && "2025-02-14".equals(e.day()));
        // 兑现的约定也在
        assertThat(years.stream().flatMap(y -> y.events().stream()))
                .anyMatch(e -> "promise".equals(e.type()));
    }

    // ========== F81 考古卡 ==========

    @Test
    void archaeologyPrefersRecordsOlderThanThirtyDays() {
        stubSpace("alice");
        String oldDay = LocalDate.now().minusDays(31).toString();
        // 考古来源之一「恋爱存折」已随共同养成裁剪，改用语录册这条仍在职的来源验证同一行为
        CoupleQuote oldRow = CoupleQuote.of("s1", "bob", "陪 TA 散步", "下班路上");
        oldRow.setCreated(java.time.LocalDate.parse(oldDay)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        when(quoteMapper.findBySpace("s1")).thenReturn(List.of(oldRow));
        when(truthMapper.findBySpace("s1")).thenReturn(List.of());
        when(moodMapper.findBySpace("s1")).thenReturn(List.of());

        CoupleChronicleService.ArchaeologyCardVO card = chronicleService.archaeology("alice");

        assertThat(card.kind()).isEqualTo("quote");
        assertThat(card.daysAgo()).isEqualTo(31);
        assertThat(card.content()).isEqualTo("陪 TA 散步");
    }

    @Test
    void archaeologyThrowsWhenNoRecordsAtAll() {
        stubSpace("alice");
        when(truthMapper.findBySpace("s1")).thenReturn(List.of());
        when(quoteMapper.findBySpace("s1")).thenReturn(List.of());
        when(moodMapper.findBySpace("s1")).thenReturn(List.of());

        assertThatThrownBy(() -> chronicleService.archaeology("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("考古层");
    }

    // ========== F82 恋爱问答机 ==========

    @Test
    void quizBuildsThreeQuestionsWithRealData() {
        stubSpace("alice");
        when(firstMapper.findBySpace("s1")).thenReturn(List.of());

        List<CoupleChronicleService.QuizQuestionVO> questions = chronicleService.quiz("alice");

        assertThat(questions).hasSize(2); // 在一起日期 + 在一起天数
        long daysTogether = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse("2024-10-01"), LocalDate.now()) + 1;
        CoupleChronicleService.QuizQuestionVO daysQuestion = questions.get(1);
        assertThat(daysQuestion.options()).contains(String.valueOf(daysTogether));
        assertThat(daysQuestion.options().indexOf(String.valueOf(daysTogether))).isEqualTo(daysQuestion.answerIndex());
        // 选项各不相同
        assertThat(daysQuestion.options()).doesNotHaveDuplicates();
    }

    // ========== F85 周年报告 ==========

    @Test
    void anniversaryReportCountsSinceLastAnniversary() {
        stubSpace("alice");
        CouplePromise done = CouplePromise.of("s1", "bob", "alice", "陪你看日出", null);
        done.setStatus(CouplePromise.STATUS_DONE);
        done.setDoneAt(System.currentTimeMillis()); // 今天兑现 → 一定在周年区间内
        when(promiseMapper.findBySpace("s1")).thenReturn(List.of(done));
        when(moodMapper.findBySpace("s1")).thenReturn(List.of());
        when(truthMapper.findBySpace("s1")).thenReturn(List.of());
        when(firstMapper.findBySpace("s1")).thenReturn(List.of());
        when(travelMapper.findBySpace("s1")).thenReturn(List.of());

        CoupleChronicleService.AnniversaryReportVO report = chronicleService.anniversaryReport("alice");

        assertThat(report.anniversaryDay()).isEqualTo("2024-10-01");
        assertThat(report.nthYear()).isGreaterThanOrEqualTo(2);
        assertThat(report.items()).hasSize(5);
        assertThat(report.items().stream().filter(i -> "promises".equals(i.key())).findFirst().orElseThrow().value())
                .isEqualTo(1);
        // since 不晚于今天
        assertThat(LocalDate.parse(report.sinceDay())).isBeforeOrEqualTo(LocalDate.now());
    }

    // ========== F86 生日回顾 ==========

    @Test
    void birthdayLookRequiresPartnerBirthday() {
        stubSpace("alice");
        UserProfile empty = new UserProfile();
        when(profileMapper.selectById("bob")).thenReturn(empty);

        assertThatThrownBy(() -> chronicleService.birthdayLook("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("生日");
    }

    @Test
    void birthdayLookAggregatesEventsOnThatDay() {
        stubSpace("alice");
        UserProfile profile = new UserProfile();
        profile.setBirthday("1999-03-15");
        when(profileMapper.selectById("bob")).thenReturn(profile);
        CoupleFirst first = CoupleFirst.of("s1", "第一次一起过生日", "2025-03-15", "蛋糕是草莓的", "alice");
        when(firstMapper.findBySpace("s1")).thenReturn(List.of(first));
        when(truthMapper.findBySpace("s1")).thenReturn(List.of());
        when(moodMapper.findBySpace("s1")).thenReturn(List.of());

        CoupleChronicleService.BirthdayLookVO look = chronicleService.birthdayLook("alice");

        assertThat(look.birthday()).isEqualTo("1999-03-15");
        assertThat(look.events()).hasSize(1);
        assertThat(look.events().get(0).title()).isEqualTo("第一次一起过生日");
    }

    // ========== F83 甜蜜语录收藏册 ==========

    @Test
    void quoteSaveValidatesAndRemoveScopesToSpace() {
        stubSpace("alice");
        stubSpace("bob");
        assertThatThrownBy(() -> keepsakeService.saveQuote("alice", " ", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("语录");

        CoupleQuote row = CoupleQuote.of("s1", "alice", "别怕，有我在", "深夜");
        when(quoteMapper.selectById(row.getId())).thenReturn(row);
        when(quoteMapper.findBySpace("s1")).thenReturn(List.of());
        keepsakeService.saveQuote("alice", "别怕，有我在", "深夜");
        verify(quoteMapper).insert(any(CoupleQuote.class));
        verify(push).pushCoupleEvent(eq("quote-kept"), eq("alice"), eq("bob"), anyString());

        keepsakeService.removeQuote("bob", row.getId());
        verify(quoteMapper).deleteById(row.getId());

        CoupleQuote foreign = CoupleQuote.of("other", "mallory", "外星语录", null);
        when(quoteMapper.selectById(foreign.getId())).thenReturn(foreign);
        assertThatThrownBy(() -> keepsakeService.removeQuote("alice", foreign.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("没有找到");
    }

    // ========== F88 恋爱电影票根 ==========

    @Test
    void ticketValidatesDayAndRating() {
        stubSpace("alice");
        assertThatThrownBy(() -> keepsakeService.saveTicket("alice", "你的名字", "2025/05/20", 5, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("yyyy-MM-dd");
        assertThatThrownBy(() -> keepsakeService.saveTicket("alice", "你的名字", "2025-05-20", 9, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1-5 星");

        when(ticketMapper.findBySpace("s1")).thenReturn(List.of());
        keepsakeService.saveTicket("alice", "你的名字", "2025-05-20", null, "看完想立刻见到你");
        ArgumentCaptor<CoupleTicket> captor = ArgumentCaptor.forClass(CoupleTicket.class);
        verify(ticketMapper).insert(captor.capture());
        assertThat(captor.getValue().getRating()).isEqualTo(5); // 缺省满分
        verify(push).pushCoupleEvent(eq("ticket-added"), eq("alice"), eq("bob"), anyString());
    }

    // ========== F89 我们的歌单 ==========

    @Test
    void songSaveAndRemove() {
        stubSpace("bob");
        CoupleSong song = CoupleSong.of("s1", "bob", "告白气球", "周杰伦", "第一次约会时店里在放");
        when(songMapper.selectById(song.getId())).thenReturn(song);
        when(songMapper.findBySpace("s1")).thenReturn(List.of());

        keepsakeService.saveSong("bob", "告白气球", "周杰伦", "第一次约会时店里在放");
        verify(songMapper).insert(any(CoupleSong.class));
        verify(push).pushCoupleEvent(eq("song-added"), eq("bob"), eq("alice"), anyString());

        keepsakeService.removeSong("bob", song.getId());
        verify(songMapper).deleteById(song.getId());
    }

    // ========== F87 胶囊到期提醒 job ==========

    @Test
    void capsuleDueJobPushesReminderForToday() {
        String today = LocalDate.now().toString();
        CoupleCapsule capsule = CoupleCapsule.of("s1", "alice", "bob", "给未来的你", today);
        when(spaceMapper.findAllActive()).thenReturn(List.of(space()));
        when(capsuleMapper.findByOpenDay("s1", today)).thenReturn(List.of(capsule));

        memoryJob.remindDueCapsules();

        verify(push, times(1)).pushCoupleEventBoth(eq("capsule-due"), eq("system"), eq("alice"), eq("bob"), anyString());
    }
}
