package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.persistence.CoupleBondDayMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleBondDayPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestionAnswerMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestionAnswerPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpacePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleWishMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleWishPO;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 百日隐藏页单测：闸门必须挡在服务端（前端藏页签不等于接口调不到），
 * 而且时间轴上每一条都得有真实日子——凑不出日期的一律不上轴。
 */
@ExtendWith(MockitoExtension.class)
class CoupleMemoryServiceTest {

    private static final String SPACE = "s1";

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleBondDayMapper bondDayMapper;
    @Mock
    private CoupleQuestionAnswerMapper answerMapper;
    @Mock
    private CoupleWishMapper wishMapper;
    @Mock
    private CoupleService coupleService;

    @InjectMocks
    private CoupleMemoryService service;

    private CoupleSpacePO space() {
        CoupleSpacePO space = new CoupleSpacePO();
        space.setId(SPACE);
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpacePO.STATUS_ACTIVE);
        space.setCreated(LocalDate.of(2026, 6, 28).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        space.setAnniversary("2026-06-28");
        return space;
    }

    /** 从 endDay 往前数 n 天，铺成连续打卡日。 */
    private List<CoupleBondDayPO> consecutive(int n, LocalDate endDay) {
        List<CoupleBondDayPO> rows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rows.add(CoupleBondDayPO.of(SPACE, endDay.minusDays(i).toString(),
                    i == 3 ? CoupleBondDayPO.SOURCE_MAKEUP : CoupleBondDayPO.SOURCE_AUTO, null));
        }
        return rows;
    }

    private void stubEmptyWishesAndAnswers() {
        lenient().when(wishMapper.findBySpace(SPACE)).thenReturn(List.of());
        lenient().when(answerMapper.findBySpace(SPACE)).thenReturn(List.of());
        lenient().when(coupleService.intimacy(anyString())).thenReturn(new CoupleService.IntimacyVO(
                1400, 7, "相守一生", "💍", null, 100,
                new CoupleService.IntimacyBreakdown(100, 100, 10, 4, 2, 300)));
    }

    @Test
    void hiddenPageIsGatedOnTheServerNotJustTheFrontend() {
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space()));
        when(bondDayMapper.findBySpace(SPACE)).thenReturn(consecutive(90, LocalDate.now()));
        stubEmptyWishesAndAnswers();

        assertThatThrownBy(() -> service.memory("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("连续贴满 100 天")
                .hasMessageContaining("还差 10 天");
    }

    @Test
    void noSpaceThrowsTheStandardFourOhFour() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.memory("solo"))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
    }

    @Test
    void unlockedPageShowsEveryTierStampedWithItsRealDay() {
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space()));
        when(bondDayMapper.findBySpace(SPACE)).thenReturn(consecutive(100, LocalDate.now()));
        stubEmptyWishesAndAnswers();

        CoupleMemoryService.MemoryVO vo = service.memory("alice");

        assertThat(vo.longestStreak()).isEqualTo(100);
        assertThat(vo.makeupDays()).isEqualTo(1);
        assertThat(vo.summary()).contains("在一起 ").contains("贴了 100 天的卡").contains("「相守一生」");
        // 七个档位都该有时间戳，且都是这一轮连续段里派生出来的真实日子
        List<CoupleMemoryService.TimelineItemVO> unlocks = vo.timeline().stream()
                .filter(item -> "unlock".equals(item.kind())).toList();
        assertThat(unlocks).hasSize(7);
        assertThat(unlocks).allSatisfy(item -> assertThat(item.day()).matches("\\d{4}-\\d{2}-\\d{2}"));
        assertThat(vo.unlockedDay()).isNotNull();
        assertThat(vo.timeline()).isSortedAccordingTo(
                java.util.Comparator.comparing(CoupleMemoryService.TimelineItemVO::day).reversed());
    }

    @Test
    void timelineOnlyCarriesEventsWithRealDates() {
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space()));
        when(bondDayMapper.findBySpace(SPACE)).thenReturn(consecutive(120, LocalDate.now()));
        List<CoupleQuestionAnswerPO> answers = List.of(
                CoupleQuestionAnswerPO.of(SPACE, "2026-09-30", 1, "今天最开心的一件事是什么？", "alice", "见到你"),
                CoupleQuestionAnswerPO.of(SPACE, "2026-09-30", 1, "今天最开心的一件事是什么？", "bob", "吃到面了"),
                // 只有 alice 答过的一天：不该作为「答完」上轴
                CoupleQuestionAnswerPO.of(SPACE, "2026-09-29", 2, "今天累不累？", "alice", "有点"));
        when(answerMapper.findBySpace(SPACE)).thenReturn(answers);
        CoupleWishPO done = CoupleWishPO.of(SPACE, "alice", "bob", "想一起看海", null);
        done.setId("w1");
        done.setStatus(CoupleWishPO.STATUS_FULFILLED);
        done.setFulfilledAt(LocalDate.of(2026, 9, 21)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        when(wishMapper.findBySpace(SPACE)).thenReturn(List.of(done));
        when(coupleService.intimacy(anyString())).thenReturn(new CoupleService.IntimacyVO(
                1400, 7, "相守一生", "💍", null, 100,
                new CoupleService.IntimacyBreakdown(120, 120, 10, 4, 2, 300)));

        CoupleMemoryService.MemoryVO vo = service.memory("alice");

        assertThat(vo.bothAnsweredDays()).isEqualTo(1);
        assertThat(vo.fulfilledWishes()).isEqualTo(1);
        assertThat(vo.timeline()).anySatisfy(item -> {
            assertThat(item.kind()).isEqualTo("wish");
            assertThat(item.day()).isEqualTo("2026-09-21");
        });
        assertThat(vo.timeline()).noneSatisfy(item -> {
            assertThat(item.kind()).isEqualTo("question");
            assertThat(item.day()).isEqualTo("2026-09-29");
        });
        assertThat(vo.timeline()).anyMatch(item -> "question".equals(item.kind())
                && "2026-09-30".equals(item.day()));
        assertThat(vo.timeline()).noneMatch(item -> "question".equals(item.kind())
                && "2026-09-29".equals(item.day()));
        assertThat(vo.timeline()).anySatisfy(item -> assertThat(item.kind()).isEqualTo("space"));
    }
}
