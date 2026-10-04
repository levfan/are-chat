package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.infrastructure.persistence.CoupleInviteMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleInvitePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpacePO;
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
 */
@ExtendWith(MockitoExtension.class)
class CoupleInviteAcceptTest {

    private static final String INVITE_ID = "i1";

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private CoupleInviteMapper inviteMapper;
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

    private CoupleInvitePO invite(String from, String to) {
        CoupleInvitePO invite = CoupleInvitePO.of(from, to, "在一起吧");
        invite.setId(INVITE_ID);
        return invite;
    }

    private CoupleSpacePO activeSpace(String a, String b) {
        CoupleSpacePO space = CoupleSpacePO.of(a, b);
        space.setId("s1");
        return space;
    }

    private void stubNoSpaceForAnyone() {
        lenient().when(spaceMapper.findActiveByUser(anyString())).thenReturn(Optional.empty());
        lenient().when(profiles.read(anyString())).thenReturn(Optional.empty());
    }

    /** 让仓储适配器那种「save 之后就能查到行」的行为在假表里成立（save 是 void，只能 doAnswer）。 */
    private void stubRepositoryCreatesSpace(String me) {
        org.mockito.Mockito.doAnswer(inv -> {
            CoupleSpace opened = inv.getArgument(0);
            CoupleSpacePO row = new CoupleSpacePO();
            row.setId(opened.id());
            row.setUserA(opened.userA());
            row.setUserB(opened.userB());
            row.setStatus(opened.status());
            row.setCreated(opened.created());
            row.setTheme(opened.theme());
            lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(row));
            return null;
        }).when(spaceRepository).save(any(CoupleSpace.class));
    }

    @Test
    void acceptingOpensTheSpaceThroughTheAggregateWithSortedMembers() {
        CoupleInvitePO stored = invite("zed", "alice");
        when(inviteMapper.selectById(INVITE_ID)).thenReturn(stored);
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
        // 不再走 PO 直插，聚合是唯一的建空间入口
        verify(spaceMapper, never()).insert(any(CoupleSpacePO.class));
        assertThat(stored.getStatus()).isEqualTo(CoupleInvitePO.STATUS_ACCEPTED);
        verify(inviteMapper).updateById(stored);
        verify(push).pushCoupleEvent(eq("invite-accepted"), eq("alice"), eq("zed"), anyString());
    }

    @Test
    void onlyTheRecipientCanAcceptAndOnlyOnce() {
        when(inviteMapper.selectById(INVITE_ID)).thenReturn(invite("zed", "alice"));
        stubNoSpaceForAnyone();

        assertThatThrownBy(() -> service.accept("zed", INVITE_ID))
                .isInstanceOf(BusinessException.class).hasMessage("只能处理发给自己的邀请");

        CoupleInvitePO handled = invite("zed", "alice");
        handled.setStatus(CoupleInvitePO.STATUS_ACCEPTED);
        when(inviteMapper.selectById(INVITE_ID)).thenReturn(handled);
        assertThatThrownBy(() -> service.accept("alice", INVITE_ID))
                .isInstanceOf(BusinessException.class).hasMessage("该邀请已经处理过了");
        verify(spaceRepository, never()).save(any(CoupleSpace.class));
    }

    @Test
    void acceptingRefusesWhenEitherSideAlreadyHasASpace() {
        when(inviteMapper.selectById(INVITE_ID)).thenReturn(invite("zed", "alice"));
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(activeSpace("alice", "bob")));

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
        verify(inviteMapper, never()).insert(any(CoupleInvitePO.class));
    }

    @Test
    void dissolvingIsOneWayAndAnnouncesToThePartner() {
        CoupleSpacePO space = activeSpace("alice", "bob");
        when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        service.dissolve("bob");

        assertThat(space.getStatus()).isEqualTo(CoupleSpacePO.STATUS_DISSOLVED);
        assertThat(space.getDissolvedAt()).isNotNull();
        verify(spaceMapper).updateById(space);
        verify(push).pushCoupleEvent(eq("dissolved"), eq("bob"), eq("alice"), anyString());
    }
}
