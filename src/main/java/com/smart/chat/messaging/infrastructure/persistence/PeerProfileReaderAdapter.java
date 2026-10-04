package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.PeerProfileReader;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** 资料卡视图适配器：把 UserProfilePO 关在 messaging 内部。 */
@Component
public class PeerProfileReaderAdapter implements PeerProfileReader {

    private final UserProfileMapper profileMapper;

    public PeerProfileReaderAdapter(UserProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
    }

    @Override
    public Optional<PeerProfile> read(String username) {
        UserProfilePO profile = profileMapper.selectById(username);
        if (profile == null) {
            return Optional.empty();
        }
        return Optional.of(new PeerProfile(profile.getUsername(), profile.getNickname(),
                profile.getAvatar(), profile.getBirthday()));
    }
}
