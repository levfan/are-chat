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

/** 夫妻老黄历（F250-F259）：节气跟风/过法/择吉日/节日家档/手账/长假愿望/放空日/生肖年运/一年小结。 */
@RestController
@RequestMapping("/api/couple/almanac")
public class CoupleAlmanacController {

    private final CoupleAlmanacService service;

    public CoupleAlmanacController(CoupleAlmanacService service) {
        this.service = service;
    }

    /** 今日岁时总览。 */
    @GetMapping("/today")
    public ApiResponse<CoupleAlmanacService.TodayVO> today(HttpSession session) {
        return ApiResponse.ok(service.today(Sessions.requireUser(session)));
    }

    /** F250 节气当日跟风打卡。 */
    public record CheckRequest(String note) {
    }

    @PostMapping("/check")
    public ApiResponse<CoupleAlmanacService.TodayVO> check(@RequestBody(required = false) CheckRequest req,
                                                           HttpSession session) {
        return ApiResponse.ok(service.termCheck(Sessions.requireUser(session), req == null ? null : req.note()));
    }

    /** F251 写节气过法。 */
    public record RitualRequest(String term, String content) {
    }

    @PostMapping("/ritual")
    public ApiResponse<CoupleAlmanacService.TodayVO> ritual(@RequestBody RitualRequest req, HttpSession session) {
        return ApiResponse.ok(service.addRitual(Sessions.requireUser(session), req.term(), req.content()));
    }

    public record RemoveRequest(String id) {
    }

    @PostMapping("/ritual/remove")
    public ApiResponse<CoupleAlmanacService.TodayVO> ritualRemove(@RequestBody RemoveRequest req, HttpSession session) {
        return ApiResponse.ok(service.removeRitual(Sessions.requireUser(session), req.id()));
    }

    /** F251 节气当日过法打卡。 */
    @PostMapping("/ritual/mark")
    public ApiResponse<CoupleAlmanacService.TodayVO> ritualMark(@RequestBody RemoveRequest req, HttpSession session) {
        return ApiResponse.ok(service.markRitual(Sessions.requireUser(session), req.id()));
    }

    /** F252 择吉日。 */
    public record LuckyRequest(String day, String matter) {
    }

    @PostMapping("/lucky")
    public ApiResponse<CoupleAlmanacService.TodayVO> lucky(@RequestBody LuckyRequest req, HttpSession session) {
        return ApiResponse.ok(service.lucky(Sessions.requireUser(session), req.day(), req.matter()));
    }

    /** F252 吉日双盖章。 */
    @PostMapping("/lucky/confirm")
    public ApiResponse<CoupleAlmanacService.TodayVO> luckyConfirm(@RequestBody RemoveRequest req, HttpSession session) {
        return ApiResponse.ok(service.confirmLucky(Sessions.requireUser(session), req.id()));
    }

    /** F254 节日家档。 */
    public record FestivalRequest(String festival, String year, String plan) {
    }

    @PostMapping("/festival")
    public ApiResponse<CoupleAlmanacService.TodayVO> festival(@RequestBody FestivalRequest req, HttpSession session) {
        return ApiResponse.ok(service.festival(Sessions.requireUser(session), req.festival(), req.year(), req.plan()));
    }

    /** F255 节气手账。 */
    public record NoteRequest(String term, String year, String text) {
    }

    @PostMapping("/note")
    public ApiResponse<CoupleAlmanacService.TodayVO> note(@RequestBody NoteRequest req, HttpSession session) {
        return ApiResponse.ok(service.note(Sessions.requireUser(session), req.term(), req.year(), req.text()));
    }

    /** F257 长假愿望（首写/补写）。 */
    public record WishRequest(String wish) {
    }

    @PostMapping("/wish")
    public ApiResponse<CoupleAlmanacService.TodayVO> wish(@RequestBody WishRequest req, HttpSession session) {
        return ApiResponse.ok(service.holidayWish(Sessions.requireUser(session), req.wish()));
    }

    /** F258 提报放空日。 */
    public record NormalRequest(String day) {
    }

    @PostMapping("/normal")
    public ApiResponse<CoupleAlmanacService.TodayVO> normal(@RequestBody NormalRequest req, HttpSession session) {
        return ApiResponse.ok(service.normalDay(Sessions.requireUser(session), req.day()));
    }

    /** F256 生肖年运。 */
    @GetMapping("/zodiac")
    public ApiResponse<CoupleAlmanacService.ZodiacVO> zodiac(HttpSession session) {
        return ApiResponse.ok(service.zodiac(Sessions.requireUser(session)));
    }

    /** F259 一年日子小结。 */
    @GetMapping("/yearly")
    public ApiResponse<CoupleAlmanacService.YearVO> yearly(@RequestParam(required = false) String year,
                                                           HttpSession session) {
        return ApiResponse.ok(service.yearly(Sessions.requireUser(session), year));
    }
}
