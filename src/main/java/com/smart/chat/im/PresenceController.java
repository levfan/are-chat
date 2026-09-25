package com.smart.chat.im;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import com.smart.chat.room.ChatSessionRegistry;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 57 全站在线信息：当前 WebSocket 在线用户列表与人数。 */
@RestController
@RequestMapping("/api/presence")
public class PresenceController {

    public record OnlineVO(int onlineCount, List<String> users) {
    }

    private final ChatSessionRegistry registry;

    public PresenceController(ChatSessionRegistry registry) {
        this.registry = registry;
    }

    @GetMapping("/online")
    public ApiResponse<OnlineVO> online(HttpSession session) {
        Sessions.requireUser(session);
        List<String> users = List.copyOf(registry.onlineUsers());
        return ApiResponse.ok(new OnlineVO(users.size(), users));
    }
}
