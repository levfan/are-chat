package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 我们公司（F240-F249）：头衔任命/董事会决议/年度述职/发薪日/金点子/会议签到/职级公示/公司名片/公司周报。 */
@RestController
@RequestMapping("/api/couple/board")
public class CoupleBoardController {

    private final CoupleBoardService service;

    public CoupleBoardController(CoupleBoardService service) {
        this.service = service;
    }

    /** 我们公司总览。 */
    @GetMapping("/overview")
    public ApiResponse<CoupleBoardService.OverviewVO> overview(HttpSession session) {
        return ApiResponse.ok(service.overview(Sessions.requireUser(session)));
    }

    /** F240 给 TA 封一个职位。 */
    public record RoleRequest(String title) {
    }

    @PostMapping("/role")
    public ApiResponse<CoupleBoardService.OverviewVO> role(@RequestBody RoleRequest req, HttpSession session) {
        return ApiResponse.ok(service.proposeRole(Sessions.requireUser(session), req.title()));
    }

    /** F240 被任命者盖章上任。 */
    public record IdRequest(String id) {
    }

    @PostMapping("/role/appoint")
    public ApiResponse<CoupleBoardService.OverviewVO> appoint(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.appoint(Sessions.requireUser(session), req.id()));
    }

    /** F241 提交决议议案。 */
    public record VoteRequest(String title) {
    }

    @PostMapping("/vote")
    public ApiResponse<CoupleBoardService.OverviewVO> votePropose(@RequestBody VoteRequest req, HttpSession session) {
        return ApiResponse.ok(service.proposeVote(Sessions.requireUser(session), req.title()));
    }

    /** F241 表决：附议通过或一票否决。 */
    public record DecideRequest(String id, Boolean agree) {
    }

    @PostMapping("/vote/decide")
    public ApiResponse<CoupleBoardService.OverviewVO> decide(@RequestBody DecideRequest req, HttpSession session) {
        return ApiResponse.ok(service.vote(Sessions.requireUser(session), req.id(),
                req.agree() != null && req.agree()));
    }

    /** F242 交年度述职+小目标。 */
    public record ReportRequest(String year, String review, String goal) {
    }

    @PostMapping("/report")
    public ApiResponse<CoupleBoardService.OverviewVO> report(@RequestBody ReportRequest req, HttpSession session) {
        return ApiResponse.ok(service.saveReport(Sessions.requireUser(session), req.year(), req.review(), req.goal()));
    }

    /** F244 发本月感谢工资（+5 积分入账）。 */
    public record SalaryRequest(String thanks) {
    }

    @PostMapping("/salary")
    public ApiResponse<CoupleBoardService.OverviewVO> salary(@RequestBody SalaryRequest req, HttpSession session) {
        return ApiResponse.ok(service.paySalary(Sessions.requireUser(session), req.thanks()));
    }

    /** F245 投一条金点子。 */
    public record IdeaRequest(String content) {
    }

    @PostMapping("/idea")
    public ApiResponse<CoupleBoardService.OverviewVO> idea(@RequestBody IdeaRequest req, HttpSession session) {
        return ApiResponse.ok(service.addIdea(Sessions.requireUser(session), req.content()));
    }

    /** F245 采纳 TA 的点子转决议。 */
    @PostMapping("/idea/adopt")
    public ApiResponse<CoupleBoardService.OverviewVO> adopt(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.adoptIdea(Sessions.requireUser(session), req.id()));
    }

    /** F247 例会签到（10s 窗口双签到召开会议）。 */
    @PostMapping("/attend")
    public ApiResponse<CoupleBoardService.OverviewVO> attend(HttpSession session) {
        return ApiResponse.ok(service.attend(Sessions.requireUser(session)));
    }
}
