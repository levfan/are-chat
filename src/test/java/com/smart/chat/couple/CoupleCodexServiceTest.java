package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
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
 * 我们百科（F280-F289）单测：词条共建推 TA 与删除归属、综艺攒词门槛/每日一期/双交默契率、
 * TOP10 建榜推猜与揭榜重新认识清单、外号小传 upsert、测验题面唯一与答错 7 天冷却、
 * 足迹同名覆盖、第一眼同刻互见与三次强制、习惯判案只归被观察人、口味变迁改写静默、
 * 人格八题校验与双报差异卡、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleCodexServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleCodexEntryMapper entryMapper;
    @Mock
    private CoupleQuizShowMapper quizMapper;
    @Mock
    private CoupleTopListMapper topListMapper;
    @Mock
    private CoupleTopGuessMapper topGuessMapper;
    @Mock
    private CouplePetnameStoryMapper storyMapper;
    @Mock
    private CoupleExamMapper examMapper;
    @Mock
    private CouplePlaceMapper placeMapper;
    @Mock
    private CoupleFirstLookMapper firstMapper;
    @Mock
    private CoupleHabitMapMapper habitMapper;
    @Mock
    private CoupleTasteShiftMapper tasteMapper;
    @Mock
    private CoupleTypeReportMapper typeMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleCodexService service;

    private static final String DAY = LocalDate.now().toString();

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

    // ========== F280 词条 ==========

    @Test
    void entryUpsertPushesAndRemoveByOwnerOnly() {
        stubSpace();
        List<CoupleCodexEntry> rows = new ArrayList<>();
        lenient().when(entryMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));
        lenient().when(entryMapper.findTerm("s1", "充电")).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getTerm().equals("充电")).findFirst().orElse(null));
        when(entryMapper.insert(any(CoupleCodexEntry.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(entryMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        when(entryMapper.deleteById(any(java.io.Serializable.class))).thenAnswer(inv -> {
            rows.removeIf(r -> r.getId().equals(inv.getArgument(0)));
            return 1;
        });
        service.saveEntry("alice", "充电", "抱一下回血", "第一次见面TA说的", "日常用法");
        verify(push).pushCoupleEvent(eq("codex-entry"), eq("alice"), eq("bob"), any());
        service.saveEntry("bob", "充电", "改成：亲一口快充", "", "");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getDefinition()).contains("快充");
        assertThatThrownBy(() -> service.removeEntry("bob", rows.get(0).getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("TA 自己撤");
        service.removeEntry("alice", rows.get(0).getId());
        assertThat(rows).isEmpty();
    }

    // ========== F281 默契综艺 ==========

    @Test
    void quizGuardsStartAndScoresBothAnswers() {
        stubSpace();
        List<CoupleCodexEntry> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            entries.add(CoupleCodexEntry.of("s1", "词" + i, "释义" + i, "", "", "alice"));
        }
        lenient().when(entryMapper.findBySpace("s1")).thenReturn(entries);
        List<CoupleQuizShow> shows = new ArrayList<>();
        lenient().when(quizMapper.findDay("s1", DAY)).thenAnswer(inv -> shows.stream().findFirst().orElse(null));
        lenient().when(quizMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(shows));
        when(quizMapper.insert(any(CoupleQuizShow.class))).thenAnswer(inv -> {
            shows.add(inv.getArgument(0));
            return 1;
        });
        when(quizMapper.updateById(any(CoupleQuizShow.class))).thenReturn(1);
        service.startQuiz("alice");
        assertThatThrownBy(() -> service.startQuiz("bob"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("考过一期");
        CoupleQuizShow show = shows.get(0);
        assertThat(show.getTerms().split(",")).hasSize(5);
        service.answerQuiz("alice", "a,b,c,d,e");
        verify(push).pushCoupleEvent(eq("codex-quiz-answer"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.answerQuiz("alice", "a,b,c,d,e"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("交卷");
        service.answerQuiz("bob", "a,x,c,z,e");
        verify(push).pushCoupleEventBoth(eq("codex-quiz-done"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(service.overview("alice").todayQuiz().match()).isEqualTo(3);
    }

    @Test
    void quizNeedsFiveEntries() {
        stubSpace();
        lenient().when(entryMapper.findBySpace("s1")).thenReturn(List.of());
        assertThatThrownBy(() -> service.startQuiz("alice"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还不到 5 条");
    }

    // ========== F282 TOP10 ==========

    @Test
    void topListGuessRevealRematch() {
        stubSpace();
        List<CoupleTopList> lists = new ArrayList<>();
        List<CoupleTopGuess> guesses = new ArrayList<>();
        lenient().when(topListMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(lists));
        lenient().when(topListMapper.find(eq("s1"), eq("FOOD"), any())).thenAnswer(inv -> lists.stream()
                .filter(l -> l.getOwnerUser().equals(inv.getArgument(2))).findFirst().orElse(null));
        lenient().when(topGuessMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(guesses));
        lenient().when(topGuessMapper.find(eq("s1"), eq("FOOD"), any())).thenAnswer(inv -> guesses.stream()
                .filter(g -> g.getGuesserUser().equals(inv.getArgument(2))).findFirst().orElse(null));
        when(topListMapper.insert(any(CoupleTopList.class))).thenAnswer(inv -> {
            lists.add(inv.getArgument(0));
            return 1;
        });
        when(topGuessMapper.insert(any(CoupleTopGuess.class))).thenAnswer(inv -> {
            guesses.add(inv.getArgument(0));
            return 1;
        });
        service.topList("bob", "FOOD", "火锅,日料,烧烤");
        verify(push).pushCoupleEvent(eq("codex-top-list"), eq("bob"), eq("alice"), any());
        assertThatThrownBy(() -> service.topList("bob", "NOPE", "x"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("类目不存在");
        service.topGuess("alice", "FOOD", "火锅,甜品,烧烤");
        verify(push).pushCoupleEvent(eq("codex-top-guess"), eq("alice"), eq("bob"), any());
        CoupleCodexService.TopBoardVO food = service.overview("alice").tops().stream()
                .filter(t -> t.category().equals("FOOD")).findFirst().orElseThrow();
        assertThat(food.revealed()).isTrue();
        assertThat(food.rematch()).anyMatch(line -> line.contains("日料"));
    }

    // ========== F283/F285/F288 upsert 家族 ==========

    @Test
    void storyPlaceTasteUpsertSemantics() {
        stubSpace();
        List<CouplePetnameStory> stories = new ArrayList<>();
        lenient().when(storyMapper.findBySpace("s1")).thenReturn(stories);
        lenient().when(storyMapper.findNick("s1", "猪猪")).thenAnswer(inv -> stories.stream().findFirst().orElse(null));
        when(storyMapper.insert(any(CouplePetnameStory.class))).thenAnswer(inv -> {
            stories.add(inv.getArgument(0));
            return 1;
        });
        service.story("alice", "猪猪", "bob", "第一次被投喂", "讲 TA 护食的故事", "");
        verify(push).pushCoupleEvent(eq("codex-story"), eq("alice"), eq("bob"), any());
        service.story("alice", "猪猪", "bob", "改写场合", "补充第二章", "");
        verify(push, times(1)).pushCoupleEvent(eq("codex-story"), any(), any(), any());

        List<CouplePlace> places = new ArrayList<>();
        lenient().when(placeMapper.findBySpace("s1")).thenReturn(places);
        lenient().when(placeMapper.findName("s1", "大理")).thenAnswer(inv -> places.stream().findFirst().orElse(null));
        when(placeMapper.insert(any(CouplePlace.class))).thenAnswer(inv -> {
            places.add(inv.getArgument(0));
            return 1;
        });
        service.place("alice", "大理", "2024", "骑车环海", 9);
        assertThat(places.get(0).getRating()).isEqualTo(5);
        assertThatThrownBy(() -> service.place("alice", "大理", "24x", "", null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy");

        List<CoupleTasteShift> tastes = new ArrayList<>();
        lenient().when(tasteMapper.findBySpace("s1")).thenReturn(tastes);
        lenient().when(tasteMapper.findThing("s1", "香菜", "alice")).thenAnswer(inv -> tastes.stream()
                .findFirst().orElse(null));
        when(tasteMapper.insert(any(CoupleTasteShift.class))).thenAnswer(inv -> {
            tastes.add(inv.getArgument(0));
            return 1;
        });
        service.taste("alice", "香菜", "闻不了", "现在就着火锅吃", "");
        verify(push).pushCoupleEvent(eq("codex-taste"), eq("alice"), eq("bob"), any());
        service.taste("alice", "香菜", "以前", "现在更爱了", LocalDate.now().plusDays(1).toString());
        verify(push, times(1)).pushCoupleEvent(eq("codex-taste"), any(), any(), any());
    }

    // ========== F284 友情测验 ==========

    @Test
    void examWrongThenCooldownThenRetry() {
        stubSpace();
        List<CoupleExam> rows = new ArrayList<>();
        lenient().when(examMapper.findBySpace("s1")).thenReturn(rows);
        lenient().when(examMapper.findQuestion("s1", "我闺蜜叫什么")).thenAnswer(inv -> rows.stream()
                .findFirst().orElse(null));
        when(examMapper.insert(any(CoupleExam.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(examMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        service.examAsk("alice", "我闺蜜叫什么", "小美");
        assertThatThrownBy(() -> service.examAsk("bob", "我闺蜜叫什么", "x"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("出过");
        CoupleExam exam = rows.get(0);
        assertThatThrownBy(() -> service.examTry("alice", exam.getId(), "小美"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("考你的");
        service.examTry("bob", exam.getId(), "美美");
        verify(push).pushCoupleEvent(eq("codex-exam-miss"), eq("bob"), eq("alice"), any());
        assertThatThrownBy(() -> service.examTry("bob", exam.getId(), "小美"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("补考");
        exam.setLastTryDay(LocalDate.now().minusDays(CoupleCodexService.EXAM_RETRY_DAYS).toString());
        service.examTry("bob", exam.getId(), "小美");
        assertThat(exam.getVerdict()).isEqualTo("RIGHT");
        verify(push).pushCoupleEvent(eq("codex-exam-hit"), eq("bob"), eq("alice"), any());
        assertThatThrownBy(() -> service.examTry("bob", exam.getId(), "小美"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("答对");
    }

    // ========== F286 第一眼对视 ==========

    @Test
    void firstLookSameMomentRevealsBoth() {
        stubSpace();
        List<CoupleFirstLook> rows = new ArrayList<>();
        lenient().when(firstMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));
        lenient().when(firstMapper.findUser(eq("s1"), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getFromUser().equals(inv.getArgument(1))).findFirst().orElse(null));
        when(firstMapper.insert(any(CoupleFirstLook.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.firstLook("alice", "演唱会散场那秒你回头");
        service.firstLook("bob", "演唱会散场那秒你回头");
        verify(push).pushCoupleEventBoth(eq("codex-firstlook-match"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(rows).allMatch(CoupleFirstLook::revealedFlag);
        assertThatThrownBy(() -> service.firstLook("alice", "再改一版"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("尘埃落定");
    }

    @Test
    void firstLookForcesRevealAfterThreeTries() {
        stubSpace();
        List<CoupleFirstLook> rows = new ArrayList<>();
        lenient().when(firstMapper.findBySpace("s1")).thenReturn(rows);
        lenient().when(firstMapper.findUser(eq("s1"), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getFromUser().equals(inv.getArgument(1))).findFirst().orElse(null));
        when(firstMapper.insert(any(CoupleFirstLook.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.firstLook("alice", "版本一");
        service.firstLook("bob", "另一个版本一");
        service.firstLook("alice", "版本二");
        service.firstLook("bob", "另一个版本二");
        service.firstLook("alice", "版本三");
        service.firstLook("bob", "另一个版本三");
        verify(push).pushCoupleEventBoth(eq("codex-firstlook-force"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.firstLook("alice", "还想改"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("尘埃落定");
    }

    // ========== F287 习惯图鉴 ==========

    @Test
    void habitDuplicateAndVerdictOwnership() {
        stubSpace();
        List<CoupleHabitMap> rows = new ArrayList<>();
        lenient().when(habitMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));
        lenient().when(habitMapper.findHabit(eq("s1"), any(), eq("alice"))).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getHabit().equals(inv.getArgument(1))).findFirst().orElse(null));
        when(habitMapper.insert(any(CoupleHabitMap.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(habitMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        service.habitAdd("alice", "睡前要检查门锁两遍", "每天");
        assertThatThrownBy(() -> service.habitAdd("alice", "睡前要检查门锁两遍", "偶尔"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("图鉴在案");
        CoupleHabitMap h = rows.get(0);
        assertThatThrownBy(() -> service.habitVerdict("alice", h.getId(), "REAL"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("得你亲自判");
        assertThatThrownBy(() -> service.habitVerdict("bob", h.getId(), "MAYBE"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("确实」或「冤枉");
        service.habitVerdict("bob", h.getId(), "REAL");
        verify(push).pushCoupleEvent(eq("codex-habit-verdict"), eq("bob"), eq("alice"), any());
    }

    // ========== F289 人格双报 ==========

    @Test
    void typeReportValidatesAndDiffs() {
        stubSpace();
        String year = String.valueOf(LocalDate.now().getYear());
        List<CoupleTypeReport> rows = new ArrayList<>();
        lenient().when(typeMapper.find(eq("s1"), eq(year), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getFromUser().equals(inv.getArgument(2))).findFirst().orElse(null));
        lenient().when(typeMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));
        when(typeMapper.insert(any(CoupleTypeReport.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        assertThatThrownBy(() -> service.typeReport("alice", "1,2,1"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("八题");
        service.typeReport("alice", "1,1,1,1,1,1,1,1");
        assertThat(rows.get(0).getTypeKey()).isEqualTo("ESTJ");
        service.typeReport("bob", "2,2,2,2,2,2,2,2");
        assertThat(rows.get(1).getTypeKey()).isEqualTo("INFP");
        verify(push).pushCoupleEventBoth(eq("codex-type-both"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(service.overview("alice").type().diffLine()).contains("0/4 轴相同");
        service.typeReport("alice", "2,2,2,2,2,2,2,2");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getTypeKey()).isEqualTo("INFP");
    }

    // ========== 兜底 ==========

    @Test
    void noSpaceLeadsTo404() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.overview("solo"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没有建立情侣空间");
    }
}
