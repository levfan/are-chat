package com.smart.chat.couple.domain.mood;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 心情回应聚合：四种回应白名单、不能回应未来、重复提交视为修改（只盖修改时刻）。
 * 心情日期口径（留空即今天／格式写错就拒）也一起锁住。
 */
class MoodReactionTest {

    @Test
    void giveStampsCreationOnly() {
        MoodReaction reaction = MoodReaction.give("s1", "2026-10-04", "alice", "HUG");

        assertThat(reaction.id()).isNotBlank();
        assertThat(reaction.reaction()).isEqualTo("HUG");
        assertThat(reaction.moodDay()).isEqualTo("2026-10-04");
        assertThat(reaction.respondedBy("alice")).isTrue();
        assertThat(reaction.respondedBy("bob")).isFalse();
        assertThat(reaction.created()).isNotNull();
        assertThat(reaction.updatedAt()).isNull();
    }

    @Test
    void giveAndReviseBothRejectOtherKeys() {
        assertThatThrownBy(() -> MoodReaction.give("s1", "2026-10-04", "alice", "SIDE_HUG"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("回应只能是抱抱/亲亲/加油/摸摸头哦");
        MoodReaction reaction = MoodReaction.give("s1", "2026-10-04", "alice", "KISS");
        assertThatThrownBy(() -> reaction.revise(null))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("回应只能是抱抱/亲亲/加油/摸摸头哦");
        assertThat(reaction.reaction()).as("被拒的那次不改写回应").isEqualTo("KISS");
    }

    @Test
    void futureDaysAreNotThereYet() {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        assertThatThrownBy(() -> MoodReaction.requireNotFuture(tomorrow))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("不能回应未来的心情哦");
        assertThatThrownBy(() -> MoodReaction.give("s1", tomorrow, "alice", "HUG"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("不能回应未来的心情哦");
        assertThat(MoodReaction.requireNotFuture("2020-01-01")).isEqualTo("2020-01-01");
    }

    @Test
    void reviseMarksTheUpdateTime() {
        MoodReaction reaction = MoodReaction.restore("r1", "s1", "2026-10-04", "alice", "HUG", 111L, 111L);

        reaction.revise("CHEER");

        assertThat(reaction.reaction()).isEqualTo("CHEER");
        assertThat(reaction.created()).isEqualTo(111L);
        assertThat(reaction.updatedAt()).isNotEqualTo(111L);
    }

    @Test
    void labelsComeFromTheFourKeyDirectory() {
        assertThat(MoodReaction.labelOf("HUG")).isEqualTo("抱抱");
        assertThat(MoodReaction.emojiOf("HUG")).isEqualTo("🤗");
        assertThat(MoodReaction.labelOf("CHEER")).isEqualTo("加油");
        assertThat(MoodReaction.emojiOf("CHEER")).isEqualTo("💪");
        assertThat(MoodReaction.labelOf("PAT")).isEqualTo("摸摸头");
        assertThat(MoodReaction.emojiOf("PAT")).isEqualTo("🫶");
        assertThat(MoodReaction.labelOf("KISS")).isEqualTo("亲亲");
        assertThat(MoodReaction.emojiOf("KISS")).isEqualTo("💋");
        assertThat(MoodReaction.labelOf("RETIRED")).isEqualTo("回应");
        assertThat(MoodReaction.emojiOf(null)).isEqualTo("💕");
    }

    @Test
    void dayOrTodayAcceptsBlankAndRefusesGarbage() {
        assertThat(Mood.dayOrToday(null)).isEqualTo(LocalDate.now().toString());
        assertThat(Mood.dayOrToday("   ")).isEqualTo(LocalDate.now().toString());
        assertThat(Mood.dayOrToday(" 2026-10-04 ")).isEqualTo("2026-10-04");
        assertThatThrownBy(() -> Mood.dayOrToday("2026-13-45"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("日期格式应为 yyyy-MM-dd");
    }
}
