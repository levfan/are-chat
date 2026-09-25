package com.smart.chat.im;

import org.springframework.stereotype.Component;

/**
 * 最近在线：WS 连接建立/断开时刷新该用户在所有好友列表里的 last_seen_at，
 * 前端据此显示「x 分钟前在线」。
 */
@Component
public class PresenceService {

    private final FriendMapper friendMapper;

    public PresenceService(FriendMapper friendMapper) {
        this.friendMapper = friendMapper;
    }

    public void touch(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        friendMapper.updateLastSeenByUsername(username, System.currentTimeMillis());
    }
}
