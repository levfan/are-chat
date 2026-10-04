package com.smart.chat.couple.domain.dine;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 饭票聚合的内容闸门与撞菜判定（纯领域，不起 Spring）。 */
class DineTicketTest {

    @Test
    void dishIsTrimmedAndReasonBlankBecomesEmpty() {
        DineTicket ticket = DineTicket.offer("s1", "2026-10-05", "alice", "  番茄牛腩  ", "   ");
        assertThat(ticket.dish()).isEqualTo("番茄牛腩");
        assertThat(ticket.reason()).isEmpty();
    }

    @Test
    void blankDishRejectedWithProductOriginalWording() {
        assertThatThrownBy(() -> DineTicket.offer("s1", "2026-10-05", "alice", "   ", "x"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("先写下今晚想吃什么呀 🍚");
        assertThatThrownBy(() -> DineTicket.offer("s1", "2026-10-05", "alice", null, "x"))
                .isInstanceOf(RuleViolation.class);
    }

    @Test
    void dishAndReasonHaveLengthCaps() {
        assertThatThrownBy(() -> DineTicket.offer("s1", "2026-10-05", "alice", "菜".repeat(31), "x"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("菜名 30 字以内哦");
        assertThatThrownBy(() -> DineTicket.offer("s1", "2026-10-05", "alice", "火锅", "由".repeat(81)))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("理由 80 字以内哦");
    }

    @Test
    void sameDishIsCaseInsensitiveAndNullSafe() {
        DineTicket a = DineTicket.offer("s1", "2026-10-05", "alice", "火锅", "");
        assertThat(a.sameDish(DineTicket.restore("t2", "s1", "2026-10-05", "bob", "HOTPOT", "", 0L))).isFalse();
        assertThat(a.sameDish(DineTicket.restore("t2", "s1", "2026-10-05", "bob", "火 锅", "", 0L))).isFalse();
        assertThat(a.sameDish(DineTicket.restore("t2", "s1", "2026-10-05", "bob", "火锅", "", 0L))).isTrue();
        assertThat(a.sameDish(null)).isFalse();
    }

    @Test
    void restoreDoesNotValidate() {
        DineTicket legacy = DineTicket.restore("t3", "s1", "2020-01-01", "alice", null, null, null);
        assertThat(legacy.id()).isEqualTo("t3");
        assertThat(legacy.created()).isZero();
        assertThat(legacy.dish()).isNull();
    }
}
