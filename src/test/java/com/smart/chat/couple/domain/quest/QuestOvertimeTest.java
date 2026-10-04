package com.smart.chat.couple.domain.quest;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 加班预报聚合的小时钳制、说明上限与「灯只能对方留」归属闸门（纯领域，不起 Spring）。 */
class QuestOvertimeTest {

    @Test
    void forecastClampsHourAndTrimsNote() {
        assertThat(QuestOvertime.forecast("s1", "2026-10-05", "alice", 26, "赶年结").untilHour()).isEqualTo(23);
        assertThat(QuestOvertime.forecast("s1", "2026-10-05", "alice", 2, "赶年结").untilHour()).isEqualTo(13);
        assertThat(QuestOvertime.forecast("s1", "2026-10-05", "alice", null, "  加班  ").untilHour())
                .isEqualTo(QuestOvertime.HOUR_DEFAULT);
        QuestOvertime fresh = QuestOvertime.forecast("s1", "2026-10-05", "alice", 20, "  加班  ");
        assertThat(fresh.note()).isEqualTo("加班");
        assertThat(fresh.lamp()).isEmpty();
        assertThat(fresh.lampBy()).isNull();
    }

    @Test
    void noteOverCapRejectedWithOriginalWording() {
        assertThatThrownBy(() -> QuestOvertime.forecast("s1", "2026-10-05", "alice", 20, "说".repeat(41)))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("一句说明最多 40 字");
    }

    @Test
    void reforecastKeepsExistingLampAndOwnership() {
        QuestOvertime base = QuestOvertime.restore("q1", "s1", "2026-10-05", "alice", 22, "加班", "灯给你留着", "bob", 111L, 111L);
        QuestOvertime rewritten = base.reforecast(26, "改到更晚");
        assertThat(rewritten.untilHour()).isEqualTo(23);
        assertThat(rewritten.note()).isEqualTo("改到更晚");
        assertThat(rewritten.lamp()).isEqualTo("灯给你留着");
        assertThat(rewritten.lampBy()).isEqualTo("bob");
        assertThat(rewritten.id()).isEqualTo("q1");
        assertThat(rewritten.created()).isEqualTo(111L);
    }

    @Test
    void onlyCounterpartCanLeaveLamp() {
        QuestOvertime base = QuestOvertime.forecast("s1", "2026-10-05", "alice", 22, "加班");
        assertThatThrownBy(() -> base.leaveLampBy("alice", "我自己留"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("灯是给加班的人留的，自己留不算 💡");
        assertThatThrownBy(() -> base.leaveLampBy("bob", "  "))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("灯下想留的那句话写一句");
        assertThatThrownBy(() -> base.leaveLampBy("bob", "灯".repeat(61)))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("灯卡最多 60 字");

        QuestOvertime lit = base.leaveLampBy("bob", "到家灯给你留着");
        assertThat(lit.lamp()).isEqualTo("到家灯给你留着");
        assertThat(lit.lampBy()).isEqualTo("bob");
        assertThat(lit.untilHour()).isEqualTo(22);
    }

    @Test
    void restoreDoesNotValidateAndDefaultsNullHour() {
        QuestOvertime legacy = QuestOvertime.restore("q9", "s1", "2020-01-01", "alice", null, null, null, null, null, null);
        assertThat(legacy.untilHour()).isEqualTo(QuestOvertime.HOUR_DEFAULT);
        assertThat(legacy.note()).isNull();
        assertThat(legacy.created()).isZero();
    }
}
