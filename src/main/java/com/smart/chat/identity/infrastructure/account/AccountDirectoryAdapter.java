package com.smart.chat.identity.infrastructure.account;

import com.smart.chat.identity.application.AppUserService;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.identity.infrastructure.persistence.AppUser;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** AccountDirectory 的唯一实现：把外部上下文的提问转给 identity 自己的服务。 */
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
        return userService.find(username).map(AppUser::view);
    }

    @Override
    public java.util.List<Account> search(String keyword, String exclude, int limit) {
        return userService.search(keyword, exclude, limit).stream().map(AppUser::view).toList();
    }

    @Override
    public void updateNickname(String username, String nickname) {
        userService.updateNickname(username, nickname);
    }

    @Override
    public void requireAdmin(String username) {
        userService.requireAdmin(username);
    }
}
