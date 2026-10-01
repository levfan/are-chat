package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 趣味游戏（F130-F139）核心逻辑单测：一百问解锁机制、出题考TA 判分权限、
 * 心动概率/塔罗/天气按日稳定、情话课收藏、周末盲选结算、情话Battle 状态机、抽象画入馆。
 */
@ExtendWith(MockitoExtension.class)
class CouplePlayServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleSurveyAnswerMapper surveyMapper;
    @Mock
    private CoupleQuizDuelMapper quizMapper;
    @Mock
    private CoupleLoveWordMapper loveWordMapper;
    @Mock
    private CoupleBlindPickMapper blindMapper;
    @Mock
    private CoupleSweetBattleMapper battleMapper;
    @Mock
    private CoupleSweetLineMapper lineMapper;
    @Mock
    private CoupleArtGalleryMapper artMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CouplePlayService playService;

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

    // ========== F130 一百问 ==========

    @Test
    void partnerAnswerUnlockedOnlyWhenIMetSameQuestion() {
        stubSpace("alice");
        CoupleSurveyAnswer myQ1 = CoupleSurveyAnswer.of("s1", "alice", 1, "你的笑");
        CoupleSurveyAnswer partnerQ1 = CoupleSurveyAnswer.of("s1", "bob", 1, "你的眼睛");
        CoupleSurveyAnswer partnerQ2 = CoupleSurveyAnswer.of("s1", "bob", 2, "海边");
        when(surveyMapper.findByUser("s1", "alice")).thenReturn(List.of(myQ1));
        when(surveyMapper.findByUser("s1", "bob")).thenReturn(List.of(partnerQ1, partnerQ2));

        CouplePlayService.SurveyVO vo = playService.survey("alice");

        // 我答了第 1 题：解锁 TA 的第 1 题；第 2 题未答不展示
        assertThat(vo.partnerUnlocked()).hasSize(1);
        assertThat(vo.partnerUnlocked().get(0).qNo()).isEqualTo(1);
    }

    @Test
    void answerSurveyRejectsBadQNo() {
        stubSpace("alice");
        assertThatThrownBy(() -> playService.answerSurvey("alice", 0, "x"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1-100");
        assertThatThrownBy(() -> playService.answerSurvey("alice", 101, "x"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1-100");
    }

    // ========== F131 出题考TA ==========

    @Test
    void partnerAnswerHiddenFromAskerUntilJudged() {
        stubSpace("alice");
        CoupleQuizDuel quiz = CoupleQuizDuel.of("s1", "bob", "我最喜欢的颜色？");
        quiz.setAnswerText("蓝色");
        quiz.setStatus(CoupleQuizDuel.STATUS_ANSWERED);
        when(quizMapper.findBySpace("s1")).thenReturn(List.of(quiz));

        // alice 不是出题人，作答内容在判分前不可见
        List<CouplePlayService.QuizVO> list = playService.quizzes("alice");
        assertThat(list.get(0).answerText()).isNull();

        // bob 是出题人，可见
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space()));
        list = playService.quizzes("bob");
        assertThat(list.get(0).answerText()).isEqualTo("蓝色");
    }

    @Test
    void judgeRequiresAskerAndAnsweredStatus() {
        stubSpace("bob");
        CoupleQuizDuel quiz = CoupleQuizDuel.of("s1", "alice", "题");
        quiz.setStatus(CoupleQuizDuel.STATUS_ANSWERED);
        when(quizMapper.selectById(quiz.getId())).thenReturn(quiz);
        // alice 出的题，bob 判分应被拒
        assertThatThrownBy(() -> playService.judgeQuiz("bob", quiz.getId(), CoupleQuizDuel.VERDICT_RIGHT))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("判分权");
    }

    @Test
    void judgeRightPushesBoth() {
        stubSpace("alice");
        CoupleQuizDuel quiz = CoupleQuizDuel.of("s1", "alice", "题");
        quiz.setStatus(CoupleQuizDuel.STATUS_ANSWERED);
        quiz.setAnswerText("答");
        when(quizMapper.selectById(quiz.getId())).thenReturn(quiz);
        when(quizMapper.findBySpace("s1")).thenReturn(List.of(quiz));

        playService.judgeQuiz("alice", quiz.getId(), CoupleQuizDuel.VERDICT_RIGHT);

        assertThat(quiz.getStatus()).isEqualTo(CoupleQuizDuel.STATUS_JUDGED);
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ========== F132/F133/F137 无表项：按日稳定 ==========

    @Test
    void heartbeatStableWithinSameDay() {
        stubSpace("alice");
        CouplePlayService.HeartbeatVO first = playService.heartbeat("alice");
        CouplePlayService.HeartbeatVO second = playService.heartbeat("alice");
        assertThat(first.score()).isEqualTo(second.score());
        assertThat(first.score()).isBetween(0, 100);
    }

    @Test
    void tarotReturnsCardWithMessage() {
        stubSpace("alice");
        CouplePlayService.TarotVO vo = playService.tarot("alice");
        assertThat(vo.name()).isNotBlank();
        assertThat(vo.message()).isNotBlank();
    }

    @Test
    void weatherHasTip() {
        stubSpace("alice");
        CouplePlayService.WeatherVO vo = playService.weather("alice");
        assertThat(vo.tip()).isNotBlank();
    }

    // ========== F134 世界情话课 ==========

    @Test
    void collectLoveWordAddsToCollection() {
        stubSpace("alice");
        when(loveWordMapper.findBySpace("s1")).thenReturn(List.of(CoupleLoveWord.of("s1", "alice", "x", null)));
        CouplePlayService.LessonVO vo = playService.collectLoveWord("alice", "Je t'aime", "我爱你");
        assertThat(vo.collected()).hasSize(1);
        verify(push).pushCoupleEvent(anyString(), anyString(), anyString(), anyString());
    }

    // ========== F135 周末盲选 ==========

    @Test
    void blindPickSettlesWhenBothSubmitted() {
        stubSpace("alice");
        String week = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString();
        CoupleBlindPick mine = CoupleBlindPick.of("s1", "alice", week, "爬山,看电影,吃火锅");
        CoupleBlindPick partner = CoupleBlindPick.of("s1", "bob", week, "逛展,做饭,野餐");
        when(blindMapper.findByWeek("s1", week)).thenReturn(List.of(mine, partner));
        when(blindMapper.findBySpace("s1")).thenReturn(List.of(mine, partner));

        CouplePlayService.BlindVO vo = playService.blindPick("alice");

        assertThat(vo.settled()).isTrue();
        assertThat(vo.planMine()).isIn("爬山", "看电影", "吃火锅");
        assertThat(vo.planPartner()).isIn("逛展", "做饭", "野餐");
    }

    @Test
    void blindPickRequiresAtLeastTwoPicks() {
        stubSpace("alice");
        assertThatThrownBy(() -> playService.submitBlindPick("alice", List.of("只有一条")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("至少");
    }

    // ========== F136 情话Battle ==========

    @Test
    void battleFullWhenBothJoined() {
        stubSpace("alice");
        String today = LocalDate.now().toString();
        when(battleMapper.findByDay("s1", today)).thenReturn(null);
        when(lineMapper.findByBattle(anyString())).thenReturn(List.of(
                CoupleSweetLine.of("s1", "b1", "bob", "TA 的句子"),
                CoupleSweetLine.of("s1", "b1", "alice", "我的句子")));
        when(battleMapper.findBySpace("s1")).thenReturn(List.of());

        // 第一次调用：先建 battle（insert），join 后双方到齐
        when(lineMapper.findByBattle(anyString())).thenReturn(List.of(
                        CoupleSweetLine.of("s1", "b1", "bob", "TA 的句子")))
                .thenReturn(List.of(
                        CoupleSweetLine.of("s1", "b1", "bob", "TA 的句子"),
                        CoupleSweetLine.of("s1", "b1", "alice", "我的句子")));

        playService.joinBattle("alice", "你今天真好看");

        ArgumentCaptor<CoupleSweetBattle> captor = ArgumentCaptor.forClass(CoupleSweetBattle.class);
        verify(battleMapper).insert(captor.capture());
        verify(battleMapper).updateById(captor.getValue());
        assertThat(captor.getValue().getStatus()).isEqualTo(CoupleSweetBattle.STATUS_FULL);
    }

    @Test
    void voteRequiresOnStageTarget() {
        stubSpace("alice");
        String today = LocalDate.now().toString();
        CoupleSweetBattle battle = CoupleSweetBattle.of("s1", today);
        battle.setStatus(CoupleSweetBattle.STATUS_FULL);
        when(battleMapper.findByDay("s1", today)).thenReturn(battle);

        assertThatThrownBy(() -> playService.voteBattle("alice", "eve"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("场上两句");
    }

    @Test
    void bothVotesSettleWinner() {
        stubSpace("alice");
        String today = LocalDate.now().toString();
        CoupleSweetBattle battle = CoupleSweetBattle.of("s1", today);
        battle.setStatus(CoupleSweetBattle.STATUS_FULL);
        // bob（userB）已投给 alice，alice 再投自己 → 同一人 → 有赢家
        battle.setVoteB("alice");
        when(battleMapper.findByDay("s1", today)).thenReturn(battle);
        when(lineMapper.findByBattle(battle.getId())).thenReturn(List.of(
                CoupleSweetLine.of("s1", battle.getId(), "alice", "我的句子")));
        when(battleMapper.findBySpace("s1")).thenReturn(List.of(battle));

        playService.voteBattle("alice", "alice");

        assertThat(battle.getStatus()).isEqualTo(CoupleSweetBattle.STATUS_DONE);
        assertThat(battle.getWinner()).isEqualTo("alice");
    }

    // ========== F138 抽象画 ==========

    @SuppressWarnings("unchecked")
    @Test
    void createArtNormalizesSeed() {
        stubSpace("alice");
        when(artMapper.findBySpace("s1")).thenReturn(List.of());
        playService.createArt("alice", "无题", 123456);
        ArgumentCaptor<CoupleArtGallery> captor = ArgumentCaptor.forClass(CoupleArtGallery.class);
        verify(artMapper).insert(captor.capture());
        assertThat(captor.getValue().getSeed()).isBetween(0, 99999);
    }
}
