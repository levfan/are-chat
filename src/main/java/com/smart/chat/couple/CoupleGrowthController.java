package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 共同养成（系统裁剪后只留心愿互换板：许愿/旅行心愿/下次一定）。
 */
@RestController
@RequestMapping("/api/couple/growth")
public class CoupleGrowthController {

    private final CoupleGrowWishService wishService;

    public CoupleGrowthController(CoupleGrowWishService wishService) {
        this.wishService = wishService;
    }

    // ---------- F73 心愿互换 ----------

    /** 心愿正文。 */
    public record WishRequest(String wish) {
    }

    /** 兑现备注。 */
    public record DoneRequest(String doneNote) {
    }

    /** 双方心愿列表。 */
    @GetMapping("/wishes")
    public ApiResponse<List<CoupleGrowWishService.WishVO>> wishes(HttpSession session) {
        return ApiResponse.ok(wishService.wishes(Sessions.requireUser(session)));
    }

    /** 许一个愿（等对方接单，自己不能接）。 */
    @PostMapping("/wishes")
    public ApiResponse<List<CoupleGrowWishService.WishVO>> makeWish(@RequestBody WishRequest req,
                                                                    HttpSession session) {
        return ApiResponse.ok(wishService.makeWish(Sessions.requireUser(session), req.wish()));
    }

    /** 对方接单。 */
    @PostMapping("/wishes/{id}/accept")
    public ApiResponse<List<CoupleGrowWishService.WishVO>> acceptWish(@PathVariable String id,
                                                                      HttpSession session) {
        return ApiResponse.ok(wishService.acceptWish(Sessions.requireUser(session), id));
    }

    /** 接单的人兑现。 */
    @PostMapping("/wishes/{id}/fulfill")
    public ApiResponse<List<CoupleGrowWishService.WishVO>> fulfillWish(@PathVariable String id,
                                                                       @RequestBody DoneRequest req,
                                                                       HttpSession session) {
        return ApiResponse.ok(wishService.fulfillWish(Sessions.requireUser(session), id, req.doneNote()));
    }

    // ---------- F75 旅行心愿地图 ----------

    /** 地点与想去做什么。 */
    public record TravelRequest(String place, String wantTodo) {
    }

    /** 打卡备注。 */
    public record VisitRequest(String visitedNote) {
    }

    /** 全部旅行心愿（含已去过）。 */
    @GetMapping("/travels")
    public ApiResponse<List<CoupleGrowWishService.TravelVO>> travels(HttpSession session) {
        return ApiResponse.ok(wishService.travels(Sessions.requireUser(session)));
    }

    /** 在地图上钉一个想去的地方。 */
    @PostMapping("/travels")
    public ApiResponse<List<CoupleGrowWishService.TravelVO>> addTravel(@RequestBody TravelRequest req,
                                                                       HttpSession session) {
        return ApiResponse.ok(wishService.addTravel(Sessions.requireUser(session), req.place(), req.wantTodo()));
    }

    /** 去过了，打卡留念。 */
    @PostMapping("/travels/{id}/visit")
    public ApiResponse<List<CoupleGrowWishService.TravelVO>> visitTravel(@PathVariable String id,
                                                                         @RequestBody VisitRequest req,
                                                                         HttpSession session) {
        return ApiResponse.ok(wishService.visitTravel(Sessions.requireUser(session), id, req.visitedNote()));
    }

    // ---------- F79 下次一定 ----------

    /** 承诺内容。 */
    public record NextTimeRequest(String byUser, String content) {
    }

    /** 在途与已兑现的「下次一定」。 */
    @GetMapping("/next-times")
    public ApiResponse<List<CoupleGrowWishService.NextTimeVO>> nextTimes(HttpSession session) {
        return ApiResponse.ok(wishService.nextTimes(Sessions.requireUser(session)));
    }

    /** 把随口一句「下次一起去」落单。 */
    @PostMapping("/next-times")
    public ApiResponse<List<CoupleGrowWishService.NextTimeVO>> addNextTime(@RequestBody NextTimeRequest req,
                                                                            HttpSession session) {
        return ApiResponse.ok(wishService.addNextTime(Sessions.requireUser(session), req.byUser(), req.content()));
    }

    /** 催办（一人 1 小时冷却）。 */
    @PostMapping("/next-times/{id}/nudge")
    public ApiResponse<List<CoupleGrowWishService.NextTimeVO>> nudgeNextTime(@PathVariable String id,
                                                                              HttpSession session) {
        return ApiResponse.ok(wishService.nudgeNextTime(Sessions.requireUser(session), id));
    }

    /** 兑现销账。 */
    @PostMapping("/next-times/{id}/fulfill")
    public ApiResponse<List<CoupleGrowWishService.NextTimeVO>> fulfillNextTime(@PathVariable String id,
                                                                               HttpSession session) {
        return ApiResponse.ok(wishService.fulfillNextTime(Sessions.requireUser(session), id));
    }
}
