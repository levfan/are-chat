package com.smart.chat.messaging.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * 加好友联想的关系判定：批量取法与「每候选三趟」旧取法在**真库**上的等价性测试。
 * <p>
 * {@code FriendService.suggest} 把循环里的逐条查询改成按「我」三趟取全（ADR-0009 第 1 条）。
 * 这类改动 mock 单测照不出来——mock 只会跟着新签名走，真表上的谓词差异（单向好友边、
 * 非 PENDING 的申请、两个方向同时挂着待处理单）只有打到 H2 才现形。所以这里把两套算法
 * 在同一片真实数据上**逐候选比对**，顺带把四态语义本身钉死。
 * <p>跑在 H2(MODE=MySQL) + Flyway 全量脚本上。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FriendSuggestRelationQueryTest {

    private static final String ME = "sug_me";
    private static final String BOB = "sug_bob";
    private static final String CAROL = "sug_carol";
    private static final String DAVE = "sug_dave";
    private static final String ERIN = "sug_erin";
    private static final String FRANK = "sug_frank";
    private static final String GRACE = "sug_grace";

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private FriendRequestMapper requestMapper;

    @AfterEach
    void cleanUp() {
        for (String name : List.of(ME, BOB, CAROL, DAVE, ERIN, FRANK, GRACE)) {
            friendMapper.deleteAllByOwner(name);
            friendMapper.deleteAllByFriend(name);
            requestMapper.delete(new LambdaQueryWrapper<FriendRequestPO>()
                    .eq(FriendRequestPO::getFromUser, name).or().eq(FriendRequestPO::getToUser, name));
        }
    }

    private void edge(String owner, String peer) {
        friendMapper.insert(FriendPO.of(owner, peer));
    }

    private void request(String from, String to, String status) {
        FriendRequestPO row = FriendRequestPO.of(from, to);
        row.setStatus(status);
        requestMapper.insert(row);
    }

    /** 改造前：每个候选跑三条查询。 */
    private String perCandidateRelation(String candidate) {
        if (friendMapper.findByOwnerAndFriend(ME, candidate).isPresent()) {
            return "friend";
        }
        if (requestMapper.findPendingBetween(ME, candidate).isPresent()) {
            return "pending-out";
        }
        if (requestMapper.findPendingBetween(candidate, ME).isPresent()) {
            return "pending-in";
        }
        return "available";
    }

    /** 改造后：按「我」三趟取全，再在内存比对（与 FriendService.suggest 同一套集合口径）。 */
    private Map<String, String> batchRelations(List<String> candidates) {
        Set<String> friends = friendMapper.findAllByOwner(ME).stream()
                .map(FriendPO::getFriendUsername).collect(Collectors.toSet());
        Set<String> pendingOut = requestMapper.findOutgoing(ME).stream()
                .map(FriendRequestPO::getToUser).collect(Collectors.toSet());
        Set<String> pendingIn = requestMapper.findIncoming(ME).stream()
                .map(FriendRequestPO::getFromUser).collect(Collectors.toSet());
        Map<String, String> out = new HashMap<>();
        for (String candidate : candidates) {
            out.put(candidate, friends.contains(candidate) ? "friend"
                    : pendingOut.contains(candidate) ? "pending-out"
                    : pendingIn.contains(candidate) ? "pending-in" : "available");
        }
        return out;
    }

    @Test
    void batchLookupEqualsPerCandidateLookupOnRealData() {
        edge(ME, BOB);                                             // 我这边有边 → friend
        edge(CAROL, ME);                                           // 只有对方那边有边 → 不算好友
        request(ME, DAVE, FriendRequestPO.STATUS_PENDING);          // 我发出的待处理
        request(ERIN, ME, FriendRequestPO.STATUS_PENDING);          // 等我处理的
        request(ME, FRANK, FriendRequestPO.STATUS_ACCEPTED);        // 已同意的旧申请，不该再算 pending-out
        request(FRANK, ME, FriendRequestPO.STATUS_REJECTED);        // 已拒绝的，不该再算 pending-in
        request(GRACE, ME, FriendRequestPO.STATUS_PENDING);         // 双向同时挂着……
        request(ME, GRACE, FriendRequestPO.STATUS_PENDING);         // ……旧写法先命中 pending-out

        List<String> candidates = List.of(BOB, CAROL, DAVE, ERIN, FRANK, GRACE, "sug_陌生人");

        Map<String, String> batch = batchRelations(candidates);
        for (String candidate : candidates) {
            assertThat(batch.get(candidate))
                    .as("候选 %s 的批量取法必须与逐条查一致", candidate)
                    .isEqualTo(perCandidateRelation(candidate));
        }
    }

    @Test
    void fourRelationStatesArePinnedOnRealData() {
        edge(ME, BOB);
        request(ME, DAVE, FriendRequestPO.STATUS_PENDING);
        request(ERIN, ME, FriendRequestPO.STATUS_PENDING);
        request(ME, GRACE, FriendRequestPO.STATUS_PENDING);
        request(GRACE, ME, FriendRequestPO.STATUS_PENDING);

        assertThat(batchRelations(List.of(BOB, DAVE, ERIN, GRACE, FRANK)))
                .contains(entry(BOB, "friend"), entry(DAVE, "pending-out"), entry(ERIN, "pending-in"),
                        // 双向各挂一张待处理单时仍报「我发出的」——四态优先级与改造前同源
                        entry(GRACE, "pending-out"), entry(FRANK, "available"));
    }
}
