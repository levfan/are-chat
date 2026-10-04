package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.profile.UserProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 资料卡适配器的建档与更新口径：主键就是 username，所以「有没有这一行」只能先查再决定插还是改。 */
@ExtendWith(MockitoExtension.class)
class UserProfileRepositoryAdapterTest {

    @Mock
    private UserProfileMapper profileMapper;

    @InjectMocks
    private UserProfileRepositoryAdapter repository;

    @Test
    void provisioningWritesTheRowTheFirstProfileNeeds() {
        when(profileMapper.selectById("bob")).thenReturn(null);

        repository.save(UserProfile.provision("bob", "bob", "c0", 5L));

        ArgumentCaptor<UserProfilePO> captor = ArgumentCaptor.forClass(UserProfilePO.class);
        verify(profileMapper).insert(captor.capture());
        UserProfilePO po = captor.getValue();
        assertThat(po.getUsername()).isEqualTo("bob");
        assertThat(po.getNickname()).isEqualTo("bob");
        assertThat(po.getSignature()).isEmpty();
        assertThat(po.getAvatar()).isEqualTo("c0");
        assertThat(po.getPresenceStatus()).isEqualTo("online");
        assertThat(po.getUpdatedAt()).isEqualTo(5L);
        verify(profileMapper, never()).updateById(any(UserProfilePO.class));
    }

    @Test
    void updateKeepsTheUsernameAsItWasLoadedAndRewritesOnlyProfileColumns() {
        UserProfilePO existing = new UserProfilePO();
        existing.setUsername("alice");
        existing.setNickname("alice");
        existing.setSignature("");
        existing.setAvatar("c0");
        existing.setPresenceStatus("busy");
        existing.setBirthday("1990-01-02");
        existing.setUpdatedAt(1L);
        when(profileMapper.selectById("alice")).thenReturn(existing);

        UserProfile profile = repository.find("alice").orElseThrow();
        profile.changeNickname("新昵称");
        profile.changePresenceStatus("away");
        repository.save(profile);

        ArgumentCaptor<UserProfilePO> captor = ArgumentCaptor.forClass(UserProfilePO.class);
        verify(profileMapper).updateById(captor.capture());
        UserProfilePO written = captor.getValue();
        assertThat(written.getUsername()).as("主键不参与改写").isEqualTo("alice");
        assertThat(written.getNickname()).isEqualTo("新昵称");
        assertThat(written.getPresenceStatus()).isEqualTo("away");
        assertThat(written.getBirthday()).as("没动的字段保持原值").isEqualTo("1990-01-02");
        assertThat(written.getAvatar()).isEqualTo("c0");
        verify(profileMapper, never()).insert(any(UserProfilePO.class));
    }

    @Test
    void batchReadSkipsTheQueryForEmptyNamesAndMapsTheRest() {
        assertThat(repository.listByUsernames(List.of())).isEmpty();
        verify(profileMapper, never()).selectBatchIds(anyCollection());

        UserProfilePO po = new UserProfilePO();
        po.setUsername("bob");
        po.setNickname("波波");
        po.setPresenceStatus("busy");
        when(profileMapper.selectBatchIds(anyCollection())).thenReturn(List.of(po));

        List<UserProfile> profiles = repository.listByUsernames(List.of("bob"));

        assertThat(profiles).hasSize(1);
        assertThat(profiles.get(0).nickname()).isEqualTo("波波");
        assertThat(profiles.get(0).presenceStatusForView()).isEqualTo("busy");
    }

    @Test
    void missingProfileReadsAsEmptySoCallersCanProvisionIt() {
        when(profileMapper.selectById("ghost")).thenReturn(null);

        assertThat(repository.find("ghost")).isEqualTo(Optional.empty());
    }
}
