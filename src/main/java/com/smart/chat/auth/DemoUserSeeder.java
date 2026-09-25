package com.smart.chat.auth;

import com.smart.chat.im.UserProfile;
import com.smart.chat.im.UserProfileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 演示账号播种：库里一个用户都没有时，注册 alice / bob / carol 三个开箱可用的账号
 * （手机号 13800000001~3，密码统一 arechat123）。
 * 它们走的就是正常注册逻辑，不再是写死的白名单——之后任何用户都必须手机号注册。
 */
@Component
public class DemoUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoUserSeeder.class);

    public static final String DEMO_PASSWORD = "arechat123";

    private record Demo(String username, String phone, String avatar) {
    }

    private static final List<Demo> DEMO_USERS = List.of(
            new Demo("alice", "13800000001", "c0"),
            new Demo("bob", "13800000002", "c1"),
            new Demo("carol", "13800000003", "c2"));

    private final AppUserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final PasswordHasher passwordHasher;

    public DemoUserSeeder(AppUserMapper userMapper, UserProfileMapper profileMapper, PasswordHasher passwordHasher) {
        this.userMapper = userMapper;
        this.profileMapper = profileMapper;
        this.passwordHasher = passwordHasher;
    }

    /** 演示账号名（登录页提示用） */
    public static String demoAccountHint() {
        return DEMO_USERS.stream().map(Demo::username).reduce((a, b) -> a + " / " + b).orElse("");
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userMapper.countUsers() > 0) {
            return;
        }
        for (Demo demo : DEMO_USERS) {
            AppUser user = AppUser.of(demo.phone(), demo.username(),
                    passwordHasher.encode(DEMO_PASSWORD), demo.username(), demo.avatar());
            userMapper.insert(user);
            ensureProfile(user);
        }
        log.info("已播种演示账号 {}（密码 {}），可直接登录体验", demoAccountHint(), DEMO_PASSWORD);
    }

    private void ensureProfile(AppUser user) {
        if (profileMapper.selectById(user.getUsername()) != null) {
            return;
        }
        UserProfile profile = new UserProfile();
        profile.setUsername(user.getUsername());
        profile.setNickname(user.getNickname());
        profile.setSignature("");
        profile.setAvatar(user.getAvatar());
        profile.setPresenceStatus("online");
        profile.setUpdatedAt(System.currentTimeMillis());
        profileMapper.insert(profile);
    }
}
