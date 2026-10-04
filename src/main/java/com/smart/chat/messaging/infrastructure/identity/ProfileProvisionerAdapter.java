package com.smart.chat.messaging.infrastructure.identity;

import com.smart.chat.messaging.domain.profile.UserProfile;
import com.smart.chat.messaging.domain.profile.UserProfileRepository;
import com.smart.chat.identity.domain.ProfileProvisioner;
import org.springframework.stereotype.Component;

/**
 * 资料卡建档：实现 identity 声明的端口（资料卡数据本来就归 messaging）。
 * 已有资料的人不重置——建档口径（签名空串、在线 online）由 {@link UserProfile#provision} 说一次。
 */
@Component
public class ProfileProvisionerAdapter implements ProfileProvisioner {

    private final UserProfileRepository profileRepository;

    public ProfileProvisionerAdapter(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public void provision(String username, String nickname, String avatar) {
        if (profileRepository.find(username).isPresent()) {
            return;
        }
        profileRepository.save(UserProfile.provision(username, nickname, avatar, System.currentTimeMillis()));
    }
}
