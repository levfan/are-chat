package com.smart.chat.im;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 收藏夹：跨会话查看自己收藏的消息。 */
@RestController
@RequestMapping("/api/stars")
public class StarsController {

    private final PrivateMessageService messageService;

    public StarsController(PrivateMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public ApiResponse<List<PrivateMessageService.StarVO>> list(HttpSession session) {
        return ApiResponse.ok(messageService.listStars(Sessions.requireUser(session)));
    }
}
