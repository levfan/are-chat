package com.smart.chat.couple.domain.intimatelog;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntimateLogsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    private static IntimateLog log(String id, String day, String kind, long created) {
        return IntimateLog.restore(id, "s1", day, kind, null, "alice", created);
    }

    @Test
    void emptyHistoryGivesNullLastAndZeroCounts() {
        IntimateLogs stats = IntimateLogs.of(List.of(), TODAY);

        assertThat(stats.last()).isNull();
        assertThat(stats.monthCount()).isZero();
        assertThat(stats.totalCount()).isZero();
    }

    @Test
    void monthAndTotalCountsAreSplitByKind() {
        IntimateLogs stats = IntimateLogs.of(List.of(
                log("i1", "2026-10-19", IntimateLog.KIND_EDGE, 1L),
                log("i2", "2026-10-18", IntimateLog.KIND_SEX, 2L),
                log("i3", "2026-09-30", IntimateLog.KIND_SEX, 3L)), TODAY);

        assertThat(stats.monthCount()).isEqualTo(2);
        assertThat(stats.monthEdgeCount()).isEqualTo(1);
        assertThat(stats.monthSexCount()).isEqualTo(1);
        assertThat(stats.totalCount()).isEqualTo(3);
        assertThat(stats.last().id()).isEqualTo("i1");
    }

    @Test
    void latestOfTheSameDayIsTheOneWrittenLast() {
        IntimateLogs stats = IntimateLogs.of(List.of(
                log("old", "2026-10-20", IntimateLog.KIND_EDGE, 10L),
                log("new", "2026-10-20", IntimateLog.KIND_SEX, 20L)), TODAY);

        assertThat(stats.last().id()).isEqualTo("new");
        assertThat(stats.last().kind()).isEqualTo(IntimateLog.KIND_SEX);
        assertThat(stats.monthCount()).isEqualTo(2);
    }

    @Test
    void rowsWithBrokenDaysAreIgnoredInsteadOfFailingTheCard() {
        IntimateLogs stats = IntimateLogs.of(List.of(
                log("broken", "2026-13-40", IntimateLog.KIND_SEX, 1L),
                log("ok", "2026-10-01", IntimateLog.KIND_SEX, 2L)), TODAY);

        assertThat(stats.totalCount()).isEqualTo(1);
        assertThat(stats.last().id()).isEqualTo("ok");
    }
}
