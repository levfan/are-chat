package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.FriendshipChecker;
import org.springframework.stereotype.Component;

/** 好友判定适配器：只回答是/否，不把 FriendPO 与 Mapper 交出去。 */
@Component
public class FriendshipCheckerAdapter implements FriendshipChecker {

    private final FriendMapper friendMapper;

    public FriendshipCheckerAdapter(FriendMapper friendMapper) {
        this.friendMapper = friendMapper;
    }

    @Override
    public boolean areFriends(String owner, String friend) {
        return friendMapper.findByOwnerAndFriend(owner, friend).isPresent();
    }
}
