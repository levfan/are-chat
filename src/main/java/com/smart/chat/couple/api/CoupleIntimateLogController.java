package com.smart.chat.couple.api;

import com.smart.chat.couple.application.CoupleIntimateLogService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 亲密记录（新卡 `couple-intimate`）：记一笔边缘亲昵/性生活、删一笔。
 * 三个接口都返回整份看板，前端拿最新统计直接替换。
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

    /** 亲密看板：最近记录 + 最近一次 + 本月/累计次数。 */
    @GetMapping("/board")
    public ApiResponse<CoupleIntimateLogService.IntimateBoardVO> board(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
    }

    /** 记一笔。 */
    @PostMapping("/add")
    public ApiResponse<CoupleIntimateLogService.IntimateBoardVO> add(@RequestBody AddRequest req,
                                                                     HttpSession session) {
        return ApiResponse.ok(service.add(Sessions.requireUser(session), req.day(), req.kind(), req.note()));
    }

    /** 删掉一笔错记。 */
    @PostMapping("/remove")
    public ApiResponse<CoupleIntimateLogService.IntimateBoardVO> remove(@RequestBody RecordIdRequest req,
                                                                        HttpSession session) {
        return ApiResponse.ok(service.remove(Sessions.requireUser(session), req.id()));
    }
}
