package com.smart.chat.identity.infrastructure.persistence;

import com.smart.chat.identity.domain.account.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 账号仓储适配器的两条口径：
 * <ol>
 *   <li><b>更新只回写聚合纳管的列</b>——app_user 上 {@code signature}/{@code presence_status} 的权威副本在
 *       user_profile，用聚合重建整行会把历史快照清空，这类 bug 编译与业务测试都照不出来；</li>
 *   <li><b>插入时补齐聚合不管的默认值</b>（空签名 / "online"），保证与改造前 {@code AppUser.of} 建出的行一致。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class AccountRepositoryAdapterTest {

    @Mock
    private AppUserMapper userMapper;

    @InjectMocks
    private AccountRepositoryAdapter repository;

    @Test
    void updateKeepsColumnsTheAggregateDoesNotOwn() {
        AppUserPO existing = new AppUserPO();
        existing.setId("u1");
        existing.setUsername("alice");
        existing.setPhone("13800000001");
        existing.setNickname("Alice");
        existing.setPasswordHash("old-hash");
        existing.setAvatar("c0");
        existing.setSignature("旧签名");
        existing.setPresenceStatus("busy");
        existing.setStatus(AppUserPO.STATUS_ACTIVE);
        existing.setRole(AppUserPO.ROLE_USER);
        existing.setCreated(1L);
        when(userMapper.selectById("u1")).thenReturn(existing);

        repository.save(Account.restore("u1", "13800000001", "alice", "新昵称", "new-hash", "c3",
                Account.STATUS_DISABLED, Account.ROLE_USER, 1L, 9000L));

        ArgumentCaptor<AppUserPO> captor = ArgumentCaptor.forClass(AppUserPO.class);
        verify(userMapper).updateById(captor.capture());
        AppUserPO written = captor.getValue();
        assertThat(written.getSignature()).as("聚合没建模的列不能被动").isEqualTo("旧签名");
        assertThat(written.getPresenceStatus()).isEqualTo("busy");
        assertThat(written.getNickname()).isEqualTo("新昵称");
        assertThat(written.getPasswordHash()).isEqualTo("new-hash");
        assertThat(written.getAvatar()).isEqualTo("c3");
        assertThat(written.getStatus()).isEqualTo(AppUserPO.STATUS_DISABLED);
        assertThat(written.getLastLoginAt()).isEqualTo(9000L);
        verify(userMapper, never()).insert(any(AppUserPO.class));
    }

    @Test
    void newAccountIsInsertedWithTheLegacyColumnDefaults() {
        // register 会生成随机 UUID 主键，所以这里按「查不到原行」统一打桩
        when(userMapper.selectById(anyString())).thenReturn(null);

        repository.save(Account.register("13900001111", "zhangsan", "张三", "hash", Account.ROLE_USER));

        ArgumentCaptor<AppUserPO> captor = ArgumentCaptor.forClass(AppUserPO.class);
        verify(userMapper).insert(captor.capture());
        AppUserPO po = captor.getValue();
        assertThat(po.getUsername()).isEqualTo("zhangsan");
        assertThat(po.getPhone()).isEqualTo("13900001111");
        assertThat(po.getNickname()).isEqualTo("张三");
        assertThat(po.getPasswordHash()).isEqualTo("hash");
        assertThat(po.getAvatar()).isEqualTo("c0");
        assertThat(po.getStatus()).isEqualTo(AppUserPO.STATUS_ACTIVE);
        assertThat(po.getRole()).isEqualTo(AppUserPO.ROLE_USER);
        assertThat(po.getCreated()).isNotNull();
        assertThat(po.getSignature()).isEqualTo("");
        assertThat(po.getPresenceStatus()).isEqualTo("online");
        verify(userMapper, never()).updateById(any(AppUserPO.class));
    }

    @Test
    void readsComeBackAsAggregatesWithEveryColumnMapped() {
        AppUserPO po = new AppUserPO();
        po.setId("u3");
        po.setUsername("bob");
        po.setPhone("13800000002");
        po.setNickname("Bob");
        po.setPasswordHash("hash");
        po.setAvatar("c1");
        po.setSignature("签名");
        po.setPresenceStatus("away");
        po.setStatus(AppUserPO.STATUS_CLOSED);
        po.setRole(AppUserPO.ROLE_ADMIN);
        po.setCreated(11L);
        po.setLastLoginAt(22L);
        when(userMapper.findByAccount("bob")).thenReturn(Optional.of(po));
        when(userMapper.searchAll("bob", 200)).thenReturn(List.of(po));

        Account byAccount = repository.findByAccount("bob").orElseThrow();
        assertThat(byAccount.username()).isEqualTo("bob");
        assertThat(byAccount.status()).isEqualTo(Account.STATUS_CLOSED);
        assertThat(byAccount.isAdmin()).isTrue();
        assertThat(byAccount.maskedPhone()).isEqualTo("138****0002");
        assertThat(byAccount.created()).isEqualTo(11L);
        assertThat(byAccount.lastLoginAt()).isEqualTo(22L);
        assertThat(byAccount.avatar()).isEqualTo("c1");

        assertThat(repository.listForAdmin("bob", 200)).singleElement()
                .satisfies(account -> assertThat(account.nickname()).isEqualTo("Bob"));
    }

    @Test
    void legacyDemoAccountsAreQueriedByPhoneAndActiveStatus() {
        AppUserPO demo = new AppUserPO();
        demo.setId("u4");
        demo.setUsername("alice");
        demo.setPhone("13800000001");
        demo.setStatus(AppUserPO.STATUS_ACTIVE);
        demo.setRole(AppUserPO.ROLE_USER);
        demo.setCreated(1L);
        when(userMapper.findActiveAmongPhones(List.of("13800000001"))).thenReturn(List.of(demo));

        List<Account> found = repository.listActiveAmongPhones(List.of("13800000001"));

        assertThat(found).singleElement().satisfies(account -> {
            assertThat(account.username()).isEqualTo("alice");
            assertThat(account.status()).isEqualTo(Account.STATUS_ACTIVE);
        });
    }
}
