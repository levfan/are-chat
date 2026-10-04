package com.smart.chat.messaging.domain.reaction;

import com.smart.chat.messaging.domain.RuleViolation;
import com.smart.chat.messaging.domain.star.MessageStar;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 两条 append-only 流水：回应带表情白名单，收藏只记「谁收了哪条」，都不编造额外行为。 */
class MessageReactionTest {

    @Test
    void whitelistIsTheThirtySixEmojisTheFrontendKnows() {
        assertThat(MessageReaction.SUPPORTED).hasSize(36);
        assertThat(MessageReaction.SUPPORTED).contains("❤️", "⛵", "👍");

        assertThatThrownBy(() -> MessageReaction.requireSupported("🐱"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("不支持的表情回应")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(400);
        assertThatThrownBy(() -> MessageReaction.requireSupported(null))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("不支持的表情回应");
    }

    @Test
    void addingCarriesItsOwnIdAndTimestamp() {
        MessageReaction reaction = MessageReaction.add("m1", "alice", "👍", 9L);

        assertThat(reaction.id()).isNotBlank();
        assertThat(reaction.msgId()).isEqualTo("m1");
        assertThat(reaction.username()).isEqualTo("alice");
        assertThat(reaction.emoji()).isEqualTo("👍");
        assertThat(reaction.created()).isEqualTo(9L);
    }

    @Test
    void restoringKeepsEmojisThatTheWhitelistLaterDropped() {
        MessageReaction legacy = MessageReaction.restore("r1", "m1", "alice", "🐱", 1L);

        assertThat(legacy.emoji()).isEqualTo("🐱");
    }

    @Test
    void starIsJustWhoBookmarkedWhichMessage() {
        MessageStar star = MessageStar.add("alice", "m1", 3L);

        assertThat(star.id()).isNotBlank();
        assertThat(star.username()).isEqualTo("alice");
        assertThat(star.msgId()).isEqualTo("m1");
        assertThat(star.created()).isEqualTo(3L);

        assertThat(MessageStar.restore("s1", "alice", "m1", 3L).msgId()).isEqualTo("m1");
    }
}
