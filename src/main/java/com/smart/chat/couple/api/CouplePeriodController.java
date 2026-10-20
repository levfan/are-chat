package com.smart.chat.couple.api;

import com.smart.chat.couple.application.CouplePeriodService;
import com.smart.chat.sharedkernel.web.ApiResponse;
import com.smart.chat.sharedkernel.web.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 经期记录（新卡 `couple-period`）：记开始日、补结束日、删错记。
 * 四个接口都返回整份看板，前端拿最新周期与预测直接替换。
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

    /** 经期看板：全部记录 + 平均周期 + 下次预计。 */
    @GetMapping("/board")
    public ApiResponse<CouplePeriodService.PeriodBoardVO> board(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
    }

    /** 记一个开始日。 */
    @PostMapping("/add")
    public ApiResponse<CouplePeriodService.PeriodBoardVO> add(@RequestBody AddRequest req, HttpSession session) {
        return ApiResponse.ok(service.add(Sessions.requireUser(session), req.startDay()));
    }

    /** 给最近一次还没结束的记录补上今天的结束日。 */
    @PostMapping("/end")
    public ApiResponse<CouplePeriodService.PeriodBoardVO> end(@RequestBody RecordIdRequest req, HttpSession session) {
        return ApiResponse.ok(service.end(Sessions.requireUser(session), req.id()));
    }

    /** 删掉一条错记。 */
    @PostMapping("/remove")
    public ApiResponse<CouplePeriodService.PeriodBoardVO> remove(@RequestBody RecordIdRequest req,
                                                                 HttpSession session) {
        return ApiResponse.ok(service.remove(Sessions.requireUser(session), req.id()));
    }
}
