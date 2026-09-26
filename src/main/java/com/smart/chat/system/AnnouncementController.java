package com.smart.chat.system;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 88 全站公告用户端：当前生效公告 + 已读标记。
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping("/current")
    public ApiResponse<AnnouncementService.AnnouncementVO> current(HttpSession session) {
        String username = Sessions.requireUser(session);
        return ApiResponse.ok(announcementService.current(username));
    }

    @PostMapping("/{id}/read")
    public ApiResponse<Void> read(@PathVariable String id, HttpSession session) {
        String username = Sessions.requireUser(session);
        announcementService.markRead(username, id);
        return ApiResponse.ok();
    }
}
