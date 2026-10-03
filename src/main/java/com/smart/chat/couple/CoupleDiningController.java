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

}
