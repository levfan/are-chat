package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 体温同步·作息与健康（F220-F229）：熄灯/睡眠单/数羊/喝水接力/冷暖/熬夜守护/慢生活/疼痛对策/抱抱/月度安眠小结。 */
@RestController
@RequestMapping("/api/couple/cozy")
public class CoupleCozyController {

    private final CoupleCozyService service;

    public CoupleCozyController(CoupleCozyService service) {
        this.service = service;
    }

    /** 今日体温同步总览。 */
    @GetMapping("/today")
    public ApiResponse<CoupleCozyService.TodayVO> today(HttpSession session) {
        return ApiResponse.ok(service.today(Sessions.requireUser(session)));
    }

    /** F220 道晚安点灯。 */
    public record LightoutRequest(String atTime) {
    }

    @PostMapping("/lightout")
    public ApiResponse<CoupleCozyService.TodayVO> lightout(@RequestBody(required = false) LightoutRequest req,
                                                           HttpSession session) {
        return ApiResponse.ok(service.lightout(Sessions.requireUser(session), req == null ? null : req.atTime()));
    }

    /** F221 报昨夜睡眠单。 */
    public record SleepRequest(String day, Integer stars, String dream) {
    }

    @PostMapping("/sleep")
    public ApiResponse<CoupleCozyService.TodayVO> sleep(@RequestBody SleepRequest req, HttpSession session) {
        return ApiResponse.ok(service.reportSleep(Sessions.requireUser(session), req.day(), req.stars(), req.dream()));
    }

    /** F222 数一只羊。 */
    @PostMapping("/sheep")
    public ApiResponse<CoupleCozyService.TodayVO> sheep(HttpSession session) {
        return ApiResponse.ok(service.sheepTap(Sessions.requireUser(session)));
    }

    /** F223 干一杯水。 */
    @PostMapping("/water")
    public ApiResponse<CoupleCozyService.TodayVO> water(HttpSession session) {
        return ApiResponse.ok(service.water(Sessions.requireUser(session)));
    }

    /** F224 互报今日冷暖。 */
    public record WeatherRequest(String city, String feel, String tempText) {
    }

    @PostMapping("/weather")
    public ApiResponse<CoupleCozyService.TodayVO> weather(@RequestBody WeatherRequest req, HttpSession session) {
        return ApiResponse.ok(service.weather(Sessions.requireUser(session), req.city(), req.feel(), req.tempText()));
    }

    /** F224 一键叮嘱添衣。 */
    @PostMapping("/weather/advise")
    public ApiResponse<CoupleCozyService.TodayVO> advise(HttpSession session) {
        return ApiResponse.ok(service.adviseWeather(Sessions.requireUser(session)));
    }

    /** F225 递「早点睡」陪伴卡（一天一张幂等）。 */
    @PostMapping("/latenight")
    public ApiResponse<CoupleCozyService.TodayVO> latenight(HttpSession session) {
        return ApiResponse.ok(service.latenight(Sessions.requireUser(session)));
    }

    /** F226 提本周慢生活小事。 */
    public record SlowRequest(String thing) {
    }

    @PostMapping("/slow")
    public ApiResponse<CoupleCozyService.TodayVO> slow(@RequestBody SlowRequest req, HttpSession session) {
        return ApiResponse.ok(service.slow(Sessions.requireUser(session), req.thing()));
    }

    /** F226 慢生活小事打卡。 */
    @PostMapping("/slow/check")
    public ApiResponse<CoupleCozyService.TodayVO> slowCheck(HttpSession session) {
        return ApiResponse.ok(service.slowCheck(Sessions.requireUser(session)));
    }

    /** F227 登记我的疼痛对策本。 */
    public record RemedyRequest(String body) {
    }

    @PostMapping("/remedy")
    public ApiResponse<CoupleCozyService.TodayVO> remedy(@RequestBody RemedyRequest req, HttpSession session) {
        return ApiResponse.ok(service.saveRemedy(Sessions.requireUser(session), req.body()));
    }

    /** F227 TA 不适日一键执行对策+送达。 */
    @PostMapping("/comfort")
    public ApiResponse<CoupleCozyService.TodayVO> comfort(HttpSession session) {
        return ApiResponse.ok(service.comfort(Sessions.requireUser(session)));
    }

    /** F228 自报抱抱次数。 */
    public record HugRequest(Integer cnt, String note) {
    }

    @PostMapping("/hug")
    public ApiResponse<CoupleCozyService.TodayVO> hug(@RequestBody HugRequest req, HttpSession session) {
        return ApiResponse.ok(service.hug(Sessions.requireUser(session), req.cnt(), req.note()));
    }

    /** F229 月度安眠小结。 */
    @GetMapping("/monthly")
    public ApiResponse<CoupleCozyService.MonthlyVO> monthly(@RequestParam(required = false) String month,
                                                            HttpSession session) {
        return ApiResponse.ok(service.monthly(Sessions.requireUser(session), month));
    }
}
