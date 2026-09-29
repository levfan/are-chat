package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 共同生活：甜蜜记账本 / 家务轮值 / 约会规划 / 双人习惯 / 暗号小本本。
 */
@RestController
@RequestMapping("/api/couple/life")
public class CoupleLifeController {

    public record ExpenseRequest(long amount, String category, String note, String spentDay) {
    }

    public record ChoreRequest(String title, String rotate) {
    }

    public record DatePlanRequest(String title, String planDay, String place, String items) {
    }

    public record DatePlanDoneRequest(boolean done) {
    }

    public record HabitRequest(String title) {
    }

    public record HabitActiveRequest(boolean active) {
    }

    public record CipherRequest(String keyword, String meaning) {
    }

    private final CoupleLifeService lifeService;

    public CoupleLifeController(CoupleLifeService lifeService) {
        this.lifeService = lifeService;
    }

    // ---------- 甜蜜记账本 ----------

    /** 记一笔开销（amount 单位：分）。 */
    @PostMapping("/expenses")
    public ApiResponse<CoupleLifeService.ExpenseVO> addExpense(@RequestBody ExpenseRequest req,
                                                               HttpSession session) {
        return ApiResponse.ok(lifeService.addExpense(Sessions.requireUser(session),
                req.amount(), req.category(), req.note(), req.spentDay()));
    }

    /** 某月账单（yyyy-MM，默认当月）：明细 + 双方合计 + AA 差额提示。 */
    @GetMapping("/expenses")
    public ApiResponse<CoupleLifeService.ExpenseMonthVO> monthExpenses(
            @RequestParam(required = false) String month, HttpSession session) {
        return ApiResponse.ok(lifeService.monthExpenses(Sessions.requireUser(session), month));
    }

    @DeleteMapping("/expenses/{id}")
    public ApiResponse<Void> deleteExpense(@PathVariable String id, HttpSession session) {
        lifeService.deleteExpense(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    // ---------- 家务轮值 ----------

    /** 添加家务（SINGLE 固定给我 / ALTERNATE 每次轮换）。 */
    @PostMapping("/chores")
    public ApiResponse<CoupleLifeService.ChoreVO> addChore(@RequestBody ChoreRequest req, HttpSession session) {
        return ApiResponse.ok(lifeService.addChore(Sessions.requireUser(session), req.title(), req.rotate()));
    }

    @GetMapping("/chores")
    public ApiResponse<List<CoupleLifeService.ChoreVO>> chores(HttpSession session) {
        return ApiResponse.ok(lifeService.listChores(Sessions.requireUser(session)));
    }

    /** 完成打卡（值日生本人；ALTERNATE 自动轮换）。 */
    @PostMapping("/chores/{id}/done")
    public ApiResponse<CoupleLifeService.ChoreVO> doneChore(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(lifeService.doneChore(Sessions.requireUser(session), id));
    }

    @DeleteMapping("/chores/{id}")
    public ApiResponse<Void> deleteChore(@PathVariable String id, HttpSession session) {
        lifeService.deleteChore(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    // ---------- 约会规划 ----------

    /** 计划一场约会。 */
    @PostMapping("/date-plans")
    public ApiResponse<CoupleLifeService.DatePlanVO> addDatePlan(@RequestBody DatePlanRequest req,
                                                                 HttpSession session) {
        return ApiResponse.ok(lifeService.addDatePlan(Sessions.requireUser(session),
                req.title(), req.planDay(), req.place(), req.items()));
    }

    @GetMapping("/date-plans")
    public ApiResponse<List<CoupleLifeService.DatePlanVO>> datePlans(HttpSession session) {
        return ApiResponse.ok(lifeService.listDatePlans(Sessions.requireUser(session)));
    }

    /** 标记完成/恢复计划。 */
    @PostMapping("/date-plans/{id}/done")
    public ApiResponse<CoupleLifeService.DatePlanVO> doneDatePlan(@PathVariable String id,
                                                                  @RequestBody DatePlanDoneRequest req,
                                                                  HttpSession session) {
        return ApiResponse.ok(lifeService.doneDatePlan(Sessions.requireUser(session), id, req.done()));
    }

    @DeleteMapping("/date-plans/{id}")
    public ApiResponse<Void> deleteDatePlan(@PathVariable String id, HttpSession session) {
        lifeService.deleteDatePlan(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    // ---------- 双人习惯 ----------

    /** 创建共同习惯。 */
    @PostMapping("/habits")
    public ApiResponse<CoupleLifeService.HabitVO> addHabit(@RequestBody HabitRequest req, HttpSession session) {
        return ApiResponse.ok(lifeService.addHabit(Sessions.requireUser(session), req.title()));
    }

    @GetMapping("/habits")
    public ApiResponse<List<CoupleLifeService.HabitVO>> habits(HttpSession session) {
        return ApiResponse.ok(lifeService.listHabits(Sessions.requireUser(session)));
    }

    /** 今日打卡（幂等）。 */
    @PostMapping("/habits/{id}/checkin")
    public ApiResponse<CoupleLifeService.HabitVO> checkinHabit(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(lifeService.checkinHabit(Sessions.requireUser(session), id));
    }

    /** 结束/重启习惯。 */
    @PostMapping("/habits/{id}/active")
    public ApiResponse<CoupleLifeService.HabitVO> toggleHabit(@PathVariable String id,
                                                              @RequestBody HabitActiveRequest req,
                                                              HttpSession session) {
        return ApiResponse.ok(lifeService.toggleHabit(Sessions.requireUser(session), id, req.active()));
    }

    @DeleteMapping("/habits/{id}")
    public ApiResponse<Void> deleteHabit(@PathVariable String id, HttpSession session) {
        lifeService.deleteHabit(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    // ---------- 暗号小本本 ----------

    /** 记一条暗号。 */
    @PostMapping("/ciphers")
    public ApiResponse<CoupleLifeService.CipherVO> addCipher(@RequestBody CipherRequest req, HttpSession session) {
        return ApiResponse.ok(lifeService.addCipher(Sessions.requireUser(session),
                req.keyword(), req.meaning()));
    }

    @GetMapping("/ciphers")
    public ApiResponse<List<CoupleLifeService.CipherVO>> ciphers(HttpSession session) {
        return ApiResponse.ok(lifeService.listCiphers(Sessions.requireUser(session)));
    }

    @DeleteMapping("/ciphers/{id}")
    public ApiResponse<Void> deleteCipher(@PathVariable String id, HttpSession session) {
        lifeService.deleteCipher(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }
}
