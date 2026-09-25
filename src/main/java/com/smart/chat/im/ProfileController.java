package com.smart.chat.im;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.BusinessException;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * 个人资料：预设 emoji 头像 + 昵称 + 个性签名；好友资料卡仅好友可见。
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    public record ProfileVO(String username, String nickname, String signature, String avatar, String presenceStatus) {
        static ProfileVO of(UserProfile p) {
            return new ProfileVO(p.getUsername(), p.getNickname(), p.getSignature(), p.getAvatar(),
                    p.getPresenceStatus() == null ? "online" : p.getPresenceStatus());
        }
    }

    public record UpdateProfileRequest(String nickname, String signature, String avatar, String presenceStatus) {
    }

    private final UserProfileMapper profileMapper;
    private final FriendMapper friendMapper;

    public ProfileController(UserProfileMapper profileMapper, FriendMapper friendMapper) {
        this.profileMapper = profileMapper;
        this.friendMapper = friendMapper;
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
        profile.setUpdatedAt(System.currentTimeMillis());
        profileMapper.updateById(profile);
        return ApiResponse.ok(ProfileVO.of(profile));
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
