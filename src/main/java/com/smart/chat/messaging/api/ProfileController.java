package com.smart.chat.messaging.api;

import com.smart.chat.messaging.domain.friend.Friend;
import com.smart.chat.messaging.domain.friend.FriendRepository;
import com.smart.chat.messaging.domain.friend.FriendshipGate;
import com.smart.chat.messaging.domain.profile.UserProfile;
import com.smart.chat.messaging.domain.profile.UserProfileRepository;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.smart.chat.messaging.application.DomainRules.guard;

/**
 * 个人资料：预设 emoji 头像 + 昵称 + 个性签名 + 生日；好友资料卡仅好友可见。
 * <p>
 * 字段的取值范围（昵称 1~32、签名 ≤100、头像只能选预设色档、在线状态三档、生日两种格式）
 * 是 {@link UserProfile} 的规则，这里只负责「请求里没传的字段不动」和投影响应。
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    public record ProfileVO(String username, String nickname, String signature, String avatar, String presenceStatus,
                            String birthday) {
        static ProfileVO of(UserProfile p) {
            return new ProfileVO(p.username(), p.nickname(), p.signature(), p.avatar(), p.presenceStatusForView(),
                    p.birthday());
        }
    }

    public record UpdateProfileRequest(String nickname, String signature, String avatar, String presenceStatus,
                                       String birthday) {
    }

    /** F42 好友生日条目：daysUntil 为今年生日的剩余天数（今天 = 0）。 */
    public record FriendBirthdayVO(String username, String nickname, String birthday, long daysUntil, boolean today) {
    }

    private final UserProfileRepository profileRepository;
    private final FriendRepository friendRepository;
    private final AccountDirectory accounts;

    public ProfileController(UserProfileRepository profileRepository, FriendRepository friendRepository,
                             AccountDirectory accounts) {
        this.profileRepository = profileRepository;
        this.friendRepository = friendRepository;
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
        guard(() -> FriendshipGate.requireProfileCardVisible(
                friendRepository.findByOwnerAndFriend(me, username).isPresent()));
        return ApiResponse.ok(ProfileVO.of(ensureProfile(username)));
    }

    @PutMapping
    public ApiResponse<ProfileVO> update(@RequestBody UpdateProfileRequest req, HttpSession session) {
        String username = Sessions.requireUser(session);
        UserProfile profile = ensureProfile(username);
        if (req.nickname() != null) {
            guard(() -> profile.changeNickname(req.nickname()));
            // 登录后修改昵称：同步 app_user（auth/me、管理后台用户列表显示的就是这里的昵称）
            accounts.updateNickname(username, profile.nickname());
        }
        if (req.signature() != null) {
            guard(() -> profile.changeSignature(req.signature()));
        }
        if (req.avatar() != null && !req.avatar().isBlank()) {
            guard(() -> profile.changeAvatar(req.avatar()));
        }
        if (req.presenceStatus() != null && !req.presenceStatus().isBlank()) {
            guard(() -> profile.changePresenceStatus(req.presenceStatus()));
        }
        // F42 生日：yyyy-MM-dd 或 MM-dd，空串 = 清除
        if (req.birthday() != null) {
            guard(() -> profile.changeBirthday(req.birthday()));
        }
        profile.markUpdated(System.currentTimeMillis());
        profileRepository.save(profile);
        return ApiResponse.ok(ProfileVO.of(profile));
    }

    /** F42 好友生日列表：填了生日的好友，按今年剩余天数升序（今天生日的排最前）。 */
    @GetMapping("/friends-birthdays")
    public ApiResponse<List<FriendBirthdayVO>> friendsBirthdays(HttpSession session) {
        String me = Sessions.requireUser(session);
        List<FriendBirthdayVO> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        List<Friend> friends = friendRepository.findAllByOwner(me);
        // 资料一次批量取：原先每个好友 selectById 一次，好友越多这个接口越慢（N+1）
        List<String> peers = friends.stream().map(Friend::friendUsername).toList();
        Map<String, UserProfile> profileMap = peers.isEmpty() ? Map.of()
                : profileRepository.listByUsernames(peers).stream()
                        .collect(Collectors.toMap(UserProfile::username, p -> p));
        for (Friend friend : friends) {
            UserProfile profile = profileMap.get(friend.friendUsername());
            if (profile == null) {
                continue;
            }
            var daysUntil = profile.daysUntilBirthday(today);
            if (daysUntil.isEmpty()) {
                continue;
            }
            long days = daysUntil.getAsLong();
            String nickname = profile.nickname();
            list.add(new FriendBirthdayVO(friend.friendUsername(),
                    nickname == null || nickname.isBlank() ? friend.friendUsername() : nickname,
                    profile.birthday(), days, days == 0));
        }
        list.sort((a, b) -> Long.compare(a.daysUntil(), b.daysUntil()));
        return ApiResponse.ok(list);
    }

    /** 老用户可能还没有资料行：第一次读到就按建档口径补一行（改造前后完全一致）。 */
    private UserProfile ensureProfile(String username) {
        UserProfile profile = profileRepository.find(username)
                .orElseGet(() -> {
                    UserProfile fresh = UserProfile.provision(username, username, UserProfile.DEFAULT_AVATAR,
                            System.currentTimeMillis());
                    profileRepository.save(fresh);
                    return fresh;
                });
        profile.fillPresenceStatusIfMissing();
        return profile;
    }
}
