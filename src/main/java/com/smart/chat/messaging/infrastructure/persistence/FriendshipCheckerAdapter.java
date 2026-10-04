package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.FriendshipChecker;
import com.smart.chat.messaging.domain.friend.FriendRepository;
import org.springframework.stereotype.Component;

/**
 * 好友判定适配器：只回答是/否，不把 {@code FriendPO} 与 Mapper 交出去。
 * <p>
 * 走 {@link FriendRepository} 而不是直接摸 Mapper——同一张表只留一条读路径，
 * 「是不是好友」和「取那条边」用的是同一份口径。
 */
@Component
public class FriendshipCheckerAdapter implements FriendshipChecker {

    private final FriendRepository friendRepository;

    public FriendshipCheckerAdapter(FriendRepository friendRepository) {
        this.friendRepository = friendRepository;
    }

    @Override
    public boolean areFriends(String owner, String friend) {
        return friendRepository.findByOwnerAndFriend(owner, friend).isPresent();
    }
}
