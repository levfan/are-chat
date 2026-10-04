package com.smart.chat.messaging.infrastructure.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 联系人列表批量查询的**真库**测试：这三条 SQL（GROUP BY 聚合、JOIN 聚合、IN 回捞）是手写口径，
 * 单测里用 mock 一律照不出来，只有打到真表才知道语法与语义对不对——本轮 V49 补索引 + 改批量，
 * 若 SQL 写错，表现是联系人页整页 500，正是上几轮反复踩的那类坑。
 * <p>测试跑在 H2(MODE=MySQL) + Flyway 全量脚本上，顺带证明 V49 在空库一路升到最新可执行。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FriendListBatchQueryTest {

    private static final String ALICE = "pmq_alice";
    private static final String BOB = "pmq_bob";
    private static final String CAROL = "pmq_carol";

    @Autowired
    private FriendMapper friendMapper;

    @Autowired
    private PrivateMessageMapper messageMapper;

    @AfterEach
    void cleanUp() {
        for (String me : List.of(ALICE, BOB, CAROL)) {
            friendMapper.deleteAllByOwner(me);
            friendMapper.deleteAllByFriend(me);
            messageMapper.delete(new com.baomidou.mybatisplus.core.conditions.query
                    .LambdaQueryWrapper<PrivateMessage>()
                    .eq(PrivateMessage::getFromUser, me).or().eq(PrivateMessage::getToUser, me));
        }
    }

    private Friend friend(String owner, String peer, Long lastReadAt) {
        Friend f = Friend.of(owner, peer);
        f.setLastReadAt(lastReadAt);
        friendMapper.insert(f);
        return f;
    }

    private PrivateMessage msg(String from, String to, String content, long created) {
        PrivateMessage m = PrivateMessage.of(from, to, content, PrivateMessage.TYPE_TEXT);
        m.setCreated(created);
        m.setStatus(PrivateMessage.STATUS_SENT);
        messageMapper.insert(m);
        return m;
    }

    @Test
    void unreadCountsUseEachFriendsOwnLastReadThreshold() {
        friend(ALICE, BOB, 1_000L);      // 阈值低：bob 之后两条都算未读
        friend(ALICE, CAROL, 5_000L);    // 阈值高：carol 那条 3000 的已被读过

        msg(BOB, ALICE, "第一条", 2_000L);
        msg(BOB, ALICE, "第二条", 4_000L);
        msg(BOB, ALICE, "早就读过的一条", 500L);
        msg(CAROL, ALICE, "在阈值之前", 3_000L);
        // 我自己发出去的不该算进未读
        msg(ALICE, BOB, "我发的", 9_000L);

        Map<String, Long> counts = toCounts(friendMapper.selectUnreadCountsByPeer(ALICE));

        assertThat(counts.get(BOB)).isEqualTo(2L);
        assertThat(counts.get(CAROL)).isZero();
    }

    @Test
    void latestCreatedAndRowFetchCoverBothDirections() {
        friend(ALICE, BOB, 0L);
        friend(ALICE, CAROL, 0L);

        msg(BOB, ALICE, "bob 最新的一条", 1_500L);
        msg(BOB, ALICE, "更早", 700L);
        msg(ALICE, CAROL, "我发给 carol 的最新版", 2_600L);

        Map<String, Long> latest = messageMapper.findLatestCreatedPerPeer(ALICE);
        assertThat(latest.get(BOB)).isEqualTo(1_500L);
        assertThat(latest.get(CAROL)).isEqualTo(2_600L);

        List<PrivateMessage> rows = messageMapper.findMessagesAtCreated(
                ALICE, List.of(BOB, CAROL), latest.values());
        assertThat(rows).extracting(PrivateMessage::getContent)
                .containsExactlyInAnyOrder("bob 最新的一条", "我发给 carol 的最新版");
    }

    @Test
    void batchQueriesTolerateOwnerWithoutAnyHistory() {
        assertThat(messageMapper.findLatestCreatedPerPeer("pmq_nobody")).isEmpty();
        assertThat(friendMapper.selectUnreadCountsByPeer("pmq_nobody")).isEmpty();
        assertThat(messageMapper.findMessagesAtCreated("pmq_nobody", List.of(), List.of())).isEmpty();
    }

    private static Map<String, Long> toCounts(List<Map<String, Object>> rows) {
        Map<String, Long> out = new java.util.HashMap<>();
        for (Map<String, Object> row : rows) {
            out.put(String.valueOf(row.get("peer")), ((Number) row.get("unread")).longValue());
        }
        return out;
    }
}
