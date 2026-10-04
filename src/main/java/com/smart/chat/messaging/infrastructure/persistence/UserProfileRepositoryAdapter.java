package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.profile.UserProfile;
import com.smart.chat.messaging.domain.profile.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@link UserProfileRepository} 的 MyBatis-Plus 适配器。
 * <p>
 * 主键就是 {@code username}（{@code @TableId(IdType.INPUT)}），所以「有没有这一行」不能靠 id 判断，
 * 只能先 {@code selectById}：没有就按建档口径插入，有就只回写聚合持有的六列。
 */
@Component
public class UserProfileRepositoryAdapter implements UserProfileRepository {

    private final UserProfileMapper profileMapper;

    public UserProfileRepositoryAdapter(UserProfileMapper profileMapper) {
        this.profileMapper = profileMapper;
    }

    @Override
    public Optional<UserProfile> find(String username) {
        return Optional.ofNullable(profileMapper.selectById(username)).map(UserProfileRepositoryAdapter::toDomain);
    }

    @Override
    public List<UserProfile> listByUsernames(Collection<String> usernames) {
        if (usernames.isEmpty()) {
            return List.of();
        }
        return profileMapper.selectBatchIds(usernames).stream().map(UserProfileRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(UserProfile profile) {
        UserProfilePO existing = profileMapper.selectById(profile.username());
        if (existing == null) {
            profileMapper.insert(toPO(profile));
            return;
        }
        applyOwnedFields(existing, profile);
        profileMapper.updateById(existing);
    }

    /** username 是主键也是身份，永远不被更新；其余六列都归资料卡聚合管 */
    private static void applyOwnedFields(UserProfilePO po, UserProfile profile) {
        po.setNickname(profile.nickname());
        po.setSignature(profile.signature());
        po.setAvatar(profile.avatar());
        po.setPresenceStatus(profile.presenceStatus());
        po.setBirthday(profile.birthday());
        po.setUpdatedAt(profile.updatedAt());
    }

    private static UserProfilePO toPO(UserProfile profile) {
        UserProfilePO po = new UserProfilePO();
        po.setUsername(profile.username());
        applyOwnedFields(po, profile);
        return po;
    }

    private static UserProfile toDomain(UserProfilePO po) {
        return UserProfile.restore(po.getUsername(), po.getNickname(), po.getSignature(), po.getAvatar(),
                po.getPresenceStatus(), po.getBirthday(), po.getUpdatedAt());
    }
}
