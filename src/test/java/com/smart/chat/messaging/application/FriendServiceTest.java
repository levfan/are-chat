package com.smart.chat.messaging.application;

import com.smart.chat.messaging.domain.conversation.PrivateMessage;
import com.smart.chat.messaging.domain.conversation.PrivateMessageRepository;
import com.smart.chat.messaging.domain.friend.Friend;
import com.smart.chat.messaging.domain.friend.FriendRepository;
import com.smart.chat.messaging.domain.friend.FriendRequest;
import com.smart.chat.messaging.domain.friend.FriendRequestRepository;
import com.smart.chat.messaging.domain.profile.UserProfile;
import com.smart.chat.messaging.domain.profile.UserProfileRepository;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.sharedkernel.web.BusinessException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * 好友用例：断言的期望值与改造前一字不差，只是 mock 从 Mapper 换成仓储端口、
 * fixture 从 PO 的 getter 换成领域类型的记录式访问器。
 */
@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private FriendRequestRepository requestRepository;

    @Mock
    private PrivateMessageRepository messageRepository;

    @Mock
    private UserProfileRepository profileRepository;

    @Mock
    private ImPushService push;

    @Mock
    private AccountDirectory accounts;

    @InjectMocks
    private FriendService service;

    /** 合法用户目录：默认「输入规范化 + 用户存在」，各用例可按需覆盖 */
    @org.junit.jupiter.api.BeforeEach
    void stubUserDirectory() {
        lenient().when(accounts.normalizeUsername(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class).trim().toLowerCase());
        lenient().when(accounts.exists(anyString())).thenReturn(true);
        lenient().when(accounts.search(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(java.util.List.of());
    }

    private static Friend edge(String owner, String peer) {
        return Friend.add(owner, peer, 1L);
    }

    private static FriendRequest offer(String from, String to) {
        return FriendRequest.offer(from, to, 1L);
    }

    @Test
    void applySuccessPushesRequestToTarget() {
        when(requestRepository.findPendingBetween("alice", "bob")).thenReturn(Optional.empty());
        when(requestRepository.findPendingBetween("bob", "alice")).thenReturn(Optional.empty());

        FriendService.FriendRequestVO vo = service.apply("alice", "bob", "我是carol，加个好友");

        assertThat(vo.status()).isEqualTo(FriendRequest.STATUS_PENDING);
        assertThat(vo.fromUser()).isEqualTo("alice");
        assertThat(vo.message()).isEqualTo("我是carol，加个好友");
        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(requestRepository).save(captor.capture());
        assertThat(captor.getValue().toUser()).isEqualTo("bob");
        assertThat(captor.getValue().message()).isEqualTo("我是carol，加个好友");
        verify(push).pushFriendEvent(eq("friend-request"), eq("bob"), anyString());
    }

    @Test
    void applyValidatesMessageLength() {
        when(requestRepository.findPendingBetween("alice", "bob")).thenReturn(Optional.empty());
        when(requestRepository.findPendingBetween("bob", "alice")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.apply("alice", "bob", "x".repeat(101)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100");

        // 空白留言视为未填
        FriendService.FriendRequestVO vo = service.apply("alice", "bob", "   ");
        assertThat(vo.message()).isEmpty();
    }

    @Test
    void applyRejectsSelfUnknownAndDuplicates() {
        assertThatThrownBy(() -> service.apply("alice", "alice", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能添加自己");
        // 未注册的账号不是合法用户
        when(accounts.exists("路人甲")).thenReturn(false);
        assertThatThrownBy(() -> service.apply("alice", "路人甲", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("查无此人");

        when(friendRepository.findByOwnerAndFriend("alice", "bob"))
                .thenReturn(Optional.of(edge("alice", "bob")));
        assertThatThrownBy(() -> service.apply("alice", "bob", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经是好友");

        when(friendRepository.findByOwnerAndFriend("alice", "bob")).thenReturn(Optional.empty());
        when(requestRepository.findPendingBetween("alice", "bob"))
                .thenReturn(Optional.of(offer("alice", "bob")));
        assertThatThrownBy(() -> service.apply("alice", "bob", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("等对方处理");

        when(requestRepository.findPendingBetween("alice", "bob")).thenReturn(Optional.empty());
        when(requestRepository.findPendingBetween("bob", "alice"))
                .thenReturn(Optional.of(offer("bob", "alice")));
        assertThatThrownBy(() -> service.apply("alice", "bob", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("先向你发起");
    }

    @Test
    void acceptCreatesBothFriendRowsAndSystemMessage() {
        FriendRequest request = offer("bob", "alice");
        when(requestRepository.find(request.id())).thenReturn(Optional.of(request));
        when(friendRepository.findByOwnerAndFriend("bob", "alice")).thenReturn(Optional.empty());
        when(friendRepository.findByOwnerAndFriend("alice", "bob")).thenReturn(Optional.empty());

        service.accept("alice", request.id());

        assertThat(request.status()).isEqualTo(FriendRequest.STATUS_ACCEPTED);
        verify(requestRepository).save(request);
        verify(friendRepository, times(2)).save(any(Friend.class));

        ArgumentCaptor<PrivateMessage> msgCaptor = ArgumentCaptor.forClass(PrivateMessage.class);
        verify(messageRepository).save(msgCaptor.capture());
        assertThat(msgCaptor.getValue().msgType()).isEqualTo(PrivateMessage.TYPE_SYSTEM);
        assertThat(msgCaptor.getValue().fromUser()).isEqualTo("alice");
        assertThat(msgCaptor.getValue().toUser()).isEqualTo("bob");
        verify(push).pushDm(any(PrivateMessage.class));
        verify(push).pushFriendEvent(eq("friend-accepted"), eq("bob"), anyString());
    }

    @Test
    void acceptOnlyHandlesOwnPendingRequest() {
        when(requestRepository.find("nope")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.accept("alice", "nope"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("申请不存在");

        FriendRequest other = offer("alice", "bob");
        when(requestRepository.find(other.id())).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.accept("alice", other.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("发给自己的");

        FriendRequest handled = FriendRequest.restore("r-1", "bob", "alice", null,
                FriendRequest.STATUS_REJECTED, 1L, 2L);
        when(requestRepository.find(handled.id())).thenReturn(Optional.of(handled));
        assertThatThrownBy(() -> service.accept("alice", handled.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经处理过");
    }

    @Test
    void deleteFriendRemovesPairAndNotifiesPeer() {
        Friend row = edge("alice", "bob");
        when(friendRepository.find(row.id())).thenReturn(Optional.of(row));

        service.deleteFriend("alice", row.id());

        verify(friendRepository).deletePair("alice", "bob");
        verify(push).pushFriendEvent(eq("friend-deleted"), eq("bob"), anyString());
    }

    @Test
    void deleteOnlyOwnFriendRow() {
        Friend row = edge("bob", "alice");
        when(friendRepository.find(row.id())).thenReturn(Optional.of(row));

        assertThatThrownBy(() -> service.deleteFriend("alice", row.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("好友不存在");
    }

    @Test
    void updateFriendSetsRemarkAndPin() {
        Friend row = edge("alice", "bob");
        when(friendRepository.find(row.id())).thenReturn(Optional.of(row));

        service.updateFriend("alice", row.id(), "  老友  ", true, null, null, null);

        ArgumentCaptor<Friend> captor = ArgumentCaptor.forClass(Friend.class);
        verify(friendRepository).save(captor.capture());
        assertThat(captor.getValue().remark()).isEqualTo("老友");
        assertThat(captor.getValue().pinnedFlag()).isTrue();

        assertThatThrownBy(() -> service.updateFriend("alice", row.id(), "x".repeat(33), null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("32");
    }

    @Test
    void updateFriendTogglesMute() {
        Friend row = edge("alice", "bob");
        when(friendRepository.find(row.id())).thenReturn(Optional.of(row));

        service.updateFriend("alice", row.id(), null, null, true, null, null);

        ArgumentCaptor<Friend> captor = ArgumentCaptor.forClass(Friend.class);
        verify(friendRepository).save(captor.capture());
        assertThat(captor.getValue().mutedFlag()).isTrue();
        assertThat(captor.getValue().remark()).isEmpty();
    }

    @Test
    void updateFriendSetsTagAndBlocked() {
        Friend row = edge("alice", "bob");
        when(friendRepository.find(row.id())).thenReturn(Optional.of(row));

        service.updateFriend("alice", row.id(), null, null, null, "  同事  ", true);

        ArgumentCaptor<Friend> captor = ArgumentCaptor.forClass(Friend.class);
        verify(friendRepository).save(captor.capture());
        assertThat(captor.getValue().tag()).isEqualTo("同事");
        assertThat(captor.getValue().blocked()).isEqualTo(1);

        assertThatThrownBy(() -> service.updateFriend("alice", row.id(), null, null, null, "x".repeat(17), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("16");

        // 解除拉黑
        service.updateFriend("alice", row.id(), null, null, null, null, false);
        ArgumentCaptor<Friend> unblock = ArgumentCaptor.forClass(Friend.class);
        verify(friendRepository, org.mockito.Mockito.times(2)).save(unblock.capture());
        assertThat(unblock.getValue().blocked()).isZero();
    }

    @Test
    void listFriendsSortsPinnedFirstAndCarriesUnreadAndPreview() {
        Friend pinned = edge("alice", "carol");
        pinned.changePinned(true);
        pinned.changeTag("家人");
        Friend normal = edge("alice", "bob");
        normal.changeBlocked(true);
        when(friendRepository.findAllByOwner("alice")).thenReturn(List.of(normal, pinned));
        when(profileRepository.listByUsernames(any())).thenReturn(java.util.List.of());

        PrivateMessage latest = PrivateMessage.offer("bob", "alice", "晚上一起吃饭吗", PrivateMessage.TYPE_TEXT, 9L);
        // 批量口径：一次算出各对端最后一条的时间点，一趟 JOIN 算出各对端未读数，再按时间点回捞整行
        lenient().when(messageRepository.findLatestCreatedPerPeer("alice"))
                .thenReturn(java.util.Map.of("bob", latest.created()));
        lenient().when(messageRepository.findMessagesAtCreated(org.mockito.ArgumentMatchers.eq("alice"),
                        org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(latest));
        lenient().when(friendRepository.unreadCountByPeer("alice"))
                .thenReturn(Map.of("bob", 2L, "carol", 0L));
        lenient().when(push.isOnline("bob")).thenReturn(true);
        lenient().when(push.isOnline("carol")).thenReturn(false);

        List<FriendService.FriendVO> friends = service.listFriends("alice");

        assertThat(friends).hasSize(2);
        assertThat(friends.get(0).username()).isEqualTo("carol");
        assertThat(friends.get(0).pinned()).isTrue();
        assertThat(friends.get(0).tag()).isEqualTo("家人");
        assertThat(friends.get(0).status()).isEqualTo("online");
        assertThat(friends.get(1).username()).isEqualTo("bob");
        assertThat(friends.get(1).unread()).isEqualTo(2);
        assertThat(friends.get(1).online()).isTrue();
        assertThat(friends.get(1).blocked()).isTrue();
        assertThat(friends.get(1).lastMessage().content()).isEqualTo("晚上一起吃饭吗");
        assertThat(friends.get(1).lastMessage().fromMe()).isFalse();
    }

    @Test
    void listFriendsNeverFallsBackToPerPeerQueries() {
        when(friendRepository.findAllByOwner("alice"))
                .thenReturn(List.of(edge("alice", "bob"), edge("alice", "carol"), edge("alice", "dave")));
        lenient().when(messageRepository.findLatestCreatedPerPeer("alice")).thenReturn(Map.of());
        lenient().when(messageRepository.findMessagesAtCreated(anyString(), anyList(), anyCollection()))
                .thenReturn(List.of());
        lenient().when(profileRepository.listByUsernames(any())).thenReturn(List.of());

        service.listFriends("alice");

        // 三个好友也只发批量查询：一旦有人把循环里的逐条查询加回来，这里就会红。
        // 改造前这条靠 verify(messageMapper, never()).findLatestBetween/countUnread 兜住，
        // 收口后那两个逐条口径已经不在端口上，所以改成「只许有这两次批量取，多一次都算红」。
        verify(messageRepository).findLatestCreatedPerPeer("alice");
        verify(messageRepository).findMessagesAtCreated(eq("alice"), anyList(), anyCollection());
        verifyNoMoreInteractions(messageRepository);
        verify(friendRepository).findAllByOwner("alice");
        verify(friendRepository).unreadCountByPeer("alice");
    }

    @Test
    void listFriendsCarriesPeerNicknameFromProfile() {
        Friend row = edge("alice", "bob");
        when(friendRepository.findAllByOwner("alice")).thenReturn(List.of(row));
        UserProfile bobProfile = UserProfile.restore("bob", "波波", null, null, "busy", null, 1L);
        when(profileRepository.listByUsernames(any())).thenReturn(List.of(bobProfile));
        lenient().when(messageRepository.findLatestCreatedPerPeer(anyString())).thenReturn(Map.of());
        lenient().when(messageRepository.findMessagesAtCreated(anyString(), anyList(), anyCollection()))
                .thenReturn(List.of());
        lenient().when(push.isOnline("bob")).thenReturn(false);

        List<FriendService.FriendVO> friends = service.listFriends("alice");

        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).nickname()).isEqualTo("波波");
        assertThat(friends.get(0).status()).isEqualTo("busy");
    }

    @Test
    void rejectMarksRequestRejected() {
        FriendRequest request = offer("bob", "alice");
        when(requestRepository.find(request.id())).thenReturn(Optional.of(request));

        service.reject("alice", request.id());

        assertThat(request.status()).isEqualTo(FriendRequest.STATUS_REJECTED);
        verify(requestRepository).save(request);
        verify(messageRepository, never()).save(any(PrivateMessage.class));
    }
}
