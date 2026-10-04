package com.smart.chat.messaging.application;

import com.smart.chat.messaging.domain.friend.FriendRepository;
import org.springframework.stereotype.Component;

/**
 * 最近在线：WS 连接建立/断开时刷新该用户在所有好友列表里的 last_seen_at，
 * 前端据此显示「x 分钟前在线」。
 * <p>
 * 这是一趟批量 UPDATE（一次改很多人的行），所以它经 {@link FriendRepository#touchLastSeenOf}
 * 而不是「取聚合—改—存回」：把在线位交给聚合的 save 去写，就会和 {@code Friend} 的
 * 「只回写自己那几列」纪律打架。
 */
@Component
public class PresenceService {

    private final FriendRepository friendRepository;

    public PresenceService(FriendRepository friendRepository) {
        this.friendRepository = friendRepository;
    }

    public void touch(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        friendRepository.touchLastSeenOf(username, System.currentTimeMillis());
    }
}
