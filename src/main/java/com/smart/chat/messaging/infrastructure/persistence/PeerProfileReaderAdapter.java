package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.PeerProfileReader;
import com.smart.chat.messaging.domain.profile.UserProfile;
import com.smart.chat.messaging.domain.profile.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 资料卡视图适配器：把 {@code UserProfilePO} 关在 messaging 内部，对外只发发布语言
 * （{@link PeerProfileReader.PeerProfile} 那四个字段）。
 */
@Component
public class PeerProfileReaderAdapter implements PeerProfileReader {

    private final UserProfileRepository profileRepository;

    public PeerProfileReaderAdapter(UserProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public Optional<PeerProfile> read(String username) {
        return profileRepository.find(username).map(PeerProfileReaderAdapter::toView);
    }

    private static PeerProfile toView(UserProfile profile) {
        return new PeerProfile(profile.username(), profile.nickname(), profile.avatar(), profile.birthday());
    }
}
