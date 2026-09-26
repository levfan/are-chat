package com.smart.chat.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.config.AdminProperties;
import com.smart.chat.im.UserProfile;
import com.smart.chat.im.UserProfileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动引导（取代已删除的 DemoUserSeeder，模拟用户一律不能登录）：
 * 1. 存量库里的旧演示账号（alice / bob / carol，13800000001~3）标记为 DISABLED——不能再登录；
 * 2. 库里没有任何管理员时，按 arechat.admin.username / password（默认 admin / admin123456，
 *    生产用环境变量 ARECHAT_ADMIN_PASSWORD 覆盖）创建一个真实管理员账号，用于审批注册申请。
 */
@Component
public class AdminBootstrapper implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapper.class);

    /** 历史版本播种的演示账号：手机号固定，逐条禁用 */
    private static final List<String> DEMO_PHONES = List.of("13800000001", "13800000002", "13800000003");

    private final AppUserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final PasswordHasher passwordHasher;
    private final AdminProperties adminProperties;

    public AdminBootstrapper(AppUserMapper userMapper, UserProfileMapper profileMapper,
                             PasswordHasher passwordHasher, AdminProperties adminProperties) {
        this.userMapper = userMapper;
        this.profileMapper = profileMapper;
        this.passwordHasher = passwordHasher;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        disableLegacyDemoUsers();
        ensureAdmin();
    }

    private void disableLegacyDemoUsers() {
        List<AppUser> demos = userMapper.selectList(new LambdaQueryWrapper<AppUser>()
                .in(AppUser::getPhone, DEMO_PHONES)
                .eq(AppUser::getStatus, AppUser.STATUS_ACTIVE));
        for (AppUser demo : demos) {
            demo.setStatus(AppUser.STATUS_DISABLED);
            userMapper.updateById(demo);
        }
        if (!demos.isEmpty()) {
            log.warn("已禁用 {} 个旧版演示账号（模拟用户不能登录）：{}", demos.size(),
                    demos.stream().map(AppUser::getUsername).toList());
        }
    }

    private void ensureAdmin() {
        if (userMapper.countAdmins() > 0) {
            return;
        }
        String username = adminProperties.username().trim().toLowerCase();
        if (userMapper.findByUsername(username).isPresent()) {
            // 同名账号已存在：直接提升为管理员（例如老库的管理员本人）
            AppUser existing = userMapper.findByUsername(username).orElseThrow();
            existing.setRole(AppUser.ROLE_ADMIN);
            userMapper.updateById(existing);
            log.info("已把既有账号 {} 提升为管理员", username);
            return;
        }
        // 管理员账号使用占位手机号（登录只用用户名 + 密码），真实用户手机号不会与其冲突
        AppUser admin = AppUser.of(ADMIN_PLACEHOLDER_PHONE, username,
                passwordHasher.encode(adminProperties.password()), username, "c0", AppUser.ROLE_ADMIN);
        userMapper.insert(admin);
        ensureProfile(admin);
        log.info("已创建管理员账号 {}（默认密码见 arechat.admin.password 配置，请尽快修改）", username);
    }

    /** 管理员占位手机号：仅供数据库唯一约束使用，不参与登录 */
    static final String ADMIN_PLACEHOLDER_PHONE = "00000000000";

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
