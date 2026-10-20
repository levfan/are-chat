package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.intimatelog.IntimateLog;
import com.smart.chat.couple.domain.intimatelog.IntimateLogRepository;
import com.smart.chat.couple.domain.period.PeriodRecord;
import com.smart.chat.couple.domain.period.PeriodRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
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

@ExtendWith(MockitoExtension.class)
class CoupleCalendarServiceTest {

    private static final String SPACE = "s1";
    private static final LocalDate TODAY = LocalDate.now();

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private PeriodRepository periodRepository;
    @Mock
    private IntimateLogRepository logRepository;

    @InjectMocks
    private CoupleCalendarService service;

    private void stubSpace() {
        CoupleSpace space = CoupleSpace.restore(SPACE, "alice", "bob", CoupleSpace.STATUS_ACTIVE,
                System.currentTimeMillis(), null, null, null, null, null, null);
        lenient().when(spaceRepository.findActiveByMember(anyString())).thenReturn(Optional.of(space));
    }

    private void periods(PeriodRecord... rows) {
        lenient().when(periodRepository.findBySpace(SPACE)).thenReturn(List.of(rows));
    }

    /** 假仓储的区间口径与适配器一致：左含右不含、日期正序、同一天按记录时刻正序。 */
    private void intimates(IntimateLog... rows) {
        lenient().when(logRepository.findInRange(anyString(), any(), any())).thenAnswer(inv -> {
            LocalDate from = inv.getArgument(1);
            LocalDate to = inv.getArgument(2);
            return List.of(rows).stream()
                    .filter(row -> {
                        LocalDate day = LocalDate.parse(row.day());
                        return !day.isBefore(from) && day.isBefore(to);
                    })
                    .sorted(Comparator.comparing(IntimateLog::day).thenComparing(IntimateLog::created))
                    .toList();
        });
    }

    @Test
    void spansCoverEveryDayOfTheViewedMonthIncludingBothEnds() {
        stubSpace();
        intimates();
        periods(
                PeriodRecord.restore("pA", SPACE, "2026-08-30", "2026-09-02", "alice", 1L),
                PeriodRecord.restore("pB", SPACE, "2026-09-10", "2026-09-14", "bob", 2L));

        CoupleCalendarService.CoupleCalendarVO calendar = service.month("alice", "2026-09");

        assertThat(calendar.month()).isEqualTo("2026-09");
        assertThat(calendar.periodDays()).containsExactly("2026-09-01", "2026-09-02",
                "2026-09-10", "2026-09-11", "2026-09-12", "2026-09-13", "2026-09-14");
        // 开始日落在上个月，这个月只该看到月内那个开始日
        assertThat(calendar.periodStartDays()).containsExactly("2026-09-10");
        assertThat(calendar.periodEndDays()).containsExactly("2026-09-02", "2026-09-14");
    }

    @Test
    void openPeriodStopsAtTodayAndStillShowsItsStartDay() {
        stubSpace();
        intimates();
        periods(PeriodRecord.restore("pOpen", SPACE, TODAY.toString(), null, "alice", 1L));

        CoupleCalendarService.CoupleCalendarVO calendar = service.month("bob", YearMonth.from(TODAY).toString());

        assertThat(calendar.periodDays()).containsExactly(TODAY.toString());
        assertThat(calendar.periodStartDays()).containsExactly(TODAY.toString());
        assertThat(calendar.periodEndDays()).isEmpty();
    }

    /** 日历不吃看板那 30 条上限：整月一条不落，字段够铺图标与展开当天详情。 */
    @Test
    void intimateMarksComeFromTheWholeMonthInAscendingOrder() {
        stubSpace();
        periods();
        intimates(
                IntimateLog.restore("i_out_1", SPACE, "2026-08-31", "SEX", null, "alice", 1L),
                IntimateLog.restore("i_late", SPACE, "2026-09-03", "EDGE", "后记的", "bob", 3L),
                IntimateLog.restore("i_early", SPACE, "2026-09-03", "SEX", null, "alice", 1L),
                IntimateLog.restore("i_out_2", SPACE, "2026-10-01", "SEX", null, "bob", 9L));

        CoupleCalendarService.CoupleCalendarVO calendar = service.month("alice", "2026-09");

        assertThat(calendar.intimate()).extracting(CoupleCalendarService.CalendarIntimateVO::day)
                .containsExactly("2026-09-03", "2026-09-03");
        assertThat(calendar.intimate().get(0).kind()).isEqualTo(IntimateLog.KIND_SEX);
        assertThat(calendar.intimate().get(1).kind()).isEqualTo(IntimateLog.KIND_EDGE);
        assertThat(calendar.intimate().get(1).note()).isEqualTo("后记的");
        assertThat(calendar.intimate().get(1).recorderUser()).isEqualTo("bob");
    }

    /** 预计下次经期照经期域的推算口径给，日历只负责标那一天。 */
    @Test
    void predictedNextDayFollowsTheCycleMath() {
        stubSpace();
        intimates();
        periods(
                PeriodRecord.restore("p1", SPACE, "2026-08-01", "2026-08-05", "alice", 1L),
                PeriodRecord.restore("p2", SPACE, "2026-08-29", "2026-09-01", "alice", 2L));

        assertThat(service.month("alice", "2026-09").predictedNextDay()).isEqualTo("2026-09-26");

        periods();
        assertThat(service.month("alice", "2026-09").predictedNextDay()).isNull();
    }

    @Test
    void malformedMonthIsFourHundredAndNoSpaceIsTheSameFourOhFourAsOtherCards() {
        stubSpace();
        intimates();
        periods();

        assertThatThrownBy(() -> service.month("alice", "202610"))
                .isInstanceOf(BusinessException.class).hasMessage("月份得是 yyyy-MM 这样的");
        assertThatThrownBy(() -> service.month("alice", "  "))
                .isInstanceOf(BusinessException.class).hasMessage("要看哪个月呀？给我 yyyy-MM 这样的");

        when(spaceRepository.findActiveByMember("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.month("solo", "2026-09"))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
        verify(periodRepository, never()).findBySpace(anyString());
    }
}
