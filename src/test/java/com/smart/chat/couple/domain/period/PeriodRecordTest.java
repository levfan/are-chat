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
}
