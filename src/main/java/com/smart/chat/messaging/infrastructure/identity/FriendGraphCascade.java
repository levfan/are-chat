package com.smart.chat.messaging.infrastructure.identity;

import com.smart.chat.messaging.infrastructure.persistence.FriendMapper;
import com.smart.chat.identity.domain.AccountCascade;
import org.springframework.stereotype.Component;

/** 账号注销时摘除双向好友关系（好友边数据归 messaging，清理自然也归它）。 */
@Component
public class FriendGraphCascade implements AccountCascade {

    private final FriendMapper friendMapper;

    public FriendGraphCascade(FriendMapper friendMapper) {
        this.friendMapper = friendMapper;
    }

    @Override
    public void onDeactivated(String username) {
        friendMapper.deleteAllByOwner(username);
        friendMapper.deleteAllByFriend(username);
    }
}
