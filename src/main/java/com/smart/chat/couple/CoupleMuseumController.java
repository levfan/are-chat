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

/** 时光博物馆系（F190-F199）：纪录片分镜、博物馆展品、去年今日对比镜、银发情话、高频词、隐藏成就、家规宪法、免打扰、问候引擎、年度记忆书目录。 */
@RestController
@RequestMapping("/api/couple/museum")
public class CoupleMuseumController {

    private final CoupleMuseumService service;

    public CoupleMuseumController(CoupleMuseumService service) {
        this.service = service;
    }

    /** F190 分镜列表。 */
    @GetMapping("/scenes")
    public ApiResponse<List<CoupleMuseumService.DocSceneVO>> scenes(HttpSession session) {
        return ApiResponse.ok(service.scenes(Sessions.requireUser(session)));
    }

    /** F190 写一部三幕纪录片。 */
    public record SceneRequest(String title, String actOne, String actTwo, String actThree) {
    }

    @PostMapping("/scenes")
    public ApiResponse<List<CoupleMuseumService.DocSceneVO>> addScene(@RequestBody SceneRequest req, HttpSession session) {
        return ApiResponse.ok(service.addScene(Sessions.requireUser(session), req.title(), req.actOne(), req.actTwo(), req.actThree()));
    }

    /** F191 展品列表。 */
    @GetMapping("/exhibits")
    public ApiResponse<List<CoupleMuseumService.ExhibitVO>> exhibits(HttpSession session) {
        return ApiResponse.ok(service.exhibits(Sessions.requireUser(session)));
    }

    /** F191 登记展品。 */
    public record ExhibitRequest(String name, String story, String obtainedDay) {
    }

    @PostMapping("/exhibits")
    public ApiResponse<List<CoupleMuseumService.ExhibitVO>> addExhibit(@RequestBody ExhibitRequest req, HttpSession session) {
        return ApiResponse.ok(service.addExhibit(Sessions.requireUser(session), req.name(), req.story(), req.obtainedDay()));
    }

    /** F192 去年今日对比镜。 */
    @GetMapping("/last-year")
    public ApiResponse<CoupleMuseumService.LastYearMirrorVO> lastYearMirror(HttpSession session) {
        return ApiResponse.ok(service.lastYearMirror(Sessions.requireUser(session)));
    }

    /** F193 今日银发情话。 */
    @GetMapping("/silver-line")
    public ApiResponse<CoupleMuseumService.SilverLineVO> silverLine(HttpSession session) {
        return ApiResponse.ok(service.silverLine(Sessions.requireUser(session)));
    }

    /** F194 恋爱高频词。 */
    @GetMapping("/words")
    public ApiResponse<List<CoupleMuseumService.WordVO>> words(HttpSession session) {
        return ApiResponse.ok(service.topWords(Sessions.requireUser(session)));
    }

    /** F195 隐藏成就墙（达标自动解锁）。 */
    @GetMapping("/achievements")
    public ApiResponse<List<CoupleMuseumService.AchievementVO>> achievements(HttpSession session) {
        return ApiResponse.ok(service.achievements(Sessions.requireUser(session)));
    }

    /** F196 家规列表。 */
    @GetMapping("/rules")
    public ApiResponse<List<CoupleMuseumService.RuleVO>> rules(HttpSession session) {
        return ApiResponse.ok(service.rules(Sessions.requireUser(session)));
    }

    /** F196 提交条款/修正案。 */
    public record RuleRequest(String kind, String refId, String content) {
    }

    @PostMapping("/rules")
    public ApiResponse<List<CoupleMuseumService.RuleVO>> addRule(@RequestBody RuleRequest req, HttpSession session) {
        return ApiResponse.ok(service.addRule(Sessions.requireUser(session), req.kind(), req.refId(), req.content()));
    }

    /** F196 签字生效。 */
    @PostMapping("/rules/{id}/sign")
    public ApiResponse<List<CoupleMuseumService.RuleVO>> signRule(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.signRule(Sessions.requireUser(session), id));
    }

    /** F197 双方免打扰设置。 */
    @GetMapping("/dnd")
    public ApiResponse<List<CoupleMuseumService.DndVO>> dnd(HttpSession session) {
        return ApiResponse.ok(service.dndSettings(Sessions.requireUser(session)));
    }

    /** F197 保存我的免打扰时段。 */
    public record DndRequest(String startTime, String endTime, Boolean enabled) {
    }

    @PostMapping("/dnd")
    public ApiResponse<List<CoupleMuseumService.DndVO>> saveDnd(@RequestBody DndRequest req, HttpSession session) {
        return ApiResponse.ok(service.saveDnd(Sessions.requireUser(session), req.startTime(), req.endTime(), req.enabled()));
    }

    /** F198 首页问候引擎。 */
    @GetMapping("/greeting")
    public ApiResponse<CoupleMuseumService.GreetingVO> greeting(HttpSession session) {
        return ApiResponse.ok(service.greeting(Sessions.requireUser(session)));
    }

    /** F199 年度记忆书目录。 */
    @GetMapping("/annual-book")
    public ApiResponse<CoupleMuseumService.AnnualBookVO> annualBook(HttpSession session) {
        return ApiResponse.ok(service.annualBook(Sessions.requireUser(session)));
    }
}
