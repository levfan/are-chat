package com.smart.chat.couple.domain.period;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodRecordTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Test
    void startAcceptsPastAndTodayButRejectsFuture() {
        PeriodRecord today = PeriodRecord.start("s1", "2026-10-20", "alice", TODAY);
        assertThat(today.startDay()).isEqualTo("2026-10-20");
        assertThat(today.endDay()).isNull();
        assertThat(today.recorderUser()).isEqualTo("alice");

        assertThatThrownBy(() -> PeriodRecord.start("s1", "2026-10-21", "alice", TODAY))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("还没到的日子");
    }

    @Test
    void startRejectsBlankMalformedAndMissingRecorder() {
        assertThatThrownBy(() -> PeriodRecord.start("s1", "  ", "alice", TODAY))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("得选一个日子");
        assertThatThrownBy(() -> PeriodRecord.start("s1", "2026/10/01", "alice", TODAY))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("yyyy-MM-dd");
        assertThatThrownBy(() -> PeriodRecord.start("s1", "2026-10-01", " ", TODAY))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("谁记的");
    }

    @Test
    void markEndedIsOneWayAndCannotPrecedeStart() {
        PeriodRecord record = PeriodRecord.restore("p1", "s1", "2026-10-01", null, "alice", 1L);

        assertThatThrownBy(() -> record.markEnded(LocalDate.of(2026, 9, 30)))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("不能早于开始日");

        record.markEnded(TODAY);
        assertThat(record.endDay()).isEqualTo("2026-10-20");
        assertThatThrownBy(() -> record.markEnded(TODAY))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("已经记过结束日");
    }

    /** 日历按天铺经期格子的判据：记了结束日就含首含尾，没记结束日只到今天。 */
    @Test
    void coversIncludesBothEndsAndStopsAtTodayWhenStillOpen() {
        PeriodRecord ended = PeriodRecord.restore("p1", "s1", "2026-10-10", "2026-10-13", "alice", 1L);
        assertThat(ended.covers("2026-10-09", "2026-10-20")).isFalse();
        assertThat(ended.covers("2026-10-10", "2026-10-20")).isTrue();
        assertThat(ended.covers("2026-10-13", "2026-10-20")).isTrue();
        assertThat(ended.covers("2026-10-14", "2026-10-20")).isFalse();
        assertThat(ended.startsOn("2026-10-10")).isTrue();
        assertThat(ended.endsOn("2026-10-13")).isTrue();
        assertThat(ended.endsOn("2026-10-12")).isFalse();

        PeriodRecord open = PeriodRecord.restore("p2", "s1", "2026-10-18", null, "bob", 2L);
        assertThat(open.covers("2026-10-19", "2026-10-20")).isTrue();
        assertThat(open.covers("2026-10-20", "2026-10-20")).isTrue();
        assertThat(open.covers("2026-10-21", "2026-10-20")).isFalse();
        assertThat(open.endsOn("2026-10-20")).isFalse();
    }

    /** 历史脏行不该让日历 500：非法日期一律算「没覆盖」，结束日早于开始日按开始日那天收。 */
    @Test
    void dirtyRowsAreTreatedAsCoveringNothingOrJustTheirStart() {
        PeriodRecord dirty = PeriodRecord.restore("p3", "s1", "2026/10/01", "2026-10-05", "alice", 1L);
        assertThat(dirty.covers("2026-10-03", "2026-10-20")).isFalse();

        PeriodRecord backwards = PeriodRecord.restore("p4", "s1", "2026-10-06", "2026-10-02", "bob", 2L);
        assertThat(backwards.covers("2026-10-06", "2026-10-20")).isTrue();
        assertThat(backwards.covers("2026-10-05", "2026-10-20")).isFalse();
    }
}
