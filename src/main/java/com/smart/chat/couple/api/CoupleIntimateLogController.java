package com.smart.chat.couple.api;

import com.smart.chat.couple.application.CoupleIntimateLogService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 亲密记录的写口（在日历卡 `couple-calendar` 上操作）：记一笔边缘亲昵/性生活、删一笔。
 * 两个接口都只回成功与否——展示数据统一由 {@code GET /api/couple/calendar/month} 下发，
 * 前端写完重拉当月（原 {@code GET /intimate/board} 已随卡片合并下线）。
 */
@RestController
@RequestMapping("/api/couple/intimate")
public class CoupleIntimateLogController {

    /** 日期 yyyy-MM-dd；类型 EDGE/SEX；备注选填。 */
    public record AddRequest(String day, String kind, String note) {
    }

    /** 记录 id。 */
    public record RecordIdRequest(String id) {
    }

    private final CoupleIntimateLogService service;

    public CoupleIntimateLogController(CoupleIntimateLogService service) {
        this.service = service;
    }

    /** 记一笔。 */
    @PostMapping("/add")
    public ApiResponse<Void> add(@RequestBody AddRequest req, HttpSession session) {
        service.add(Sessions.requireUser(session), req.day(), req.kind(), req.note());
        return ApiResponse.ok();
    }

    /** 删掉一笔错记。 */
    @PostMapping("/remove")
    public ApiResponse<Void> remove(@RequestBody RecordIdRequest req, HttpSession session) {
        service.remove(Sessions.requireUser(session), req.id());
        return ApiResponse.ok();
    }
}
