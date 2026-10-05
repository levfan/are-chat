package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.invite.InviteRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.wish.WishRepository;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.messaging.domain.FriendshipChecker;
import com.smart.chat.messaging.domain.PeerProfileReader;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 空间个性化（`PUT /api/couple/profile`）的推送分流。
 * <p>
 * 裁剪后宣言、主题、爱称三项合并进了同一个写入口，但它们在对方眼里是三件不同的事：
 * 「TA 给你起了新爱称」和「TA 打扮了小空间」不是一句话。全量测试照不出这里写反——
 * 只有把「改了什么就推什么」逐条钉住，才能挡住将来有人图省事把它合并回一次推送。
 */
@ExtendWith(MockitoExtension.class)
class CoupleProfilePushTest {

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private InviteRepository inviteRepository;
    @Mock
    private WishRepository wishRepository;
    @Mock
    private CoupleStreakService streakService;
    @Mock
    private CoupleQuestionService questionService;
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

    /** alice=userA、bob=userB；nickB 是「bob 的爱称」，只有 alice 能改。 */
    private CoupleSpace space(String slogan, String theme, String nickB) {
        CoupleSpace space = CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 0L,
                null, null, nickB, slogan, theme, null);
        lenient().when(spaceRepository.findActiveByMember("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceRepository.findActiveByMember("bob")).thenReturn(Optional.of(space));
        lenient().when(profiles.read(any())).thenReturn(Optional.empty());
        lenient().when(push.isOnline(any())).thenReturn(true);
        return space;
    }

    @Test
    void renamingOnlyPushesThePetNameEvent() {
        space("我们的宣言", "cherry", null);

        service.updateProfile("alice", null, null, "小饼");

        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEvent(eq("pet-name-changed"), eq("alice"), eq("bob"), detail.capture());
        assertThat(detail.getValue()).contains("小饼");
        verify(push, never()).pushCoupleEvent(eq("space-themed"), any(), any(), any());
    }

    @Test
    void decoratingOnlyPushesTheThemeEvent() {
        space("我们的宣言", "cherry", "小饼");

        service.updateProfile("alice", "换个说法", null, null);

        verify(push).pushCoupleEvent(eq("space-themed"), eq("alice"), eq("bob"), any());
        verify(push, never()).pushCoupleEvent(eq("pet-name-changed"), any(), any(), any());
    }

    @Test
    void bothChangedPushesEachEventOnce() {
        space("我们的宣言", "cherry", null);

        service.updateProfile("alice", "新宣言", "ocean", "小饼");

        verify(push, times(1)).pushCoupleEvent(eq("space-themed"), any(), any(), any());
        verify(push, times(1)).pushCoupleEvent(eq("pet-name-changed"), any(), any(), any());
    }

    @Test
    void unchangedPetNameDoesNotCelebrateAgain() {
        space(null, "cherry", "小饼");

        service.updateProfile("alice", null, null, "小饼");

        // 把同一个爱称再提交一次不该让对方再收到一次「新爱称」（isOnline 仍会被 VO 调用，所以只锁推送）
        verify(push, never()).pushCoupleEvent(any(), any(), any(), any());
        verify(spaceRepository).save(any());
    }

    @Test
    void clearingThePetNameSaysSoInsteadOfQuotingNull() {
        space(null, "cherry", "小饼");

        service.updateProfile("alice", null, null, "");

        ArgumentCaptor<String> detail = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEvent(eq("pet-name-changed"), any(), any(), detail.capture());
        assertThat(detail.getValue()).doesNotContain("null").contains("收回");
    }

    @Test
    void invalidThemeSavesNothingAndPushesNothing() {
        space(null, "cherry", null);

        assertThatThrownBy(() -> service.updateProfile("alice", null, "sakura", "小饼"))
                .hasMessageContaining("主题");

        verify(spaceRepository, never()).save(any());
        verifyNoInteractions(push);
    }
}
