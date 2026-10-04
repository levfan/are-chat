package com.smart.chat.messaging.infrastructure.identity;

import com.smart.chat.messaging.domain.friend.FriendRepository;
import com.smart.chat.identity.domain.AccountCascade;
import org.springframework.stereotype.Component;

/** 账号注销时摘除双向好友关系（好友边数据归 messaging，清理自然也归它）。 */
@Component
public class FriendGraphCascade implements AccountCascade {

    private final FriendRepository friendRepository;

    public FriendGraphCascade(FriendRepository friendRepository) {
        this.friendRepository = friendRepository;
    }

    @Override
    public void onDeactivated(String username) {
        friendRepository.deleteAllEdgesOf(username);
    }
}
