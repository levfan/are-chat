package com.smart.chat.im;

import com.smart.chat.auth.AppUserService;
import com.smart.chat.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendMapper friendMapper;

    @Mock
    private FriendRequestMapper requestMapper;

    @Mock
    private PrivateMessageMapper messageMapper;

    @Mock
    private UserProfileMapper profileMapper;

    @Mock
    private ImPushService push;

    @Mock
    private AppUserService userService;

    @InjectMocks
    private FriendService service;

    /** 合法用户目录：默认「输入规范化 + 用户存在」，各用例可按需覆盖 */
    @org.junit.jupiter.api.BeforeEach
    void stubUserDirectory() {
        lenient().when(userService.normalizeUsername(anyString()))
                .thenAnswer(inv -> inv.getArgument(0, String.class).trim().toLowerCase());
        lenient().when(userService.exists(anyString())).thenReturn(true);
        lenient().when(userService.search(anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(java.util.List.of());
    }

    @Test
    void applySuccessPushesRequestToTarget() {
        when(requestMapper.findPendingBetween("alice", "bob")).thenReturn(Optional.empty());
        when(requestMapper.findPendingBetween("bob", "alice")).thenReturn(Optional.empty());

        FriendService.FriendRequestVO vo = service.apply("alice", "bob", "我是carol，加个好友");

        assertThat(vo.status()).isEqualTo(FriendRequest.STATUS_PENDING);
        assertThat(vo.fromUser()).isEqualTo("alice");
        assertThat(vo.message()).isEqualTo("我是carol，加个好友");
        ArgumentCaptor<FriendRequest> captor = ArgumentCaptor.forClass(FriendRequest.class);
        verify(requestMapper).insert(captor.capture());
        assertThat(captor.getValue().getToUser()).isEqualTo("bob");
        assertThat(captor.getValue().getMessage()).isEqualTo("我是carol，加个好友");
        verify(push).pushFriendEvent(eq("friend-request"), eq("bob"), anyString());
    }

    @Test
    void applyValidatesMessageLength() {
        when(requestMapper.findPendingBetween("alice", "bob")).thenReturn(Optional.empty());
        when(requestMapper.findPendingBetween("bob", "alice")).thenReturn(Optional.empty());
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
        when(userService.exists("路人甲")).thenReturn(false);
        assertThatThrownBy(() -> service.apply("alice", "路人甲", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("查无此人");

        when(friendMapper.findByOwnerAndFriend("alice", "bob"))
                .thenReturn(Optional.of(Friend.of("alice", "bob")));
        assertThatThrownBy(() -> service.apply("alice", "bob", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经是好友");

        when(friendMapper.findByOwnerAndFriend("alice", "bob")).thenReturn(Optional.empty());
        when(requestMapper.findPendingBetween("alice", "bob"))
                .thenReturn(Optional.of(FriendRequest.of("alice", "bob")));
        assertThatThrownBy(() -> service.apply("alice", "bob", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("等对方处理");

        when(requestMapper.findPendingBetween("alice", "bob")).thenReturn(Optional.empty());
        when(requestMapper.findPendingBetween("bob", "alice"))
                .thenReturn(Optional.of(FriendRequest.of("bob", "alice")));
        assertThatThrownBy(() -> service.apply("alice", "bob", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("先向你发起");
    }

    @Test
    void acceptCreatesBothFriendRowsAndSystemMessage() {
        FriendRequest request = FriendRequest.of("bob", "alice");
        when(requestMapper.selectById(request.getId())).thenReturn(request);
        when(friendMapper.findByOwnerAndFriend("bob", "alice")).thenReturn(Optional.empty());
        when(friendMapper.findByOwnerAndFriend("alice", "bob")).thenReturn(Optional.empty());

        service.accept("alice", request.getId());

        assertThat(request.getStatus()).isEqualTo(FriendRequest.STATUS_ACCEPTED);
        verify(requestMapper).updateById(request);
        verify(friendMapper, times(2)).insert(any(Friend.class));

        ArgumentCaptor<PrivateMessage> msgCaptor = ArgumentCaptor.forClass(PrivateMessage.class);
        verify(messageMapper).insert(msgCaptor.capture());
        assertThat(msgCaptor.getValue().getMsgType()).isEqualTo(PrivateMessage.TYPE_SYSTEM);
        assertThat(msgCaptor.getValue().getFromUser()).isEqualTo("alice");
        assertThat(msgCaptor.getValue().getToUser()).isEqualTo("bob");
        verify(push).pushDm(any(PrivateMessage.class));
        verify(push).pushFriendEvent(eq("friend-accepted"), eq("bob"), anyString());
    }

    @Test
    void acceptOnlyHandlesOwnPendingRequest() {
        when(requestMapper.selectById("nope")).thenReturn(null);
        assertThatThrownBy(() -> service.accept("alice", "nope"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("申请不存在");

        FriendRequest other = FriendRequest.of("alice", "bob");
        when(requestMapper.selectById(other.getId())).thenReturn(other);
        assertThatThrownBy(() -> service.accept("alice", other.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("发给自己的");

        FriendRequest handled = FriendRequest.of("bob", "alice");
        handled.setStatus(FriendRequest.STATUS_REJECTED);
        when(requestMapper.selectById(handled.getId())).thenReturn(handled);
        assertThatThrownBy(() -> service.accept("alice", handled.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经处理过");
    }

    @Test
    void deleteFriendRemovesPairAndNotifiesPeer() {
        Friend row = Friend.of("alice", "bob");
        when(friendMapper.selectById(row.getId())).thenReturn(row);

        service.deleteFriend("alice", row.getId());

        verify(friendMapper).deletePair("alice", "bob");
        verify(push).pushFriendEvent(eq("friend-deleted"), eq("bob"), anyString());
    }

    @Test
    void deleteOnlyOwnFriendRow() {
        Friend row = Friend.of("bob", "alice");
        when(friendMapper.selectById(row.getId())).thenReturn(row);

        assertThatThrownBy(() -> service.deleteFriend("alice", row.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("好友不存在");
    }

    @Test
    void updateFriendSetsRemarkAndPin() {
        Friend row = Friend.of("alice", "bob");
        when(friendMapper.selectById(row.getId())).thenReturn(row);

        service.updateFriend("alice", row.getId(), "  老友  ", true, null, null, null);

        ArgumentCaptor<Friend> captor = ArgumentCaptor.forClass(Friend.class);
        verify(friendMapper).updateById(captor.capture());
        assertThat(captor.getValue().getRemark()).isEqualTo("老友");
        assertThat(captor.getValue().getPinned()).isTrue();

        assertThatThrownBy(() -> service.updateFriend("alice", row.getId(), "x".repeat(33), null, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("32");
    }

    @Test
    void updateFriendTogglesMute() {
        Friend row = Friend.of("alice", "bob");
        when(friendMapper.selectById(row.getId())).thenReturn(row);

        service.updateFriend("alice", row.getId(), null, null, true, null, null);

        ArgumentCaptor<Friend> captor = ArgumentCaptor.forClass(Friend.class);
        verify(friendMapper).updateById(captor.capture());
        assertThat(captor.getValue().getMuted()).isTrue();
        assertThat(captor.getValue().getRemark()).isEmpty();
    }

    @Test
    void updateFriendSetsTagAndBlocked() {
        Friend row = Friend.of("alice", "bob");
        when(friendMapper.selectById(row.getId())).thenReturn(row);

        service.updateFriend("alice", row.getId(), null, null, null, "  同事  ", true);

        ArgumentCaptor<Friend> captor = ArgumentCaptor.forClass(Friend.class);
        verify(friendMapper).updateById(captor.capture());
        assertThat(captor.getValue().getTag()).isEqualTo("同事");
        assertThat(captor.getValue().getBlocked()).isEqualTo(1);

        assertThatThrownBy(() -> service.updateFriend("alice", row.getId(), null, null, null, "x".repeat(17), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("16");

        // 解除拉黑
        service.updateFriend("alice", row.getId(), null, null, null, null, false);
        ArgumentCaptor<Friend> unblock = ArgumentCaptor.forClass(Friend.class);
        verify(friendMapper, org.mockito.Mockito.times(2)).updateById(unblock.capture());
        assertThat(unblock.getValue().getBlocked()).isZero();
    }

    @Test
    void listFriendsSortsPinnedFirstAndCarriesUnreadAndPreview() {
        Friend pinned = Friend.of("alice", "carol");
        pinned.setPinned(true);
        pinned.setTag("家人");
        Friend normal = Friend.of("alice", "bob");
        normal.setBlocked(1);
        when(friendMapper.findAllByOwner("alice")).thenReturn(List.of(normal, pinned));
        when(profileMapper.selectBatchIds(any())).thenReturn(java.util.List.of());

        PrivateMessage latest = PrivateMessage.of("bob", "alice", "晚上一起吃饭吗", PrivateMessage.TYPE_TEXT);
        lenient().when(messageMapper.findLatestBetween("alice", "bob")).thenReturn(Optional.of(latest));
        lenient().when(messageMapper.findLatestBetween("alice", "carol")).thenReturn(Optional.empty());
        lenient().when(messageMapper.countUnread("alice", "bob", normal.getLastReadAt())).thenReturn(2L);
        lenient().when(messageMapper.countUnread("alice", "carol", pinned.getLastReadAt())).thenReturn(0L);
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
    void rejectMarksRequestRejected() {
        FriendRequest request = FriendRequest.of("bob", "alice");
        when(requestMapper.selectById(request.getId())).thenReturn(request);

        service.reject("alice", request.getId());

        assertThat(request.getStatus()).isEqualTo(FriendRequest.STATUS_REJECTED);
        verify(requestMapper).updateById(request);
        verify(messageMapper, never()).insert(any(PrivateMessage.class));
    }
}
