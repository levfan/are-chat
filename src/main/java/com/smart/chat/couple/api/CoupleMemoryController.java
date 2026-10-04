package com.smart.chat.couple.api;

import com.smart.chat.couple.application.CoupleMemoryService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 隐藏彩蛋页（连续 100 天解锁）：回顾时间轴 + 一句话总结。
 * 闸门在 {@code CoupleMemoryService} 里，未解锁直接 400，不靠前端藏页签兜。
 */
@RestController
@RequestMapping("/api/couple/memory")
public class CoupleMemoryController {

    private final CoupleMemoryService service;

    public CoupleMemoryController(CoupleMemoryService service) {
        this.service = service;
    }

    /** 百日回顾页内容。 */
    @GetMapping("/page")
    public ApiResponse<CoupleMemoryService.MemoryVO> page(HttpSession session) {
        return ApiResponse.ok(service.memory(Sessions.requireUser(session)));
    }
}
