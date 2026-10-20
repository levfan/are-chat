package com.smart.chat.couple.domain.intimatelog;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntimateLogTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Test
    void bothKindsAreAcceptedAndNoteIsTrimmed() {
        IntimateLog edge = IntimateLog.record("s1", "2026-10-19", "edge", "  抱了很久  ", "alice", TODAY);
        IntimateLog sex = IntimateLog.record("s1", "2026-10-20", "SEX", null, "bob", TODAY);

        assertThat(edge.kind()).isEqualTo(IntimateLog.KIND_EDGE);
        assertThat(edge.note()).isEqualTo("抱了很久");
        assertThat(sex.kind()).isEqualTo(IntimateLog.KIND_SEX);
        assertThat(sex.note()).isNull();
        assertThat(edge.recorderUser()).isEqualTo("alice");
        assertThat(edge.created()).isNotNull();
    }

    @Test
    void blankOrMalformedDaysAreRejected() {
        assertThatThrownBy(() -> IntimateLog.record("s1", " ", "EDGE", null, "alice", TODAY))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("日子得选一个");
        assertThatThrownBy(() -> IntimateLog.record("s1", "2026/10/01", "EDGE", null, "alice", TODAY))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("yyyy-MM-dd");
    }

    @Test
    void futureDaysAndUnknownKindsAreRejected() {
        assertThatThrownBy(() -> IntimateLog.record("s1", "2026-10-21", "SEX", null, "alice", TODAY))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("还没到的日子");
        assertThatThrownBy(() -> IntimateLog.record("s1", "2026-10-20", "KISS", null, "alice", TODAY))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("选「边缘」或「生活」");
    }

    @Test
    void notesOverSixtyCharsAndMissingRecorderAreRejected() {
        assertThatThrownBy(() -> IntimateLog.record("s1", "2026-10-20", "SEX", "x".repeat(61), "alice", TODAY))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("最多 60 个字");
        assertThatThrownBy(() -> IntimateLog.record("s1", "2026-10-20", "SEX", null, "  ", TODAY))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("是谁记的");
    }

    @Test
    void restoreKeepsHistoricalRowsUntouched() {
        IntimateLog row = IntimateLog.restore("i1", "s1", "2026-10-19", "EDGE", "旧备注", "alice", 1L);

        assertThat(row.id()).isEqualTo("i1");
        assertThat(row.day()).isEqualTo("2026-10-19");
        assertThat(row.note()).isEqualTo("旧备注");
        assertThat(row.created()).isEqualTo(1L);
    }
}
