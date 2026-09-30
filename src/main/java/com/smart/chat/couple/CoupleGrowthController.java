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
 * 共同养成（F70-F79）：双人挑战赛 / 恋爱存折 / 百日之约 / 心愿互换 / 共读计划 /
 * 旅行心愿地图 / 追剧清单 / 星座配对 / 恋爱词典 / 下次一定清单。
 */
@RestController
@RequestMapping("/api/couple/growth")
public class CoupleGrowthController {

    public record PassbookRequest(String content) {
    }

    public record HundredCreateRequest(String goal, String startDay) {
    }

    public record HundredCheckinRequest(String note) {
    }

    public record WishRequest(String wish) {
    }

    public record WishFulfillRequest(String doneNote) {
    }

    public record TravelRequest(String place, String wantTodo) {
    }

    public record TravelVisitRequest(String visitedNote) {
    }

    public record NextTimeRequest(String byUser, String content) {
    }

    public record ReadPlanRequest(String title, Integer totalUnits, String unitLabel) {
    }

    public record ReadProgressRequest(Integer unit, String note) {
    }

    public record WatchRequest(String title, Integer totalUnit) {
    }

    public record WatchProgressRequest(Integer currentUnit) {
    }

    public record DictRequest(String word, String meaning) {
    }

    private final CoupleGrowthService growthService;
    private final CoupleGrowWishService wishService;
    private final CoupleEntertainService entertainService;

    public CoupleGrowthController(CoupleGrowthService growthService, CoupleGrowWishService wishService,
                                  CoupleEntertainService entertainService) {
        this.growthService = growthService;
        this.wishService = wishService;
        this.entertainService = entertainService;
    }

    // ---------- F70 双人挑战赛 ----------

    /** 今日挑战 + 历史。 */
    @GetMapping("/challenge")
    public ApiResponse<CoupleGrowthService.ChallengeBoardVO> challenge(HttpSession session) {
        return ApiResponse.ok(growthService.challenge(Sessions.requireUser(session)));
    }

    /** 打卡今日挑战（双方都完成即达成）。 */
    @PostMapping("/challenge/check")
    public ApiResponse<CoupleGrowthService.ChallengeBoardVO> checkChallenge(HttpSession session) {
        return ApiResponse.ok(growthService.checkChallenge(Sessions.requireUser(session)));
    }

    // ---------- F71 恋爱存折 ----------

    /** 存折看板：今天双方存款 + 我的连续天数 + 最近流水。 */
    @GetMapping("/passbook")
    public ApiResponse<CoupleGrowthService.PassbookBoardVO> passbook(HttpSession session) {
        return ApiResponse.ok(growthService.passbook(Sessions.requireUser(session)));
    }

    /** 存一笔「今天为这段感情做的小事」。 */
    @PostMapping("/passbook")
    public ApiResponse<CoupleGrowthService.PassbookBoardVO> depositPassbook(@RequestBody PassbookRequest req,
                                                                            HttpSession session) {
        return ApiResponse.ok(growthService.depositPassbook(Sessions.requireUser(session), req.content()));
    }

    // ---------- F72 百日之约 ----------

    @GetMapping("/hundreds")
    public ApiResponse<List<CoupleGrowthService.HundredVO>> hundreds(HttpSession session) {
        return ApiResponse.ok(growthService.hundreds(Sessions.requireUser(session)));
    }

    /** 发起百日之约（同一时间仅一个进行中）。 */
    @PostMapping("/hundreds")
    public ApiResponse<List<CoupleGrowthService.HundredVO>> createHundred(@RequestBody HundredCreateRequest req,
                                                                          HttpSession session) {
        return ApiResponse.ok(growthService.createHundred(Sessions.requireUser(session), req.goal(), req.startDay()));
    }

    /** 每日打卡（双方打卡满 100 天自动达成）。 */
    @PostMapping("/hundreds/{id}/checkin")
    public ApiResponse<List<CoupleGrowthService.HundredVO>> checkinHundred(@PathVariable String id,
                                                                           @RequestBody HundredCheckinRequest req,
                                                                           HttpSession session) {
        return ApiResponse.ok(growthService.checkinHundred(Sessions.requireUser(session), id, req.note()));
    }

