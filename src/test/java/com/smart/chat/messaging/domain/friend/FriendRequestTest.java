package com.smart.chat.messaging.domain.friend;

import com.smart.chat.messaging.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 申请单的状态机：一人一票、只有接收方能处理、处理过就不能再动。 */
class FriendRequestTest {

    @Test
    void offeredRequestStartsPendingAndCarriesItsOwnId() {
        FriendRequest request = FriendRequest.offer("alice", "bob", 5L);

        assertThat(request.id()).isNotBlank();
        assertThat(request.status()).isEqualTo(FriendRequest.STATUS_PENDING);
        assertThat(request.created()).isEqualTo(5L);
        assertThat(request.updatedAt()).isNull();
        assertThat(request.pendingFlag()).isTrue();
    }

    @Test
    void cannotApplyToYourself() {
        assertThatThrownBy(() -> FriendRequest.offer("alice", "alice", 1L))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("不能添加自己为好友")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(400);
    }

    @Test
    void recipientAcceptsOnceAndStampsTheTime() {
        FriendRequest request = FriendRequest.offer("bob", "alice", 1L);

        request.acceptBy("alice", 8_000L);

        assertThat(request.status()).isEqualTo(FriendRequest.STATUS_ACCEPTED);
        assertThat(request.updatedAt()).isEqualTo(8_000L);
        assertThatThrownBy(() -> request.acceptBy("alice", 9_000L))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("该申请已经处理过了")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(409);
    }

    @Test
    void onlyTheRecipientCanDecide() {
        FriendRequest request = FriendRequest.offer("bob", "alice", 1L);

        assertThatThrownBy(() -> request.acceptBy("bob", 2L))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("只能处理发给自己的申请")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(403);
        assertThatThrownBy(() -> request.rejectBy("bob", 2L))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("只能处理发给自己的申请");
    }

    @Test
    void rejectionIsFinalToo() {
        FriendRequest request = FriendRequest.offer("bob", "alice", 1L);

        request.rejectBy("alice", 2L);

        assertThat(request.status()).isEqualTo(FriendRequest.STATUS_REJECTED);
        assertThatThrownBy(() -> request.rejectBy("alice", 3L))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("该申请已经处理过了");
    }

    @Test
    void blankMessageStaysUnsetAndLongOneIsRejected() {
        FriendRequest request = FriendRequest.offer("alice", "bob", 1L);

        request.attachMessage("   ");
        assertThat(request.message()).isNull();

        request.attachMessage("  我是carol  ");
        assertThat(request.message()).isEqualTo("我是carol");

        assertThatThrownBy(() -> request.attachMessage("x".repeat(101)))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("申请留言最长 100 个字");
    }
}
