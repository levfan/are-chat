package com.smart.chat.platform.api;

import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.platform.application.AnnouncementService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 88 公告管理：原先长在 identity 的 AdminController 里，路由保持 `/api/admin/announcements*` 不变，
 * 但归属挪回拥有公告数据的 platform——公告的新建/关闭/列表都不该由账号上下文代办。
 */
@RestController
@RequestMapping("/api/admin")
public class AnnouncementAdminController {

    public record AnnouncementRequest(String content) {
    }

    private final AnnouncementService announcementService;
    private final AccountDirectory accounts;

    public AnnouncementAdminController(AnnouncementService announcementService, AccountDirectory accounts) {
        this.announcementService = announcementService;
        this.accounts = accounts;
    }

    @PostMapping("/announcements")
    public ApiResponse<AnnouncementService.AnnouncementVO> publish(@RequestBody AnnouncementRequest req,
                                                                   HttpSession session) {
        String actor = requireAdmin(session);
        return ApiResponse.ok(announcementService.publish(actor, req == null ? null : req.content()));
    }

    @PostMapping("/announcements/{id}/close")
    public ApiResponse<Void> close(@PathVariable String id, HttpSession session) {
        String actor = requireAdmin(session);
        announcementService.close(actor, id);
        return ApiResponse.ok();
    }

    @GetMapping("/announcements")
    public ApiResponse<List<AnnouncementService.AnnouncementAdminVO>> announcements(HttpSession session) {
        requireAdmin(session);
        return ApiResponse.ok(announcementService.all());
    }

    private String requireAdmin(HttpSession session) {
        String username = Sessions.requireUser(session);
        accounts.requireAdmin(username);
        return username;
    }
}
