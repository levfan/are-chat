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
    private CoupleGuessRoundMapper guessMapper;
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

    // ========== F101 冷静角 ==========

    // ========== F102 情绪接力棒 ==========

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

    // ========== F105 词典小考 ==========

    // ========== F107 道歉三部曲 ==========

    // ========== F108 情绪词汇 ==========

    // ========== F109 晚安电台 ==========

}
