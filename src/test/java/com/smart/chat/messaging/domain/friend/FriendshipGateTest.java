package com.smart.chat.messaging.domain.friend;

import com.smart.chat.messaging.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 关系闸门的文案与状态码：这四条都是前端按 code 分支的对外契约，
 * 挪进领域之后必须仍然一字不差（改造前写在 Service 里）。
 */
class FriendshipGateTest {

    private static Friend edge(String owner, String peer, boolean blocked) {
        Friend edge = Friend.add(owner, peer, 1L);
        edge.changeBlocked(blocked);
        return edge;
    }

    @Test
    void noRelationshipMeansNoDirectMessage() {
        assertThatThrownBy(() -> FriendshipGate.requireMessagingAllowed(Optional.empty(), Optional.empty()))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("还不是好友，先加个好友吧")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(403);
    }

    @Test
    void blockingInEitherDirectionIsRejectedWithItsOwnMessage() {
        assertThatThrownBy(() -> FriendshipGate.requireMessagingAllowed(
                Optional.of(edge("alice", "bob", true)), Optional.of(edge("bob", "alice", false))))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("已拉黑对方，解除后才能发消息");

        assertThatThrownBy(() -> FriendshipGate.requireMessagingAllowed(
                Optional.of(edge("alice", "bob", false)), Optional.of(edge("bob", "alice", true))))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("对方已将你拉黑");
    }

    @Test
    void bothSidesUnblockedPasses() {
        assertThatCode(() -> FriendshipGate.requireMessagingAllowed(
                Optional.of(edge("alice", "bob", false)), Optional.of(edge("bob", "alice", false))))
                .doesNotThrowAnyException();
    }

    @Test
    void conversationOperationsOnlyNeedTheRelationship() {
        // 被对方拉黑的一方仍要能收尾自己的会话（置顶/清空/附件），所以这里不看拉黑位
        assertThatCode(() -> FriendshipGate.requireConversationUsable(Optional.of(edge("alice", "bob", true))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> FriendshipGate.requireConversationUsable(Optional.empty()))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("还不是好友，无法操作会话")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(403);
    }

    @Test
    void applyIsBlockedByThreeDifferentConflicts() {
        assertThatThrownBy(() -> FriendshipGate.requireApplyPossible(true, false, false))
                .hasMessage("你们已经是好友了").extracting(e -> ((RuleViolation) e).code()).isEqualTo(409);
        assertThatThrownBy(() -> FriendshipGate.requireApplyPossible(false, true, false))
                .hasMessage("好友申请已发送，等对方处理吧");
        assertThatThrownBy(() -> FriendshipGate.requireApplyPossible(false, false, true))
                .hasMessage("对方已经先向你发起了申请，去「好友申请」处理吧");
        assertThatCode(() -> FriendshipGate.requireApplyPossible(false, false, false)).doesNotThrowAnyException();
    }

    @Test
    void theTwoUnknownAccountMessagesStayDifferent() {
        // 加好友入口提示两种输法，私信入口只提手机号——合并文案就是改产品口径
        assertThatThrownBy(() -> FriendshipGate.requireKnownAccountToApply(false))
                .hasMessage("查无此人：对方还没用手机号注册，或手机号/用户名输错了");
        assertThatThrownBy(() -> FriendshipGate.requireKnownAccountToMail(false))
                .hasMessage("查无此人：对方还没有用手机号注册");
        assertThatThrownBy(() -> FriendshipGate.requireMessagingPartner("alice", "alice"))
                .hasMessage("不能给自己发私信");
    }

    @Test
    void profileCardIsFriendEyesOnly() {
        assertThatThrownBy(() -> FriendshipGate.requireProfileCardVisible(false))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("只有好友才能查看资料卡")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(403);
    }
}
