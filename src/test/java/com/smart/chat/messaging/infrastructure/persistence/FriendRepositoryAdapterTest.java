package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.friend.Friend;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 好友边适配器的写回口径：聚合只维护 remark/tag/pinned/muted/blocked/last_read_at，
 * {@code last_seen_at} 由 {@code touchLastSeenOf} 的批量 UPDATE 负责。
 * <p>这条断言防的是「用聚合重建整行、把没纳管的列静默清空」——改一次备注就把对方的
 * 「x 分钟前在线」清成 null，编译与业务用例都照不出来。
 */
@ExtendWith(MockitoExtension.class)
class FriendRepositoryAdapterTest {

    @Mock
    private FriendMapper friendMapper;

    @InjectMocks
    private FriendRepositoryAdapter repository;

    @Test
    void updateKeepsColumnsTheAggregateDoesNotOwn() {
        FriendPO existing = FriendPO.of("alice", "bob");
        existing.setId("f1");
        existing.setLastSeenAt(777_000L);
        existing.setCreated(666L);
        existing.setTag("家人");
        when(friendMapper.selectById("f1")).thenReturn(existing);

        Friend edge = Friend.restore("f1", "alice", "bob", "老友", "家人", Boolean.TRUE, Boolean.FALSE,
                Integer.valueOf(1), 9_000L, null, null);
        repository.save(edge);

        ArgumentCaptor<FriendPO> captor = ArgumentCaptor.forClass(FriendPO.class);
        verify(friendMapper).updateById(captor.capture());
        FriendPO written = captor.getValue();
        assertThat(written.getLastSeenAt()).as("聚合没纳管的在线位不能被清空").isEqualTo(777_000L);
        assertThat(written.getCreated()).as("边的建立时间不属于改写范围").isEqualTo(666L);
        assertThat(written.getOwnerUsername()).isEqualTo("alice");
        assertThat(written.getFriendUsername()).isEqualTo("bob");
        assertThat(written.getRemark()).isEqualTo("老友");
        assertThat(written.getTag()).as("聚合带过来的 tag 原样写回（请求没改它）").isEqualTo("家人");
        assertThat(written.getPinned()).isTrue();
        assertThat(written.getBlocked()).isEqualTo(1);
        assertThat(written.getLastReadAt()).isEqualTo(9_000L);
        verify(friendMapper, never()).insert(any(FriendPO.class));
    }

    @Test
    void newEdgeIsInsertedWithBothSidesAndDefaults() {
        when(friendMapper.selectById("f2")).thenReturn(null);
        Friend edge = Friend.restore("f2", "alice", "bob", "", null, Boolean.FALSE, Boolean.FALSE,
                null, 0L, null, 123L);

        repository.save(edge);

        ArgumentCaptor<FriendPO> captor = ArgumentCaptor.forClass(FriendPO.class);
        verify(friendMapper).insert(captor.capture());
        FriendPO po = captor.getValue();
        assertThat(po.getId()).isEqualTo("f2");
        assertThat(po.getOwnerUsername()).isEqualTo("alice");
        assertThat(po.getFriendUsername()).isEqualTo("bob");
        assertThat(po.getPinned()).isFalse();
        assertThat(po.getLastReadAt()).isZero();
        assertThat(po.getCreated()).isEqualTo(123L);
        verify(friendMapper, never()).updateById(any(FriendPO.class));
    }

    @Test
    void storageRowMapsBackIntoTheEdgeIncludingTheReadOnlySeenStamp() {
        FriendPO po = FriendPO.of("alice", "bob");
        po.setId("f3");
        po.setTag("同事");
        po.setBlocked(1);
        po.setLastSeenAt(50L);
        when(friendMapper.findByOwnerAndFriend("alice", "bob")).thenReturn(Optional.of(po));

        Friend edge = repository.findByOwnerAndFriend("alice", "bob").orElseThrow();

        assertThat(edge.id()).isEqualTo("f3");
        assertThat(edge.tag()).isEqualTo("同事");
        assertThat(edge.blockedFlag()).isTrue();
        assertThat(edge.lastSeenAt()).isEqualTo(50L);
    }

    @Test
    void listByOwnerMapsEveryEdge() {
        FriendPO one = FriendPO.of("alice", "bob");
        FriendPO two = FriendPO.of("alice", "carol");
        when(friendMapper.findAllByOwner("alice")).thenReturn(List.of(one, two));

        assertThat(repository.findAllByOwner("alice")).extracting(Friend::friendUsername)
                .containsExactly("bob", "carol");
    }

    @Test
    void unreadRowsCollapseIntoPeerToCountProjection() {
        // Mapper 那趟 JOIN 返回 [{peer, unread}]，端口对外只给「对端 → 条数」
        when(friendMapper.selectUnreadCountsByPeer("alice")).thenReturn(List.of(
                Map.of("peer", "bob", "unread", 2L),
                Map.of("peer", "carol", "unread", 0)));

        Map<String, Long> counts = repository.unreadCountByPeer("alice");

        assertThat(counts).containsEntry("bob", 2L);
        assertThat(counts).containsEntry("carol", 0L);
    }

    @Test
    void pairDeletionAndCascadeGoStraightThrough() {
        repository.deletePair("alice", "bob");
        verify(friendMapper).deletePair("alice", "bob");

        repository.deleteAllEdgesOf("alice");
        verify(friendMapper).deleteAllByOwner("alice");
        verify(friendMapper).deleteAllByFriend("alice");

        repository.touchLastSeenOf("bob", 88L);
        verify(friendMapper).updateLastSeenByUsername("bob", 88L);
    }
}
