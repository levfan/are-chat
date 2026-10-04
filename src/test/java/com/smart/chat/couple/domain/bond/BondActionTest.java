package com.smart.chat.couple.domain.bond;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 一次贴贴：目录校验、推送话术、里程碑踩线与「谁在哪天发的」这两个计数口径。
 */
class BondActionTest {

    @Test
    void sentIsValidatesTheDirectoryAndStampsTheRow() {
        BondAction action = BondAction.sent("s1", "alice", "MISS");

        assertThat(action.id()).isNotBlank();
        assertThat(action.spaceId()).isEqualTo("s1");
        assertThat(action.username()).isEqualTo("alice");
        assertThat(action.kind()).isEqualTo("MISS");
        assertThat(action.created()).isNotNull();
    }

    @Test
    void unknownKindIsRejectedWithTheProductWording() {
        assertThatThrownBy(() -> BondAction.sent("s1", "alice", "BITE"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("不认识这个动作哦，换一个试试～");
        assertThat(BondAction.isValidKind("TICKLE")).isTrue();
        assertThat(BondAction.isValidKind(null)).isFalse();
    }

    @Test
    void eachKindHasItsOwnPushLine() {
        assertThat(text("POKE")).isEqualTo("TA 戳了戳你 👉 快回戳！");
        assertThat(text("HUG")).isEqualTo("TA 给了你一个大大的拥抱 🤗 快抱回去！");
        assertThat(text("KISS")).isEqualTo("TA 亲了你一口 💋 嘻嘻");
        assertThat(text("PAT")).isEqualTo("TA 捏了捏你的脸 🫳 好软");
        assertThat(text("NUZZLE")).isEqualTo("TA 蹭了蹭你 😚 好黏人");
        assertThat(text("TICKLE")).isEqualTo("TA 挠你痒痒 🤭 哈哈哈别跑！");
        assertThat(text("MISS")).isEqualTo("TA 说 TA 在想你 💌 现在立刻马上");
        assertThat(BondAction.restore("x", "s1", "alice", "RETIRED", 1L).pushText())
                .isEqualTo("TA 贴了贴你 💕");
    }

    @Test
    void onlyHugKissMissAreCelebratedAtRoundNumbers() {
        assertThat(BondAction.celebrates("HUG")).isTrue();
        assertThat(BondAction.celebrates("KISS")).isTrue();
        assertThat(BondAction.celebrates("MISS")).isTrue();
        assertThat(BondAction.celebrates("POKE")).as("戳一戳不配里程碑（现役口径）").isFalse();

        assertThat(BondAction.milestoneAt(1L)).isEqualTo(1L);
        assertThat(BondAction.milestoneAt(520L)).isEqualTo(520L);
        assertThat(BondAction.milestoneAt(1314L)).isEqualTo(1314L);
        assertThat(BondAction.milestoneAt(521L)).isNull();
        assertThat(BondAction.milestoneAt(0L)).isNull();
        assertThat(BondAction.milestoneDetail("HUG", 10L)).isEqualTo("第 10 次「抱抱」达成 🎉🤗 你们好甜！");
    }

    @Test
    void directoryOrderIsTheBoardOrder() {
        assertThat(BondAction.displayedKinds())
                .containsExactly("MISS", "HUG", "KISS", "POKE", "PAT", "NUZZLE", "TICKLE");
    }

    @Test
    void sentByAndOnDayUseTheServerDay() {
        long at = System.currentTimeMillis();
        BondAction action = BondAction.restore("a1", "s1", "alice", "HUG", at);
        String day = java.time.Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate().toString();

        assertThat(action.sentBy("alice")).isTrue();
        assertThat(action.sentBy("bob")).isFalse();
        assertThat(action.onDay(day)).isTrue();
        assertThat(action.onDay(LocalDate.now().minusDays(1).toString())).isFalse();
    }

    private String text(String kind) {
        return BondAction.sent("s1", "alice", kind).pushText();
    }
}
