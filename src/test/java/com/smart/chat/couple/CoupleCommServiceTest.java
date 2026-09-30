package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 会说情话·沟通增强（F100-F109）核心逻辑单测：翻译词典命中、冷静角规则、
 * 接力棒归属与回抛、比划猜限流/提示校验/结算、接龙轮替、词典小考出题、道歉收下、情绪词 upsert、晚安电台。
 */
@ExtendWith(MockitoExtension.class)
class CoupleCommServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleCoolDownMapper coolMapper;
    @Mock
    private CoupleMoodRelayMapper relayMapper;
    @Mock
    private CoupleGuessRoundMapper guessMapper;
    @Mock
    private CoupleStoryLineMapper storyMapper;
    @Mock
    private CoupleApologyCardMapper apologyMapper;
    @Mock
    private CoupleFeelingWordMapper feelingMapper;
    @Mock
    private CoupleDictWordMapper dictMapper;
    @Mock
    private CoupleSongMapper songMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleCommService commService;

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

    // ========== F100 恋爱翻译器 ==========

    @Test
    void translateHitsKnownPhrase() {
        CoupleChatBank.Translation hit = commService.translate("我没事");
        assertThat(hit.subtext()).contains("有事");
        assertThat(hit.reply()).isNotBlank();
    }

    @Test
    void translateMissFallsBackToCarefulReply() {
        CoupleChatBank.Translation miss = commService.translate("今天月亮好圆");
        assertThat(miss.reply()).isNotBlank();
    }

    // ========== F101 冷静角 ==========

    @Test
    void startCoolDownRejectsWhenActiveExists() {
        stubSpace("alice");
        when(coolMapper.findActive("s1")).thenReturn(new CoupleCoolDown());
        assertThatThrownBy(() -> commService.startCoolDown("alice", "吵架了"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("冷静");
    }

    @Test
    void softenBeforeEndAtIsRejected() {
        stubSpace("alice");
        CoupleCoolDown row = CoupleCoolDown.of("s1", "bob", null);
        row.setEndAt(System.currentTimeMillis() + 60_000);
        when(coolMapper.selectById(row.getId())).thenReturn(row);
        assertThatThrownBy(() -> commService.soften("alice", row.getId(), "抱抱"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("冷静期还没结束");
    }

    @Test
    void bothSoftenedHealsAndPushesBoth() {
        stubSpace("alice");
        CoupleCoolDown row = CoupleCoolDown.of("s1", "bob", null);
        row.setEndAt(System.currentTimeMillis() - 1000);
        row.setSoftB("我也有不对");
        when(coolMapper.selectById(row.getId())).thenReturn(row);
        when(coolMapper.findBySpace("s1")).thenReturn(List.of(row));

        commService.soften("alice", row.getId(), "抱抱，我们和好");

        assertThat(row.getStatus()).isEqualTo(CoupleCoolDown.STATUS_HEALED);
        assertThat(row.getSoftA()).isEqualTo("抱抱，我们和好");
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ========== F102 情绪接力棒 ==========

    @Test
    void tossRejectedWhenRelayPending() {
        stubSpace("alice");
        when(relayMapper.findPending("s1")).thenReturn(new CoupleMoodRelay());
        assertThatThrownBy(() -> commService.tossRelay("alice", "开心", "🥳", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还在路上");
    }

    @Test
    void catchOwnRelayForbidden() {
        stubSpace("alice");
        CoupleMoodRelay relay = CoupleMoodRelay.of("s1", "alice", "委屈", null, null);
        when(relayMapper.selectById(relay.getId())).thenReturn(relay);
        assertThatThrownBy(() -> commService.catchRelay("alice", relay.getId(), "抱抱", null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能自己接");
    }

    @Test
    void catchMarksCaughtAndTossesBack() {
        stubSpace("bob");
        CoupleMoodRelay relay = CoupleMoodRelay.of("s1", "alice", "委屈", "🥺", null);
        when(relayMapper.selectById(relay.getId())).thenReturn(relay);
        when(relayMapper.findBySpace("s1")).thenReturn(List.of(relay));

        commService.catchRelay("bob", relay.getId(), "抱抱你", "被治愈", "🥰", null);

        assertThat(relay.getStatus()).isEqualTo(CoupleMoodRelay.STATUS_CAUGHT);
        verify(relayMapper).insert(any(CoupleMoodRelay.class));
        verify(push, atLeastOnce()).pushCoupleEvent(anyString(), anyString(), anyString(), anyString());
    }

    // ========== F103 你比划我猜 ==========

    @Test
    void startGuessRespectsDailyLimit() {
        stubSpace("alice");
        when(guessMapper.countByDay("s1", LocalDate.now().toString()))
                .thenReturn((long) CoupleGuessRound.DAILY_ROUNDS);
        assertThatThrownBy(() -> commService.startGuess("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("额度");
    }

    @Test
    void clueCannotContainTheWord() {
        stubSpace("alice");
        CoupleGuessRound round = CoupleGuessRound.of("s1", LocalDate.now().toString(), "alice", "吃火锅");
        when(guessMapper.selectById(round.getId())).thenReturn(round);
        assertThatThrownBy(() -> commService.clueGuess("alice", round.getId(), "一起去吃火锅吧"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("吃火锅");
    }

    @Test
    void thirdWrongGuessSettlesMissed() {
        stubSpace("bob");
        CoupleGuessRound round = CoupleGuessRound.of("s1", LocalDate.now().toString(), "alice", "吃火锅");
        round.setClue("辣辣的涮涮的");
        round.setStatus(CoupleGuessRound.STATUS_CLUED);
        round.setAttempts(2);
        when(guessMapper.selectById(round.getId())).thenReturn(round);
        when(guessMapper.findBySpace("s1")).thenReturn(List.of(round));

        commService.doGuess("bob", round.getId(), "麻辣烫");

        assertThat(round.getStatus()).isEqualTo(CoupleGuessRound.STATUS_MISSED);
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void correctGuessSettlesHit() {
        stubSpace("bob");
        CoupleGuessRound round = CoupleGuessRound.of("s1", LocalDate.now().toString(), "alice", "吃火锅");
        round.setClue("辣辣的涮涮的");
        round.setStatus(CoupleGuessRound.STATUS_CLUED);
        when(guessMapper.selectById(round.getId())).thenReturn(round);
        when(guessMapper.findBySpace("s1")).thenReturn(List.of(round));

        commService.doGuess("bob", round.getId(), "吃火锅");

        assertThat(round.getStatus()).isEqualTo(CoupleGuessRound.STATUS_HIT);
    }

    // ========== F104 故事接龙 ==========

    @Test
    void addStoryLineEnforcesAlternatingTurns() {
        stubSpace("alice");
        CoupleStoryLine last = CoupleStoryLine.of("s1", "c1", 1, "alice", "很久很久以前", false);
        when(storyMapper.findLastOfChain("s1", "c1")).thenReturn(last);
        assertThatThrownBy(() -> commService.addStoryLine("alice", "c1", "有一只小猫"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("轮着来");
    }

    @Test
    void addStoryLineAfterFinalRejected() {
        stubSpace("alice");
        CoupleStoryLine last = CoupleStoryLine.of("s1", "c1", 2, "bob", "小猫回家了", true);
        when(storyMapper.findLastOfChain("s1", "c1")).thenReturn(last);
        assertThatThrownBy(() -> commService.addStoryLine("alice", "c1", "完结撒花"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("完结");
    }

    @Test
    void addStoryLineAppendsAndPushes() {
        stubSpace("alice");
        CoupleStoryLine last = CoupleStoryLine.of("s1", "c1", 1, "bob", "很久很久以前", false);
        when(storyMapper.findLastOfChain("s1", "c1")).thenReturn(last);
        when(storyMapper.findRecent("s1", 300)).thenReturn(List.of(last, CoupleStoryLine.of("s1", "c1", 2, "alice", "有一只小猫", false)));

        List<CoupleCommService.StoryVO> stories = commService.addStoryLine("alice", "c1", "有一只小猫");

        assertThat(stories).hasSize(1);
        assertThat(stories.get(0).lines()).hasSize(2);
        verify(push).pushCoupleEvent(anyString(), anyString(), anyString(), anyString());
    }

    // ========== F105 词典小考 ==========

    @Test
    void dictQuizRequiresWords() {
        stubSpace("alice");
        when(dictMapper.findBySpace("s1")).thenReturn(List.of());
        assertThatThrownBy(() -> commService.dictQuiz("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("恋爱词典");
    }

    @Test
    void dictQuizBuildsFourOptions() {
        stubSpace("alice");
        CoupleDictWord w1 = CoupleDictWord.of("s1", "alice", "小老虎", "生气的时候像小老虎");
        CoupleDictWord w2 = CoupleDictWord.of("s1", "bob", "奶茶日", "每周三必须喝奶茶");
        when(dictMapper.findBySpace("s1")).thenReturn(List.of(w1, w2));

        CoupleCommService.DictQuizVO quiz = commService.dictQuiz("alice");

        assertThat(quiz.options()).hasSize(4);
        assertThat(quiz.options().get(quiz.correctIndex())).isNotBlank();
    }

    // ========== F107 道歉三部曲 ==========

    @Test
    void apologyRequiresAllThreeSections() {
        stubSpace("alice");
        assertThatThrownBy(() -> commService.sendApology("alice", "迟到", null, "以后提前出门"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("三步");
    }

    @Test
    void acceptApologyByPartnerOnly() {
        stubSpace("alice");
        CoupleApologyCard card = CoupleApologyCard.of("s1", "alice", "迟到", "让你等了半小时", "提前出门");
        when(apologyMapper.selectById(card.getId())).thenReturn(card);
        assertThatThrownBy(() -> commService.acceptApology("alice", card.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己收下");
    }

    @Test
    void acceptMarksAccepted() {
        stubSpace("bob");
        CoupleApologyCard card = CoupleApologyCard.of("s1", "alice", "迟到", "让你等了半小时", "提前出门");
        when(apologyMapper.selectById(card.getId())).thenReturn(card);
        when(apologyMapper.findBySpace("s1")).thenReturn(List.of(card));

        commService.acceptApology("bob", card.getId());

        assertThat(card.getStatus()).isEqualTo(CoupleApologyCard.STATUS_ACCEPTED);
        verify(push).pushCoupleEvent(anyString(), anyString(), anyString(), anyString());
    }

    // ========== F108 情绪词汇 ==========

    @Test
    void saveFeelingUpdatesSameDay() {
        stubSpace("alice");
        CoupleFeelingWord existing = CoupleFeelingWord.of("s1", "alice", LocalDate.now().toString(), "累", null);
        when(feelingMapper.findBySpace("s1", 60)).thenReturn(List.of(existing));

        List<CoupleFeelingWord> result = commService.saveFeeling("alice", "被治愈", "抱了一下就好了");

        assertThat(result).hasSize(1);
        assertThat(existing.getWord()).isEqualTo("被治愈");
        verify(feelingMapper, never()).insert(any(CoupleFeelingWord.class));
    }

    @Test
    void saveFeelingNewWordPushesToPartner() {
        stubSpace("alice");
        when(feelingMapper.findBySpace("s1", 60)).thenReturn(List.of());

        commService.saveFeeling("alice", "元气满满", null);

        verify(feelingMapper).insert(any(CoupleFeelingWord.class));
        verify(push).pushCoupleEvent(anyString(), anyString(), anyString(), anyString());
    }

    // ========== F109 晚安电台 ==========

    @Test
    void radioFallsBackWithoutSongs() {
        stubSpace("alice");
        when(songMapper.findBySpace("s1")).thenReturn(List.of());

        CoupleCommService.RadioVO radio = commService.goodnightRadio("alice");

        assertThat(radio.hasSong()).isFalse();
        assertThat(radio.line()).isNotBlank();
    }

    @Test
    void radioPicksSongWhenAvailable() {
        stubSpace("alice");
        CoupleSong song = CoupleSong.of("s1", "alice", "小情歌", "苏打绿", "我们的定情曲");
        when(songMapper.findBySpace("s1")).thenReturn(List.of(song));

        CoupleCommService.RadioVO radio = commService.goodnightRadio("alice");

        assertThat(radio.hasSong()).isTrue();
        assertThat(radio.title()).isEqualTo("小情歌");
        assertThat(radio.line()).contains("小情歌");
    }
}
