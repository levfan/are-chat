package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 二人制造厂（F270-F279）：家务轮盘/采买清单/冰箱库存/代拿快递/叫醒服务/服药链/久坐互拍/垫付本/战利品互猜/家安月检。 */
@RestController
@RequestMapping("/api/couple/factory")
public class CoupleFactoryController {

    private final CoupleFactoryService service;

    public CoupleFactoryController(CoupleFactoryService service) {
        this.service = service;
    }

    /** 本周车间总览。 */
    @GetMapping("/board")
    public ApiResponse<CoupleFactoryService.BoardVO> board(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
    }

    /** F270 一转定分工。 */
    public record SpinRequest(String items) {
    }

    @PostMapping("/spin")
    public ApiResponse<CoupleFactoryService.BoardVO> spin(@RequestBody SpinRequest req, HttpSession session) {
        return ApiResponse.ok(service.spin(Sessions.requireUser(session), req.items()));
    }

    public record IdRequest(String id) {
    }

    @PostMapping("/spin/confirm")
    public ApiResponse<CoupleFactoryService.BoardVO> spinConfirm(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.confirmSpin(Sessions.requireUser(session), req.id()));
    }

    @PostMapping("/spin/done")
    public ApiResponse<CoupleFactoryService.BoardVO> spinDone(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.doneSpin(Sessions.requireUser(session), req.id()));
    }

    /** F271 采买清单。 */
    public record ShopRequest(String name, String qty) {
    }

    @PostMapping("/shop")
    public ApiResponse<CoupleFactoryService.BoardVO> shopAdd(@RequestBody ShopRequest req, HttpSession session) {
        return ApiResponse.ok(service.shopAdd(Sessions.requireUser(session), req.name(), req.qty()));
    }

    @PostMapping("/shop/remove")
    public ApiResponse<CoupleFactoryService.BoardVO> shopRemove(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.shopRemove(Sessions.requireUser(session), req.id()));
    }

    @PostMapping("/shop/done")
    public ApiResponse<CoupleFactoryService.BoardVO> shopDone(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.shopDone(Sessions.requireUser(session), req.id()));
    }

    /** F272 冰箱库存。 */
    public record StockRequest(String item, String qty, String expireDay) {
    }

    @PostMapping("/stock")
    public ApiResponse<CoupleFactoryService.BoardVO> stockAdd(@RequestBody StockRequest req, HttpSession session) {
        return ApiResponse.ok(service.stockAdd(Sessions.requireUser(session), req.item(), req.qty(), req.expireDay()));
    }

    @PostMapping("/stock/out")
    public ApiResponse<CoupleFactoryService.BoardVO> stockOut(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.stockOut(Sessions.requireUser(session), req.id()));
    }

    /** F273 代拿快递。 */
    public record ParcelRequest(String note) {
    }

    @PostMapping("/parcel")
    public ApiResponse<CoupleFactoryService.BoardVO> parcelNew(@RequestBody(required = false) ParcelRequest req,
                                                               HttpSession session) {
        return ApiResponse.ok(service.parcelNew(Sessions.requireUser(session), req == null ? null : req.note()));
    }

    @PostMapping("/parcel/grab")
    public ApiResponse<CoupleFactoryService.BoardVO> parcelGrab(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.parcelGrab(Sessions.requireUser(session), req.id()));
    }

    @PostMapping("/parcel/done")
    public ApiResponse<CoupleFactoryService.BoardVO> parcelDone(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.parcelDone(Sessions.requireUser(session), req.id()));
    }

    /** F274 叫醒服务。 */
    public record WakeRequest(String content) {
    }

    @PostMapping("/wake")
    public ApiResponse<CoupleFactoryService.BoardVO> wakeSet(@RequestBody WakeRequest req, HttpSession session) {
        return ApiResponse.ok(service.wakeSet(Sessions.requireUser(session), req.content()));
    }

    @PostMapping("/wake/give")
    public ApiResponse<CoupleFactoryService.BoardVO> wakeGive(HttpSession session) {
        return ApiResponse.ok(service.wakeGive(Sessions.requireUser(session)));
    }

    /** F275 服药提醒链。 */
    public record MedRequest(String name, String times) {
    }

    @PostMapping("/med")
    public ApiResponse<CoupleFactoryService.BoardVO> medAdd(@RequestBody MedRequest req, HttpSession session) {
        return ApiResponse.ok(service.medAdd(Sessions.requireUser(session), req.name(), req.times()));
    }

    @PostMapping("/med/stop")
    public ApiResponse<CoupleFactoryService.BoardVO> medStop(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.medStop(Sessions.requireUser(session), req.id()));
    }

    @PostMapping("/med/remind")
    public ApiResponse<CoupleFactoryService.BoardVO> medRemind(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.medRemind(Sessions.requireUser(session), req.id()));
    }

    @PostMapping("/med/taken")
    public ApiResponse<CoupleFactoryService.BoardVO> medTaken(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.medTaken(Sessions.requireUser(session), req.id()));
    }

    /** F276 久坐互拍。 */
    @PostMapping("/standup")
    public ApiResponse<CoupleFactoryService.BoardVO> standup(HttpSession session) {
        return ApiResponse.ok(service.standup(Sessions.requireUser(session)));
    }

    /** F277 垫付本。 */
    public record AdvanceRequest(String item, Integer amountCents, String note) {
    }

    @PostMapping("/advance")
    public ApiResponse<CoupleFactoryService.BoardVO> advanceAdd(@RequestBody AdvanceRequest req, HttpSession session) {
        return ApiResponse.ok(service.advanceAdd(Sessions.requireUser(session), req.item(),
                req.amountCents(), req.note()));
    }

    @PostMapping("/advance/settle")
    public ApiResponse<CoupleFactoryService.BoardVO> advanceSettle(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.advanceSettle(Sessions.requireUser(session), req.id()));
    }

    /** F278 战利品互猜。 */
    public record GroceryRequest(String items) {
    }

    @PostMapping("/grocery")
    public ApiResponse<CoupleFactoryService.BoardVO> grocery(@RequestBody GroceryRequest req, HttpSession session) {
        return ApiResponse.ok(service.grocery(Sessions.requireUser(session), req.items()));
    }

    public record GuessRequest(String id, String guess) {
    }

    @PostMapping("/grocery/guess")
    public ApiResponse<CoupleFactoryService.BoardVO> groceryGuess(@RequestBody GuessRequest req, HttpSession session) {
        return ApiResponse.ok(service.groceryGuess(Sessions.requireUser(session), req.id(), req.guess()));
    }

    public record RateRequest(String id, Integer score) {
    }

    @PostMapping("/grocery/rate")
    public ApiResponse<CoupleFactoryService.BoardVO> groceryRate(@RequestBody RateRequest req, HttpSession session) {
        return ApiResponse.ok(service.groceryRate(Sessions.requireUser(session), req.id(), req.score()));
    }

    /** F279 家安月检。 */
    public record CheckRequest(String items) {
    }

    @PostMapping("/homecheck")
    public ApiResponse<CoupleFactoryService.BoardVO> homeCheck(@RequestBody CheckRequest req, HttpSession session) {
        return ApiResponse.ok(service.homeCheck(Sessions.requireUser(session), req.items()));
    }
}
