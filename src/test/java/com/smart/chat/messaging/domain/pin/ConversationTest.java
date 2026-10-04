package com.smart.chat.messaging.domain.pin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 会话规范化的唯一规则：字典序小的一方占 A 位，且区分大小写（沿用改造前的 String.compareTo）。 */
class ConversationTest {

    @Test
    void smallerNameTakesTheASlotWhicheverSideAsks() {
        Conversation fromAlice = Conversation.between("alice", "bob");
        Conversation fromBob = Conversation.between("bob", "alice");

        assertThat(fromAlice.userA()).isEqualTo("alice");
        assertThat(fromAlice.userB()).isEqualTo("bob");
        assertThat(fromBob).isEqualTo(fromAlice);
        assertThat(fromBob).hasSameHashCodeAs(fromAlice);
    }

    @Test
    void comparisonIsCaseSensitiveSoUpperGoesFirst() {
        Conversation mixed = Conversation.between("Alice", "bob");

        assertThat(mixed.userA()).isEqualTo("Alice");
        assertThat(mixed.userB()).isEqualTo("bob");
    }

    @Test
    void peerOfReadsTheOtherEnd() {
        Conversation conversation = Conversation.between("alice", "bob");

        assertThat(conversation.peerOf("alice")).isEqualTo("bob");
        assertThat(conversation.peerOf("bob")).isEqualTo("alice");
    }

    @Test
    void pinCarriesTheOperatorWhoSetItAndItsOwnId() {
        ConversationPin pin = ConversationPin.by("bob", "alice", "m1", 7L);

        assertThat(pin.id()).isNotBlank();
        assertThat(pin.userA()).isEqualTo("alice");
        assertThat(pin.userB()).isEqualTo("bob");
        assertThat(pin.msgId()).isEqualTo("m1");
        assertThat(pin.createdBy()).isEqualTo("bob");
        assertThat(pin.created()).isEqualTo(7L);
        assertThat(pin.conversation()).isEqualTo(Conversation.between("alice", "bob"));
    }

    @Test
    void restoringARowThatWasWrittenBackwardsStillReads() {
        // 历史行不校验：适配器必须能把任何 (user_a,user_b) 读出来，否则老数据整页 500
        ConversationPin legacy = ConversationPin.restore("p1", "zoe", "adam", "m9", "zoe", 1L);

        assertThat(legacy.userA()).isEqualTo("zoe");
        assertThat(legacy.conversation()).isEqualTo(Conversation.between("adam", "zoe"));
        assertThat(legacy.conversation().peerOf("zoe")).isEqualTo("adam");
    }
}
