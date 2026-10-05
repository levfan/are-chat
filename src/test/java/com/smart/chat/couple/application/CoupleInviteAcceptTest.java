package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.invite.Invite;
import com.smart.chat.couple.domain.invite.InviteRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.messaging.domain.FriendshipChecker;
import com.smart.chat.messaging.domain.PeerProfileReader;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 邀请建立情侣空间（需求 1）的守卫：同意之后必须由<b>聚合</b>开出空间、
 * 双方按字典序落进 userA/userB，且「有一方已经在别的空间」这道闸门要真的拦下来。
 * 邀请的状态机（谁能处理、只能处理一次）现在由 {@link Invite} 自己守，
 * 所以这里断言的是聚合落定后的状态 + 交回仓储的那次 save。
 */
@ExtendWith(MockitoExtension.class)
class CoupleInviteAcceptTest {

    private static final String INVITE_ID = "i1";

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private CoupleStreakService streakService;
    @Mock
    private CoupleQuestionService questionService;
    @Mock
    private com.smart.chat.couple.domain.wish.WishRepository wishRepository;
    @Mock
    private InviteRepository inviteRepository;
    @Mock
    private FriendshipChecker friendships;
    @Mock
    private PeerProfileReader profiles;
    @Mock
    private AccountDirectory accounts;
    @Mock
    private CoupleEventPublisher push;

    @InjectMocks
    private CoupleService service;

    private Invite invite(String from, String to) {
        return Invite.restore(INVITE_ID, from, to, "在一起吧", Invite.STATUS_PENDING,
                System.currentTimeMillis(), null);
    }

    private Invite inviteIn(String from, String to, String status) {
        return Invite.restore(INVITE_ID, from, to, "在一起吧", status, System.currentTimeMillis(), null);
    }

    private CoupleSpace activeSpace(String a, String b) {
        return CoupleSpace.restore("s1", a, b, CoupleSpace.STATUS_ACTIVE, System.currentTimeMillis(),
                null, null, null, null, null, null);
    }

    private void stubNoSpaceForAnyone() {
        lenient().when(spaceRepository.findActiveByMember(anyString())).thenReturn(Optional.empty());
        lenient().when(profiles.read(anyString())).thenReturn(Optional.empty());
    }

    /** 让仓储那种「save 之后就能查到空间」的行为在假表里成立（save 是 void，只能 doAnswer）。 */
    private void stubRepositoryCreatesSpace(String me) {
        org.mockito.Mockito.doAnswer(inv -> {
            CoupleSpace opened = inv.getArgument(0);
            lenient().when(spaceRepository.findActiveByMember(me)).thenReturn(Optional.of(opened));
            return null;
        }).when(spaceRepository).save(any(CoupleSpace.class));
    }

    @Test
    void acceptingOpensTheSpaceThroughTheAggregateWithSortedMembers() {
        Invite stored = invite("zed", "alice");
        when(inviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(stored));
        stubNoSpaceForAnyone();
        stubRepositoryCreatesSpace("alice");

        CoupleService.SpaceVO vo = service.accept("alice", INVITE_ID);

        ArgumentCaptor<CoupleSpace> captor = ArgumentCaptor.forClass(CoupleSpace.class);
        verify(spaceRepository).save(captor.capture());
        CoupleSpace opened = captor.getValue();
        // 字典序小者是 userA：邀请人是 zed，同意者是 alice
        assertThat(opened.userA()).isEqualTo("alice");
        assertThat(opened.userB()).isEqualTo("zed");
        assertThat(opened.status()).isEqualTo(CoupleSpace.STATUS_ACTIVE);
        assertThat(opened.isActive()).isTrue();
        assertThat(vo.partner().username()).isEqualTo("zed");
        // 建空间只有一条路：经聚合交给仓储端口
        assertThat(stored.status()).isEqualTo(Invite.STATUS_ACCEPTED);
        verify(inviteRepository).save(stored);
        verify(push).pushCoupleEvent(eq("invite-accepted"), eq("alice"), eq("zed"), anyString());
    }

    @Test
    void onlyTheRecipientCanAcceptAndOnlyOnce() {
        when(inviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(invite("zed", "alice")));
        stubNoSpaceForAnyone();

        assertThatThrownBy(() -> service.accept("zed", INVITE_ID))
                .isInstanceOf(BusinessException.class).hasMessage("只能处理发给自己的邀请");

        when(inviteRepository.findById(INVITE_ID)).thenReturn(
                Optional.of(inviteIn("zed", "alice", Invite.STATUS_ACCEPTED)));
        assertThatThrownBy(() -> service.accept("alice", INVITE_ID))
                .isInstanceOf(BusinessException.class).hasMessage("该邀请已经处理过了");
        verify(spaceRepository, never()).save(any(CoupleSpace.class));
    }

    @Test
    void acceptingRefusesWhenEitherSideAlreadyHasASpace() {
        when(inviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(invite("zed", "alice")));
        when(spaceRepository.findActiveByMember("alice")).thenReturn(Optional.of(activeSpace("alice", "bob")));

        assertThatThrownBy(() -> service.accept("alice", INVITE_ID))
                .isInstanceOf(BusinessException.class).hasMessage("无法同意：有一方已经进入其他情侣空间");
        verify(spaceRepository, never()).save(any(CoupleSpace.class));
    }

    @Test
    void invitingNonFriendIsRefusedBeforeAnythingIsWritten() {
        when(accounts.normalizeUsername("bob")).thenReturn("bob");
        when(accounts.exists("bob")).thenReturn(true);
        when(friendships.areFriends("alice", "bob")).thenReturn(false);
        stubNoSpaceForAnyone();

        assertThatThrownBy(() -> service.invite("alice", "bob", "在一起"))
                .isInstanceOf(BusinessException.class).hasMessage("只能邀请自己的好友，先去通讯录加个好友吧");
        verify(inviteRepository, never()).save(any(Invite.class));
    }

    @Test
    void rejectingAndCancelingAreBothOneWayAndAnnounceOnlyToTheSender() {
        Invite toMe = invite("zed", "alice");
        when(inviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(toMe));
        service.reject("alice", INVITE_ID);
        assertThat(toMe.status()).isEqualTo(Invite.STATUS_REJECTED);
        verify(push).pushCoupleEvent(eq("invite-rejected"), eq("alice"), eq("zed"),
                eq("TA 婉拒了情侣空间邀请，做朋友也很好"));

        Invite fromMe = invite("alice", "zed");
        when(inviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(fromMe));
        service.cancel("alice", INVITE_ID);
        assertThat(fromMe.status()).isEqualTo(Invite.STATUS_CANCELED);
        // 撤回不推事件：现役口径就是只落库不打扰
        verify(push, never()).pushCoupleEvent(eq("invite-canceled"), anyString(), anyString(), anyString());

        Invite someoneElses = invite("zed", "bob");
        when(inviteRepository.findById(INVITE_ID)).thenReturn(Optional.of(someoneElses));
        assertThatThrownBy(() -> service.cancel("alice", INVITE_ID))
                .isInstanceOf(BusinessException.class).hasMessage("只能撤回自己发出的邀请");
    }
}
