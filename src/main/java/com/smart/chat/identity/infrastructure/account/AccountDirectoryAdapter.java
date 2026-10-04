package com.smart.chat.identity.infrastructure.account;

import com.smart.chat.identity.application.AppUserService;
import com.smart.chat.identity.domain.AccountDirectory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * AccountDirectory 的唯一实现：把外部上下文的提问转给 identity 自己的服务，
 * 并在这一处把账号聚合折成发布语言（{@code domain.account.Account} → {@link Account}），
 * 保证 app_user 的表模型与密码哈希永远不外泄。
 * <p>
 * 类名 {@code Account} 在本文件里指的是端口内嵌的**最小视图记录**（继承自接口的成员类型），
 * 聚合本体一律用全限定名书写，避免两个同名类型混在一起。
 */
@Component
public class AccountDirectoryAdapter implements AccountDirectory {

    private final AppUserService userService;

    public AccountDirectoryAdapter(AppUserService userService) {
        this.userService = userService;
    }

    @Override
    public String normalizeUsername(String raw) {
        return userService.normalizeUsername(raw);
    }

    @Override
    public boolean exists(String username) {
        return userService.exists(username);
    }

    @Override
    public Optional<Account> find(String username) {
        return userService.find(username).map(AccountDirectoryAdapter::toView);
    }

    @Override
    public java.util.List<Account> search(String keyword, String exclude, int limit) {
        return userService.search(keyword, exclude, limit).stream().map(AccountDirectoryAdapter::toView).toList();
    }

    @Override
    public void updateNickname(String username, String nickname) {
        userService.updateNickname(username, nickname);
    }

    @Override
    public void requireAdmin(String username) {
        userService.requireAdmin(username);
    }

    /** 对外只给这张最小视图：手机号脱敏，角色只留「是不是管理员」 */
    private static Account toView(com.smart.chat.identity.domain.account.Account account) {
        return new Account(account.username(), account.nickname(), account.isAdmin(), account.maskedPhone());
    }
}