    /** 中止百日之约。 */
    @PostMapping("/hundreds/{id}/break")
    public ApiResponse<List<CoupleGrowthService.HundredVO>> breakHundred(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(growthService.breakHundred(Sessions.requireUser(session), id));
    }

    // ---------- F77 星座配对（静态） ----------

    /** 配对指数与评语（按元素相性 + 稳定哈希，同对恒定）。 */
    @GetMapping("/zodiac")
    public ApiResponse<CoupleGrowthService.ZodiacVO> zodiac(@RequestParam String mine,
                                                            @RequestParam String partner, HttpSession session) {
        Sessions.requireUser(session);
        return ApiResponse.ok(growthService.zodiac(mine, partner));
    }

    // ---------- F73 心愿互换 ----------

    @GetMapping("/wishes")
    public ApiResponse<List<CoupleGrowWishService.WishVO>> wishes(HttpSession session) {
        return ApiResponse.ok(wishService.wishes(Sessions.requireUser(session)));
    }

    /** 许一个「想让 TA 实现的心愿」。 */
    @PostMapping("/wishes")
    public ApiResponse<List<CoupleGrowWishService.WishVO>> makeWish(@RequestBody WishRequest req, HttpSession session) {
        return ApiResponse.ok(wishService.makeWish(Sessions.requireUser(session), req.wish()));
    }

    /** TA 接单我的心愿。 */
    @PostMapping("/wishes/{id}/accept")
    public ApiResponse<List<CoupleGrowWishService.WishVO>> acceptWish(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(wishService.acceptWish(Sessions.requireUser(session), id));
    }

    /** 实现 TA 的心愿。 */
    @PostMapping("/wishes/{id}/fulfill")
    public ApiResponse<List<CoupleGrowWishService.WishVO>> fulfillWish(@PathVariable String id,
                                                                       @RequestBody WishFulfillRequest req,
                                                                       HttpSession session) {
        return ApiResponse.ok(wishService.fulfillWish(Sessions.requireUser(session), id, req.doneNote()));
    }

    // ---------- F75 旅行心愿地图 ----------

    @GetMapping("/travels")
    public ApiResponse<List<CoupleGrowWishService.TravelVO>> travels(HttpSession session) {
        return ApiResponse.ok(wishService.travels(Sessions.requireUser(session)));
    }

    /** 添加「想一起去」的地方。 */
    @PostMapping("/travels")
    public ApiResponse<List<CoupleGrowWishService.TravelVO>> addTravel(@RequestBody TravelRequest req,
                                                                       HttpSession session) {
        return ApiResponse.ok(wishService.addTravel(Sessions.requireUser(session), req.place(), req.wantTodo()));
    }

    /** 打卡去过。 */
    @PostMapping("/travels/{id}/visit")
    public ApiResponse<List<CoupleGrowWishService.TravelVO>> visitTravel(@PathVariable String id,
                                                                         @RequestBody TravelVisitRequest req,
                                                                         HttpSession session) {
        return ApiResponse.ok(wishService.visitTravel(Sessions.requireUser(session), id, req.visitedNote()));
    }

    // ---------- F79 下次一定清单 ----------

    @GetMapping("/next-times")
    public ApiResponse<List<CoupleGrowWishService.NextTimeVO>> nextTimes(HttpSession session) {
        return ApiResponse.ok(wishService.nextTimes(Sessions.requireUser(session)));
    }

    /** 登记一句「下次一定」（默认记我，byUser 可记 TA）。 */
    @PostMapping("/next-times")
    public ApiResponse<List<CoupleGrowWishService.NextTimeVO>> addNextTime(@RequestBody NextTimeRequest req,
                                                                           HttpSession session) {
        return ApiResponse.ok(wishService.addNextTime(Sessions.requireUser(session), req.byUser(), req.content()));
    }

