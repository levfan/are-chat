package com.smart.chat.messaging.domain.reaction;

import com.smart.chat.messaging.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** append-only 流水：回应带表情白名单，不编造额外行为。 */
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
}
