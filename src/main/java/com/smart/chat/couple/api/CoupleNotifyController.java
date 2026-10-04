package com.smart.chat.couple.api;

import com.smart.chat.couple.application.CoupleNotifyService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * F41 情侣空间通知中心：事件补看 / 全部已读。
 */
@RestController
@RequestMapping("/api/couple/notify")
public class CoupleNotifyController {

    private final CoupleNotifyService notifyService;

    public CoupleNotifyController(CoupleNotifyService notifyService) {
        this.notifyService = notifyService;
    }

    /** 我的最近 50 条通知 + 未读数。 */
    @GetMapping
    public ApiResponse<CoupleNotifyService.NotifyListVO> mine(HttpSession session) {
        return ApiResponse.ok(notifyService.mine(Sessions.requireUser(session)));
    }

    /** 全部标记已读。 */
    @PostMapping("/read-all")
    public ApiResponse<Void> readAll(HttpSession session) {
        notifyService.markAllRead(Sessions.requireUser(session));
        return ApiResponse.ok();
    }
}
