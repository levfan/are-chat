package com.smart.chat.messaging.domain.profile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 资料卡的仓储端口。
 * <p>
 * {@link #listByUsernames} 是给「联系人列表 / 好友生日表」这类页面准备的批量读法：
 * 逐人 {@code find} 会退化成 N+1，改造前已经用 selectBatchIds 收掉，不许退回去。
 */
public interface UserProfileRepository {

    Optional<UserProfile> find(String username);

    List<UserProfile> listByUsernames(Collection<String> usernames);

    /** 没有资料行就插入，已有就只回写聚合持有的列 */
    void save(UserProfile profile);
}
