package com.smart.chat.messaging.api;

import com.smart.chat.messaging.infrastructure.persistence.FriendMapper;
import com.smart.chat.messaging.infrastructure.persistence.UserProfile;
import com.smart.chat.messaging.infrastructure.persistence.UserProfileMapper;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 个人资料：预设 emoji 头像 + 昵称 + 个性签名 + 生日；好友资料卡仅好友可见。
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    public record ProfileVO(String username, String nickname, String signature, String avatar, String presenceStatus,
                            String birthday) {
        static ProfileVO of(UserProfile p) {
            return new ProfileVO(p.getUsername(), p.getNickname(), p.getSignature(), p.getAvatar(),
                    p.getPresenceStatus() == null ? "online" : p.getPresenceStatus(), p.getBirthday());
        }
    }

    public record UpdateProfileRequest(String nickname, String signature, String avatar, String presenceStatus,
                                       String birthday) {
    }

    /** F42 好友生日条目：daysUntil 为今年生日的剩余天数（今天 = 0）。 */
    public record FriendBirthdayVO(String username, String nickname, String birthday, long daysUntil, boolean today) {
    }

    private final UserProfileMapper profileMapper;
    private final FriendMapper friendMapper;
    private final AccountDirectory accounts;

    public ProfileController(UserProfileMapper profileMapper, FriendMapper friendMapper, AccountDirectory accounts) {
        this.profileMapper = profileMapper;
        this.friendMapper = friendMapper;
        this.accounts = accounts;
    }

    @GetMapping
    public ApiResponse<ProfileVO> me(HttpSession session) {
        String username = Sessions.requireUser(session);
        return ApiResponse.ok(ProfileVO.of(ensureProfile(username)));
    }

    /** 好友资料卡：仅好友可查看对方的昵称/签名/头像。 */
    @GetMapping("/{username}")
    public ApiResponse<ProfileVO> ofFriend(@PathVariable String username, HttpSession session) {
        String me = Sessions.requireUser(session);
        if (friendMapper.findByOwnerAndFriend(me, username).isEmpty()) {
            throw new BusinessException(403, "只有好友才能查看资料卡");
        }
        return ApiResponse.ok(ProfileVO.of(ensureProfile(username)));
    }

    @PutMapping
    public ApiResponse<ProfileVO> update(@RequestBody UpdateProfileRequest req, HttpSession session) {
        String username = Sessions.requireUser(session);
        UserProfile profile = ensureProfile(username);
        if (req.nickname() != null) {
            String nickname = req.nickname().trim();
            if (nickname.isEmpty() || nickname.length() > 32) {
                throw new BusinessException(400, "昵称需为 1~32 个字");
            }
            profile.setNickname(nickname);
            // 登录后修改昵称：同步 app_user（auth/me、管理后台用户列表显示的就是这里的昵称）
            accounts.updateNickname(username, nickname);
        }
        if (req.signature() != null) {
            String signature = req.signature().trim();
            if (signature.length() > 100) {
                throw new BusinessException(400, "签名最长 100 个字");
            }
            profile.setSignature(signature);
        }
        if (req.avatar() != null && !req.avatar().isBlank()) {
            String avatar = req.avatar().trim();
            if (avatar.length() > 8) {
                throw new BusinessException(400, "头像请从预设色档中选择");
            }
            profile.setAvatar(avatar);
        }
        if (req.presenceStatus() != null && !req.presenceStatus().isBlank()) {
            String status = req.presenceStatus().trim();
            if (!Set.of("online", "busy", "away").contains(status)) {
                throw new BusinessException(400, "在线状态仅支持 online / busy / away");
            }
            profile.setPresenceStatus(status);
        }
        // F42 生日：yyyy-MM-dd 或 MM-dd，空串 = 清除
        if (req.birthday() != null) {
            String birthday = req.birthday().trim();
            if (birthday.isEmpty()) {
                profile.setBirthday(null);
            } else {
                validateBirthday(birthday);
                profile.setBirthday(birthday);
            }
        }
        profile.setUpdatedAt(System.currentTimeMillis());
        profileMapper.updateById(profile);
        return ApiResponse.ok(ProfileVO.of(profile));
    }

    /** F42 好友生日列表：填了生日的好友，按今年剩余天数升序（今天生日的排最前）。 */
    @GetMapping("/friends-birthdays")
    public ApiResponse<List<FriendBirthdayVO>> friendsBirthdays(HttpSession session) {
        String me = Sessions.requireUser(session);
        List<FriendBirthdayVO> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        var friends = friendMapper.findAllByOwner(me);
        // 资料一次批量取：原先每个好友 selectById 一次，好友越多这个接口越慢（N+1）
        List<String> peers = friends.stream().map(f -> f.getFriendUsername()).toList();
        Map<String, UserProfile> profileMap = peers.isEmpty() ? Map.of()
                : profileMapper.selectBatchIds(peers).stream()
                        .collect(Collectors.toMap(UserProfile::getUsername, p -> p));
        for (var friend : friends) {
            UserProfile profile = profileMap.get(friend.getFriendUsername());
            String birthday = profile == null ? null : profile.getBirthday();
            if (birthday == null || birthday.isBlank()) {
                continue;
            }
            String monthDay = birthday.length() >= 10 ? birthday.substring(5) : birthday;
            LocalDate next;
            try {
                next = LocalDate.parse(today.getYear() + "-" + monthDay, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                continue;
            }
            if (next.isBefore(today)) {
                next = next.plusYears(1);
            }
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, next);
            String nickname = profile.getNickname();
            list.add(new FriendBirthdayVO(friend.getFriendUsername(),
                    nickname == null || nickname.isBlank() ? friend.getFriendUsername() : nickname,
                    birthday, days, days == 0));
        }
        list.sort((a, b) -> Long.compare(a.daysUntil(), b.daysUntil()));
        return ApiResponse.ok(list);
    }

    /** 生日格式校验：yyyy-MM-dd 或 MM-dd。 */
    private void validateBirthday(String birthday) {
        boolean ok = false;
        try {
            LocalDate.parse(birthday, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            ok = true;
        } catch (Exception ignored) {
            // 尝试下一种格式
        }
        if (!ok) {
            try {
                LocalDate.parse("2000-" + birthday, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                ok = birthday.length() == 5;
            } catch (Exception ignored) {
                // 保持 false
            }
        }
        if (!ok) {
            throw new BusinessException(400, "生日格式应为 yyyy-MM-dd 或 MM-dd");
        }
    }

    private UserProfile ensureProfile(String username) {
        UserProfile profile = profileMapper.selectById(username);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUsername(username);
            profile.setNickname(username);
            profile.setSignature("");
            profile.setAvatar("c0");
            profile.setPresenceStatus("online");
            profile.setUpdatedAt(System.currentTimeMillis());
            profileMapper.insert(profile);
        }
        if (profile.getPresenceStatus() == null) {
            profile.setPresenceStatus("online");
        }
        return profile;
    }
}
