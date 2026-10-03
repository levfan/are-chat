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
 * 纪念与回忆：徽章墙 / 那年今天 / 时光胶囊 / 倒数日期待清单 / 恋爱月报 / 数据总览。
 */
@RestController
@RequestMapping("/api/couple/memory")
public class CoupleMemoryController {

    public record CapsuleRequest(String content, String openDay) {
    }

    public record CountdownRequest(String title, String targetDay, String note) {
    }

    public record CountdownDoneRequest(boolean done) {
    }

    public record FirstRequest(String title, String firstDay, String note) {
    }

    private final CoupleMemoryService memoryService;

    public CoupleMemoryController(CoupleMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /** 徽章墙：里程碑徽章 + 行为成就。 */
    @GetMapping("/badges")
    public ApiResponse<CoupleMemoryService.BadgeWallVO> badges(HttpSession session) {
        return ApiResponse.ok(memoryService.badgeWall(Sessions.requireUser(session)));
    }

    /** 那年今天：历史上同月同日发生的事。 */
    @GetMapping("/on-this-day")
    public ApiResponse<List<CoupleMemoryService.OnThisDayEvent>> onThisDay(HttpSession session) {
        return ApiResponse.ok(memoryService.onThisDay(Sessions.requireUser(session)));
    }

    // ---------- 时光胶囊 ----------

    /** 封一枚胶囊（30~365 天后可开）。 */
    @PostMapping("/capsules")
    public ApiResponse<CoupleMemoryService.CapsuleVO> sealCapsule(@RequestBody CapsuleRequest req,
                                                                  HttpSession session) {
        return ApiResponse.ok(memoryService.sealCapsule(Sessions.requireUser(session),
                req.content(), req.openDay()));
    }

    @GetMapping("/capsules")
    public ApiResponse<List<CoupleMemoryService.CapsuleVO>> capsules(HttpSession session) {
        return ApiResponse.ok(memoryService.listCapsules(Sessions.requireUser(session)));
    }

    /** 开启胶囊（收件人 + 到点）。 */
    @PostMapping("/capsules/{id}/open")
    public ApiResponse<CoupleMemoryService.CapsuleVO> openCapsule(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(memoryService.openCapsule(Sessions.requireUser(session), id));
    }

    // ---------- 倒数日期待清单 ----------

    /** 新增一个倒数日。 */
    @PostMapping("/countdowns")
    public ApiResponse<CoupleMemoryService.CountdownVO> addCountdown(@RequestBody CountdownRequest req,
                                                                     HttpSession session) {
        return ApiResponse.ok(memoryService.addCountdown(Sessions.requireUser(session),
                req.title(), req.targetDay(), req.note()));
    }

    @GetMapping("/countdowns")
    public ApiResponse<List<CoupleMemoryService.CountdownVO>> countdowns(HttpSession session) {
        return ApiResponse.ok(memoryService.listCountdowns(Sessions.requireUser(session)));
    }

    /** 标记实现/取消实现。 */
    @PostMapping("/countdowns/{id}/done")
    public ApiResponse<CoupleMemoryService.CountdownVO> doneCountdown(@PathVariable String id,
                                                                      @RequestBody CountdownDoneRequest req,
                                                                      HttpSession session) {
        return ApiResponse.ok(memoryService.doneCountdown(Sessions.requireUser(session), id, req.done()));
    }

    @DeleteMapping("/countdowns/{id}")
    public ApiResponse<Void> deleteCountdown(@PathVariable String id, HttpSession session) {
        memoryService.deleteCountdown(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    // ---------- 恋爱月报 / 数据总览 ----------


    // ---------- F46 第一次清单 ----------

    /** 记录一个「我们的第一次」。 */
    @PostMapping("/firsts")
    public ApiResponse<CoupleMemoryService.FirstVO> createFirst(@RequestBody FirstRequest req, HttpSession session) {
        return ApiResponse.ok(memoryService.createFirst(Sessions.requireUser(session),
                req.title(), req.firstDay(), req.note()));
    }

    /** 第一次清单：按发生日期升序。 */
    @GetMapping("/firsts")
    public ApiResponse<List<CoupleMemoryService.FirstVO>> listFirsts(HttpSession session) {
        return ApiResponse.ok(memoryService.listFirsts(Sessions.requireUser(session)));
    }

    /** 删除一条第一次记录。 */
    @DeleteMapping("/firsts/{id}")
    public ApiResponse<Void> deleteFirst(@PathVariable String id, HttpSession session) {
        memoryService.deleteFirst(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

}
