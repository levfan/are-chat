package com.smart.chat.couple.api;

import com.smart.chat.couple.application.CouplePeriodService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 经期记录的写口（在日历卡 `couple-calendar` 上操作）：记开始日、补结束日、删错记。
 * 三个接口都只回成功与否——展示数据统一由 {@code GET /api/couple/calendar/month} 下发，
 * 前端写完重拉当月，不在这里再拼一份经期看板（原 {@code GET /period/board} 已随卡片合并下线）。
 */
@RestController
@RequestMapping("/api/couple/period")
public class CouplePeriodController {

    /** 开始日：yyyy-MM-dd，不能是将来。 */
    public record AddRequest(String startDay) {
    }

    /** 记录 id。 */
    public record RecordIdRequest(String id) {
    }

    private final CouplePeriodService service;

    public CouplePeriodController(CouplePeriodService service) {
        this.service = service;
    }

    /** 记一个开始日。 */
    @PostMapping("/add")
    public ApiResponse<Void> add(@RequestBody AddRequest req, HttpSession session) {
        service.add(Sessions.requireUser(session), req.startDay());
        return ApiResponse.ok();
    }

    /** 给某次还没结束的经期补上今天的结束日（后端只认今天，历史区间改不了）。 */
    @PostMapping("/end")
    public ApiResponse<Void> end(@RequestBody RecordIdRequest req, HttpSession session) {
        service.end(Sessions.requireUser(session), req.id());
        return ApiResponse.ok();
    }

    /** 删掉一条错记。 */
    @PostMapping("/remove")
    public ApiResponse<Void> remove(@RequestBody RecordIdRequest req, HttpSession session) {
        service.remove(Sessions.requireUser(session), req.id());
        return ApiResponse.ok();
    }
}
