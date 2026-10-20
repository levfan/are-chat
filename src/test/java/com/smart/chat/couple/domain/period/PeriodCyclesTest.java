package com.smart.chat.couple.domain.period;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodCyclesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private static PeriodRecord record(String day) {
        return PeriodRecord.restore("p-" + day, "s1", day, null, "alice", 0L);
    }

    @Test
    void noRecordsMeansDefaultCycleAndNoPrediction() {
        PeriodCycles cycles = PeriodCycles.of(List.of(), TODAY);
        assertThat(cycles.averageCycleDays()).isEqualTo(28);
        assertThat(cycles.sampleCount()).isZero();
        assertThat(cycles.predictedNextDay()).isNull();
        assertThat(cycles.daysUntilNext()).isNull();
    }

    @Test
    void oneRecordUsesTheDefaultCycle() {
        PeriodCycles cycles = PeriodCycles.of(List.of(record("2026-10-01")), TODAY);
        assertThat(cycles.averageCycleDays()).isEqualTo(28);
        assertThat(cycles.sampleCount()).isZero();
        assertThat(cycles.predictedNextDay()).isEqualTo(LocalDate.of(2026, 10, 29));
        assertThat(cycles.daysUntilNext()).isEqualTo(9);
    }

    @Test
    void averageUsesValidIntervalsAndIgnoresOutliers() {
        // 相邻间隔：28、28、30、32、13（最后一段多半是漏记造成的假间隔，忽略）→ 平均 29.5 取整 30
        PeriodCycles cycles = PeriodCycles.of(List.of(
                record("2026-10-01"), record("2026-09-18"), record("2026-08-17"), record("2026-07-18"),
                record("2026-06-20"), record("2026-05-23")), TODAY);
        assertThat(cycles.sampleCount()).isEqualTo(4);
        assertThat(cycles.averageCycleDays()).isEqualTo(30);
        assertThat(cycles.predictedNextDay()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(cycles.daysUntilNext()).isEqualTo(11);
    }

    @Test
    void allOutlierIntervalsFallBackToDefault() {
        PeriodCycles cycles = PeriodCycles.of(List.of(record("2026-10-01"), record("2026-01-01")), TODAY);
        assertThat(cycles.sampleCount()).isZero();
        assertThat(cycles.averageCycleDays()).isEqualTo(28);
    }

    @Test
    void invalidDatesFromDirtyRowsAreIgnoredInsteadOfThrowing() {
        PeriodCycles cycles = PeriodCycles.of(List.of(
                PeriodRecord.restore("bad", "s1", "not-a-day", null, "alice", 0L),
                record("2026-10-01")), TODAY);
        assertThat(cycles.predictedNextDay()).isEqualTo(LocalDate.of(2026, 10, 29));
    }
}
