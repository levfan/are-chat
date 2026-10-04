package com.smart.chat.messaging.domain.friend;

import com.smart.chat.messaging.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 好友边自己的规则：归属闸门、私设字段长度、拉黑位与建档默认值。 */
class FriendTest {

    @Test
    void newEdgeStartsUnpinnedUnmutedWithZeroReadCursor() {
        Friend edge = Friend.add("alice", "bob", 123L);

        assertThat(edge.id()).isNotBlank();
        assertThat(edge.ownerUsername()).isEqualTo("alice");
        assertThat(edge.friendUsername()).isEqualTo("bob");
        assertThat(edge.remark()).isEmpty();
        assertThat(edge.tag()).isNull();
        assertThat(edge.pinnedFlag()).isFalse();
        assertThat(edge.mutedFlag()).isFalse();
        assertThat(edge.blockedFlag()).isFalse();
        assertThat(edge.lastReadAt()).isZero();
        assertThat(edge.created()).isEqualTo(123L);
    }

    @Test
    void onlyTheOwnerCanTouchItsOwnEdge() {
        Friend edge = Friend.add("bob", "alice", 1L);

        assertThatThrownBy(() -> edge.requireOwnedBy("alice"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("好友不存在")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(404);
    }

    @Test
    void remarkAndTagHaveTheirOwnCeilings() {
        Friend edge = Friend.add("alice", "bob", 1L);

        edge.changeRemark("  老友  ");
        edge.changeTag("  同事  ");
        assertThat(edge.remark()).isEqualTo("老友");
        assertThat(edge.tag()).isEqualTo("同事");

        assertThatThrownBy(() -> edge.changeRemark("x".repeat(33)))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("备注最长 32 个字");
        assertThatThrownBy(() -> edge.changeTag("x".repeat(17)))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("分组标签最长 16 个字");
    }

    @Test
    void blockingWritesTheColumnValueAndTheFlag() {
        Friend edge = Friend.add("alice", "bob", 1L);

        edge.changeBlocked(true);
        assertThat(edge.blocked()).isEqualTo(1);
        assertThat(edge.blockedFlag()).isTrue();

        edge.changeBlocked(false);
        assertThat(edge.blocked()).isZero();
        assertThat(edge.blockedFlag()).isFalse();
    }

    @Test
    void readCursorMovesForwardWhenTheConversationIsOpened() {
        Friend edge = Friend.add("alice", "bob", 1L);

        edge.markReadAt(9_000L);

        assertThat(edge.lastReadAt()).isEqualTo(9_000L);
    }

    @Test
    void restoringKeepsLegacyNullsSoHistoricalRowsStillRead() {
        Friend edge = Friend.restore("f1", "alice", "bob", null, null, null, null, null, null, 777L, null);

        assertThat(edge.remark()).isNull();
        assertThat(edge.pinnedFlag()).isFalse();
        assertThat(edge.blockedFlag()).isFalse();
        assertThat(edge.lastSeenAt()).isEqualTo(777L);
    }
}
