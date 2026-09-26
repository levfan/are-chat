package com.smart.chat.auth;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.FriendMapper;
import com.smart.chat.im.UserProfile;
import com.smart.chat.im.UserProfileMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 账号服务：注册建号昵称回退、登录后修改昵称（同步 app_user） */
@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserMapper userMapper;

    @Mock
    private UserProfileMapper profileMapper;

    @Mock
    private FriendMapper friendMapper;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private SmsCodeService smsCodeService;

    @InjectMocks
    private AppUserService service;

    @Test
    void createAccountFallsBackToUsernameWhenNicknameBlank() {
        when(userMapper.findByPhone("13900001111")).thenReturn(Optional.empty());
        when(userMapper.findByUsername("zhangsan")).thenReturn(Optional.empty());
        when(profileMapper.selectById("zhangsan")).thenReturn(null);

        service.createAccount("13900001111", "zhangsan", "  ", "hash", AppUser.ROLE_USER);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("zhangsan");
    }

    @Test
    void createAccountUsesProvidedNickname() {
        when(userMapper.findByPhone("13900001111")).thenReturn(Optional.empty());
        when(userMapper.findByUsername("zhangsan")).thenReturn(Optional.empty());
        when(profileMapper.selectById("zhangsan")).thenReturn(null);

        service.createAccount("13900001111", "zhangsan", "张三", "hash", AppUser.ROLE_USER);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("张三");

        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(profileMapper).insert(profileCaptor.capture());
        assertThat(profileCaptor.getValue().getNickname()).isEqualTo("张三");
    }

    @Test
    void updateNicknameTrimsAndSaves() {
        AppUser alice = AppUser.of("13800000001", "alice", "hash", "alice", "c0");
        when(userMapper.findByUsername("alice")).thenReturn(Optional.of(alice));

        service.updateNickname("alice", "  新昵称  ");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getNickname()).isEqualTo("新昵称");
    }

    @Test
    void updateNicknameRejectsBlank() {
        when(userMapper.findByUsername("alice")).thenReturn(Optional.of(
                AppUser.of("13800000001", "alice", "hash", "alice", "c0")));

        assertThatThrownBy(() -> service.updateNickname("alice", "   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("昵称");
        verify(userMapper, never()).updateById(any(AppUser.class));
    }

    @Test
    void updateNicknameRejectsTooLong() {
        when(userMapper.findByUsername("alice")).thenReturn(Optional.of(
                AppUser.of("13800000001", "alice", "hash", "alice", "c0")));

        assertThatThrownBy(() -> service.updateNickname("alice", "x".repeat(33)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("昵称");
    }

    @Test
    void updateNicknameUnknownUserIs404() {
        when(userMapper.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateNickname("ghost", "新昵称"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号不存在");
        verify(userMapper, never()).updateById(any(AppUser.class));
    }

    @Test
    void validateNicknameNormalizesAndValidates() {
        assertThat(service.validateNickname(null)).isNull();
        assertThat(service.validateNickname("   ")).isNull();
        assertThat(service.validateNickname(" 小明 ")).isEqualTo("小明");
        assertThatThrownBy(() -> service.validateNickname("x".repeat(33)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1~32");
    }

    @Test
    void validateNicknameToleratesCjkBoundary() {
        // 32 个汉字恰好合法（昵称按字符数校验）
        assertThat(service.validateNickname("中".repeat(32))).hasSize(32);
    }
}
