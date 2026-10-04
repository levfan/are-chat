package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.content.CoupleQuestionBank;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestionAnswerMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestionAnswerPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpacePO;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.sharedkernel.web.BusinessException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 每日一问单测：锁的是「互看」的时序与一天一行的落库形状。
 * 同空间同天必须同题，否则两个人的答案对不上同一个问题。
 */
@ExtendWith(MockitoExtension.class)
class CoupleQuestionServiceTest {

    private static final String SPACE = "s1";
    private static final String DAY = LocalDate.now().toString();

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleQuestionAnswerMapper answerMapper;
    @Mock
    private CoupleEventPublisher push;

    @InjectMocks
    private CoupleQuestionService service;

    private final class Bag {
        private final List<CoupleQuestionAnswerPO> rows = new ArrayList<>();

        Bag stub() {
            lenient().when(answerMapper.findBySpace(SPACE)).thenAnswer(inv -> List.copyOf(rows));
            lenient().when(answerMapper.findBySpaceAndDay(SPACE, DAY)).thenAnswer(inv -> rows.stream()
                    .filter(r -> r.getDay().equals(DAY)).toList());
            lenient().when(answerMapper.findBySpaceFrom(eq(SPACE), anyString())).thenAnswer(inv -> rows);
            lenient().when(answerMapper.find(eq(SPACE), anyString(), anyString())).thenAnswer(inv -> rows.stream()
                    .filter(r -> r.getDay().equals(inv.getArgument(1, String.class))
                            && r.getUsername().equals(inv.getArgument(2, String.class)))
                    .findFirst());
            lenient().when(answerMapper.insert(any(CoupleQuestionAnswerPO.class))).thenAnswer(inv -> {
                rows.add(inv.getArgument(0));
                return 1;
            });
            lenient().when(answerMapper.updateById(any(CoupleQuestionAnswerPO.class))).thenReturn(1);
            return this;
        }

        void answered(String who, String text) {
            CoupleQuestionAnswerPO row = CoupleQuestionAnswerPO.of(SPACE, DAY,
                    CoupleQuestionBank.indexOf(SPACE, DAY), CoupleQuestionBank.textAt(0), who, text);
            rows.add(row);
        }
    }

    private void stubSpace() {
        CoupleSpacePO space = new CoupleSpacePO();
        space.setId(SPACE);
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpacePO.STATUS_ACTIVE);
        space.setCreated(System.currentTimeMillis());
        lenient().when(spaceMapper.findActiveByUser(anyString())).thenReturn(Optional.of(space));
    }

    @Test
    void sameSpaceSameDayAlwaysGetsTheSameQuestion() {
        int first = CoupleQuestionBank.indexOf(SPACE, DAY);
        int again = CoupleQuestionBank.indexOf(SPACE, DAY);
        assertThat(first).isEqualTo(again);
        assertThat(CoupleQuestionBank.textAt(first)).isEqualTo(CoupleQuestionBank.all().get(first));
        assertThat(CoupleQuestionBank.size()).isGreaterThanOrEqualTo(60);
        assertThat(CoupleQuestionBank.all()).doesNotHaveDuplicates();
        // 题号必须落在集合内，越界就意味着题库与快照口径漂移了
        assertThat(first).isBetween(0, CoupleQuestionBank.size() - 1);
    }

    @Test
    void partnerAnswerStaysHiddenUntilBothAreDone() {
        stubSpace();
        Bag bag = new Bag().stub();
        bag.answered("bob", "吃到了一碗很好的面");

        CoupleQuestionService.TodayVO vo = service.today("alice");
        assertThat(vo.answeredByPartner()).isTrue();
        assertThat(vo.answeredByMe()).isFalse();
        assertThat(vo.partnerAnswer()).isNull();
        assertThat(vo.bothAnswered()).isFalse();

        service.answer("alice", "见到你");
        CoupleQuestionService.TodayVO after = service.today("alice");
        assertThat(after.bothAnswered()).isTrue();
        assertThat(after.partnerAnswer()).isEqualTo("吃到了一碗很好的面");
        assertThat(after.mine().answer()).isEqualTo("见到你");
    }

    @Test
    void answeringTwiceEditsTheSameRowAndTellsThePartnerItIsComplete() {
        stubSpace();
        Bag bag = new Bag().stub();
        bag.answered("bob", "已经答过了");

        service.answer("alice", "第一版答案");
        service.answer("alice", "改了一版答案");

        assertThat(bag.rows).hasSize(2);
        assertThat(bag.rows.stream().filter(r -> r.getUsername().equals("alice")).count()).isEqualTo(1);
        CoupleQuestionAnswerPO mine = bag.rows.stream()
                .filter(r -> r.getUsername().equals("alice")).findFirst().orElseThrow();
        assertThat(mine.getAnswer()).isEqualTo("改了一版答案");
        assertThat(mine.getUpdatedAt()).isNotNull();
        verify(answerMapper, times(1)).updateById(any(CoupleQuestionAnswerPO.class));
        verify(push, times(2)).pushCoupleEvent(eq("question-answered"), eq("alice"), eq("bob"), anyString());
    }

    @Test
    void answerTextGatesWriteNothing() {
        stubSpace();
        Bag bag = new Bag().stub();

        assertThatThrownBy(() -> service.answer("alice", "  "))
                .isInstanceOf(BusinessException.class).hasMessage("写一句再交卷呀 📝");
        assertThatThrownBy(() -> service.answer("alice", "一".repeat(301)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("300");
        assertThat(bag.rows).isEmpty();
    }

    @Test
    void historyOnlyRevealsThePartnerOnDaysBothAnswered() {
        stubSpace();
        Bag bag = new Bag().stub();
        bag.answered("alice", "我的答案");
        bag.answered("bob", "TA 的答案");

        CoupleQuestionService.HistoryListVO list = service.history("alice", 14);

        assertThat(list.items()).hasSize(1);
        assertThat(list.items().get(0).partnerAnswer()).isEqualTo("TA 的答案");
        assertThat(list.answeredDays()).isEqualTo(1);
        assertThat(list.bothAnsweredDays()).isEqualTo(1);
    }

    @Test
    void noSpaceThrowsTheStandardFourOhFour() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.today("solo"))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
    }
}
