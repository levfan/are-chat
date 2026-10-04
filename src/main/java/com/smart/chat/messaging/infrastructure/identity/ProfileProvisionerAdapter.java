package com.smart.chat.messaging.infrastructure.identity;

import com.smart.chat.identity.domain.ProfileProvisioner;
import com.smart.chat.messaging.infrastructure.persistence.UserProfilePO;
import com.smart.chat.messaging.infrastructure.persistence.UserProfileMapper;
import org.springframework.stereotype.Component;

/** 资料卡建档：实现 identity 声明的端口（资料卡数据本来就归 messaging）。 */
@Component
public class ProfileProvisionerAdapter implements ProfileProvisioner {

    private final UserProfileMapper profileMapper;

    public ProfileProvisionerAdapter(UserProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
    }

    @Override
    public void provision(String username, String nickname, String avatar) {
        if (profileMapper.selectById(username) != null) {
            return;
        }
        UserProfilePO profile = new UserProfilePO();
        profile.setUsername(username);
        profile.setNickname(nickname);
        profile.setSignature("");
        profile.setAvatar(avatar);
        profile.setPresenceStatus("online");
        profile.setUpdatedAt(System.currentTimeMillis());
        profileMapper.insert(profile);
    }
}
