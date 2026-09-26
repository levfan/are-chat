package com.smart.chat.im;

import com.smart.chat.auth.AppUser;
import com.smart.chat.auth.AppUserService;
import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import com.smart.chat.room.ChatSessionRegistry;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 57 全站在线信息：当前 WebSocket 在线用户列表与人数。
 * 管理员看全站：统计所有在线用户；普通用户只统计自己通讯录好友里的在线人数。
 */
@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    public record OnlineVO(int onlineCount, List<String> users) {
    }

    private final ChatSessionRegistry registry;
    private final AppUserService userService;
    private final FriendMapper friendMapper;

    public PresenceController(ChatSessionRegistry registry, AppUserService userService, FriendMapper friendMapper) {
        this.registry = registry;
        this.userService = userService;
        this.friendMapper = friendMapper;
    }

    @GetMapping("/online")
    public ApiResponse<OnlineVO> online(HttpSession session) {
        String username = Sessions.requireUser(session);
        Set<String> online = registry.onlineUsers();
        List<String> users;
        AppUser user = userService.find(username).orElse(null);
        if (user != null && user.isAdmin()) {
            // 管理员：全站所有在线用户
            users = List.copyOf(online);
        } else {
            // 普通用户：仅通讯录好友中的在线者（自己不算自己的好友）
            Set<String> friends = Set.copyOf(
                    friendMapper.findAllByOwner(username).stream().map(Friend::getFriendUsername).toList());
            users = online.stream().filter(friends::contains).sorted().toList();
        }
        return ApiResponse.ok(new OnlineVO(users.size(), users));
    }
}
