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

/** 两个人的饭桌（F210-F219）：饭票/裁决/星评/踩雷/菜单/拿手菜/点单机/搭伙车/话题卡/年度干饭账。 */
@RestController
@RequestMapping("/api/couple/dining")
public class CoupleDiningController {

    private final CoupleDiningService service;

    public CoupleDiningController(CoupleDiningService service) {
        this.service = service;
    }

    /** F210/F211/F218 今日饭桌总览。 */
    @GetMapping("/today")
    public ApiResponse<CoupleDiningService.TodayVO> today(HttpSession session) {
        return ApiResponse.ok(service.today(Sessions.requireUser(session)));
    }

    /** F210 投今晚饭票。 */
    public record TicketRequest(String dish, String reason) {
    }

    @PostMapping("/ticket")
    public ApiResponse<CoupleDiningService.TodayVO> throwTicket(@RequestBody TicketRequest req, HttpSession session) {
        return ApiResponse.ok(service.throwTicket(Sessions.requireUser(session), req.dish(), req.reason()));
    }

    /** F218 标记今日话题聊过了。 */
    @PostMapping("/topic/mark")
    public ApiResponse<CoupleDiningService.TodayVO> markTopic(HttpSession session) {
        return ApiResponse.ok(service.markTopic(Sessions.requireUser(session)));
    }

    /** F212 星评流水。 */
    @GetMapping("/rates")
    public ApiResponse<List<CoupleDiningService.RateVO>> rates(HttpSession session) {
        return ApiResponse.ok(service.rates(Sessions.requireUser(session)));
    }

    /** F212 登记一笔吃过星评。 */
    public record RateRequest(String day, String dish, Integer stars, String comment) {
    }

    @PostMapping("/rate")
    public ApiResponse<List<CoupleDiningService.RateVO>> rate(@RequestBody RateRequest req, HttpSession session) {
        return ApiResponse.ok(service.rate(Sessions.requireUser(session), req.day(), req.dish(), req.stars(), req.comment()));
    }

    /** F213 踩雷库列表。 */
    @GetMapping("/nogos")
    public ApiResponse<List<CoupleDiningService.NogoVO>> nogos(HttpSession session) {
        return ApiResponse.ok(service.nogos(Sessions.requireUser(session)));
    }

    /** F213 拉黑一家店。 */
    public record NogoRequest(String name, String reason) {
    }

    @PostMapping("/nogo")
    public ApiResponse<List<CoupleDiningService.NogoVO>> addNogo(@RequestBody NogoRequest req, HttpSession session) {
        return ApiResponse.ok(service.addNogo(Sessions.requireUser(session), req.name(), req.reason()));
    }

    /** F213 划掉踩雷（仅提议人）。 */
    @DeleteMapping("/nogo/{id}")
    public ApiResponse<List<CoupleDiningService.NogoVO>> removeNogo(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.removeNogo(Sessions.requireUser(session), id));
    }

    /** F214-F217 本周饭桌看板：菜单格 + 拿手菜 + 搭伙车。 */
    @GetMapping("/board")
    public ApiResponse<CoupleDiningService.BoardVO> board(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
    }

    /** F214 排/改/擦某一天的正餐。 */
    public record PlanRequest(String day, String dish) {
    }

    @PostMapping("/plan")
    public ApiResponse<CoupleDiningService.BoardVO> setPlan(@RequestBody PlanRequest req, HttpSession session) {
        return ApiResponse.ok(service.setPlan(Sessions.requireUser(session), req.day(), req.dish()));
    }

    /** F215 报本周拿手菜。 */
    public record HomecookRequest(String dish, Integer score) {
    }

    @PostMapping("/homecook")
    public ApiResponse<CoupleDiningService.BoardVO> reportHomecook(@RequestBody HomecookRequest req, HttpSession session) {
        return ApiResponse.ok(service.reportHomecook(Sessions.requireUser(session), req.dish(), req.score()));
    }

    /** F216 点单机：心情换饮品。 */
    @GetMapping("/drink")
    public ApiResponse<CoupleDiningService.DrinkVO> drink(@RequestParam(required = false) String mood) {
        return ApiResponse.ok(service.drink(mood));
    }

    /** F217 往搭伙车加菜。 */
    public record CartRequest(String item, Integer qty) {
    }

    @PostMapping("/cart")
    public ApiResponse<CoupleDiningService.BoardVO> cartAdd(@RequestBody CartRequest req, HttpSession session) {
        return ApiResponse.ok(service.cartAdd(Sessions.requireUser(session), req.item(), req.qty()));
    }

    /** F217 给一道菜按锁，双方都锁即成行。 */
    @PostMapping("/cart/{id}/lock")
    public ApiResponse<CoupleDiningService.BoardVO> cartLock(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.cartLock(Sessions.requireUser(session), id));
    }

    /** F217 拿掉自己加的未锁菜。 */
    @DeleteMapping("/cart/{id}")
    public ApiResponse<CoupleDiningService.BoardVO> cartRemove(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.cartRemove(Sessions.requireUser(session), id));
    }

    /** F219 年度干饭账。 */
    @GetMapping("/year")
    public ApiResponse<CoupleDiningService.YearVO> year(@RequestParam(required = false) String year, HttpSession session) {
        return ApiResponse.ok(service.yearReport(Sessions.requireUser(session), year));
    }
}