    /** 催 TA 兑现（1 小时冷却）。 */
    @PostMapping("/next-times/{id}/nudge")
    public ApiResponse<List<CoupleGrowWishService.NextTimeVO>> nudgeNextTime(@PathVariable String id,
                                                                             HttpSession session) {
        return ApiResponse.ok(wishService.nudgeNextTime(Sessions.requireUser(session), id));
    }

    /** 兑现我的承诺。 */
    @PostMapping("/next-times/{id}/fulfill")
    public ApiResponse<List<CoupleGrowWishService.NextTimeVO>> fulfillNextTime(@PathVariable String id,
                                                                               HttpSession session) {
        return ApiResponse.ok(wishService.fulfillNextTime(Sessions.requireUser(session), id));
    }

    // ---------- F74 共读计划 ----------

    @GetMapping("/read-plans")
    public ApiResponse<List<CoupleEntertainService.ReadPlanVO>> readPlans(HttpSession session) {
        return ApiResponse.ok(entertainService.readPlans(Sessions.requireUser(session)));
    }

    /** 开一个共读计划。 */
    @PostMapping("/read-plans")
    public ApiResponse<List<CoupleEntertainService.ReadPlanVO>> createReadPlan(@RequestBody ReadPlanRequest req,
                                                                               HttpSession session) {
        return ApiResponse.ok(entertainService.createReadPlan(Sessions.requireUser(session),
                req.title(), req.totalUnits() == null ? 0 : req.totalUnits(), req.unitLabel()));
    }

    /** 上报我的最新进度。 */
    @PostMapping("/read-plans/{id}/progress")
    public ApiResponse<List<CoupleEntertainService.ReadPlanVO>> reportReadProgress(@PathVariable String id,
                                                                                   @RequestBody ReadProgressRequest req,
                                                                                   HttpSession session) {
        return ApiResponse.ok(entertainService.reportReadProgress(Sessions.requireUser(session), id,
                req.unit() == null ? 0 : req.unit(), req.note()));
    }

    // ---------- F76 追剧清单 ----------

    @GetMapping("/watchlist")
    public ApiResponse<List<CoupleEntertainService.WatchVO>> watchlist(HttpSession session) {
        return ApiResponse.ok(entertainService.watchlist(Sessions.requireUser(session)));
    }

    /** 加一部一起追的剧。 */
    @PostMapping("/watchlist")
    public ApiResponse<List<CoupleEntertainService.WatchVO>> addWatch(@RequestBody WatchRequest req,
                                                                      HttpSession session) {
        return ApiResponse.ok(entertainService.addWatch(Sessions.requireUser(session), req.title(), req.totalUnit()));
    }

    /** 更新共同进度。 */
    @PostMapping("/watchlist/{id}/progress")
    public ApiResponse<List<CoupleEntertainService.WatchVO>> updateWatch(@PathVariable String id,
                                                                         @RequestBody WatchProgressRequest req,
                                                                         HttpSession session) {
        return ApiResponse.ok(entertainService.updateWatch(Sessions.requireUser(session), id,
                req.currentUnit() == null ? 0 : req.currentUnit()));
    }

    // ---------- F78 恋爱词典 ----------

    @GetMapping("/dict")
    public ApiResponse<List<CoupleEntertainService.DictVO>> dictWords(HttpSession session) {
        return ApiResponse.ok(entertainService.dictWords(Sessions.requireUser(session)));
    }

    /** 收录专属词汇。 */
    @PostMapping("/dict")
    public ApiResponse<List<CoupleEntertainService.DictVO>> addWord(@RequestBody DictRequest req, HttpSession session) {
        return ApiResponse.ok(entertainService.addWord(Sessions.requireUser(session), req.word(), req.meaning()));
    }

    /** 删除词条。 */
    @DeleteMapping("/dict/{id}")
    public ApiResponse<List<CoupleEntertainService.DictVO>> removeWord(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(entertainService.removeWord(Sessions.requireUser(session), id));
    }
}
