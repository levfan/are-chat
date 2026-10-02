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

/**
 * 人生关卡（F370-F379，批次三十三）：关卡预告/出关战报/加班预报/生病陪护单/静音舱/搬家互助/
 * 低谷通行证/小胜利账本/关卡成就墙/下次关卡预约。
 * 写接口一律返回整份 QuestVO 聚合（GET /board 的形状），前端整体替换；GET /wall 除外。
 */
@RestController
@RequestMapping("/api/couple/quest")
public class CoupleQuestController {

    private final CoupleQuestService service;

    public CoupleQuestController(CoupleQuestService service) {
        this.service = service;
    }

    /** 关卡总览（F370-F379 聚合）。 */
    @GetMapping("/board")
    public ApiResponse<CoupleQuestService.QuestVO> board(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
    }

    /** F378 关卡成就墙（按年聚合，year 缺省当年）。 */
    @GetMapping("/wall")
    public ApiResponse<CoupleQuestService.WallVO> wall(@RequestParam(required = false) String year,
                                                       HttpSession session) {
        return ApiResponse.ok(service.wall(Sessions.requireUser(session), year));
    }

    /** F370 宣布一场 Boss 战（day 今天或以后、kind 白名单、name ≤30、fear ≤60、在途每人 ≤3）。 */
    public record BattleRequest(String day, String kind, String name, String fear) {
    }

    @PostMapping("/battle")
    public ApiResponse<CoupleQuestService.QuestVO> battle(@RequestBody BattleRequest req, HttpSession session) {
        return ApiResponse.ok(service.addBattle(Sessions.requireUser(session), req.day(), req.kind(),
                req.name(), req.fear()));
    }

    public record IdRequest(String id) {
    }

    /** F370 撤掉自己挂的在途关卡。 */
    @PostMapping("/battle/remove")
    public ApiResponse<CoupleQuestService.QuestVO> battleRemove(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.removeBattle(Sessions.requireUser(session), req.id()));
    }

    /** F371 交出关战报（WIN/LOSE/SURVIVE，一战一报，只有打这关的人能交）。 */
    public record ReportRequest(String battleId, String result, String feeling) {
    }

    @PostMapping("/report")
    public ApiResponse<CoupleQuestService.QuestVO> report(@RequestBody ReportRequest req, HttpSession session) {
        return ApiResponse.ok(service.report(Sessions.requireUser(session), req.battleId(), req.result(),
                req.feeling()));
    }

    /** F371 对方按战果盖章（庆功/抱抱/幸亏，重复盖幂等）。 */
    @PostMapping("/report/seal")
    public ApiResponse<CoupleQuestService.QuestVO> seal(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.seal(Sessions.requireUser(session), req.id()));
    }

    /** F372 预报今晚忙到几点（13-23 钳制，note ≤40，每人每天一行可改写）。 */
    public record OvertimeRequest(Integer untilHour, String note) {
    }

    @PostMapping("/overtime")
    public ApiResponse<CoupleQuestService.QuestVO> overtime(@RequestBody OvertimeRequest req, HttpSession session) {
        return ApiResponse.ok(service.overtime(Sessions.requireUser(session), req.untilHour(), req.note()));
    }

    /** F372 给对方留一张到家灯卡（只有对方能留）。 */
    public record LampRequest(String id, String text) {
    }

    @PostMapping("/overtime/lamp")
    public ApiResponse<CoupleQuestService.QuestVO> lamp(@RequestBody LampRequest req, HttpSession session) {
        return ApiResponse.ok(service.leaveLamp(Sessions.requireUser(session), req.id(), req.text()));
    }

    /** F373 为 TA 开生病陪护单（症状 ≤60，在途每人 ≤1）。 */
    public record NurseRequest(String symptom) {
    }

    @PostMapping("/nurse")
    public ApiResponse<CoupleQuestService.QuestVO> nurse(@RequestBody NurseRequest req, HttpSession session) {
        return ApiResponse.ok(service.openNurse(Sessions.requireUser(session), req.symptom()));
    }

    /** F373 陪护人代记一次喝水/吃药（一天每种只记一次）。 */
    public record CareMarkRequest(String nurseId, String kind) {
    }

    @PostMapping("/nurse/mark")
    public ApiResponse<CoupleQuestService.QuestVO> nurseMark(@RequestBody CareMarkRequest req, HttpSession session) {
        return ApiResponse.ok(service.careMark(Sessions.requireUser(session), req.nurseId(), req.kind()));
    }

    /** F373 陪护人写病中留言（≤80 字）。 */
    public record NurseMessageRequest(String nurseId, String text) {
    }

    @PostMapping("/nurse/message")
    public ApiResponse<CoupleQuestService.QuestVO> nurseMessage(@RequestBody NurseMessageRequest req,
                                                                HttpSession session) {
        return ApiResponse.ok(service.nurseMessage(Sessions.requireUser(session), req.nurseId(), req.text()));
    }

    /** F373 病人自己宣布痊愈关单（陪护人不能替 TA 宣布）。 */
    @PostMapping("/nurse/close")
    public ApiResponse<CoupleQuestService.QuestVO> nurseClose(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.closeNurse(Sessions.requireUser(session), req.id()));
    }

    /** F374 宣布进静音舱（出舱日必须晚于今天，一人一个在途舱）。 */
    public record PodRequest(String untilDay) {
    }

    @PostMapping("/pod")
    public ApiResponse<CoupleQuestService.QuestVO> pod(@RequestBody PodRequest req, HttpSession session) {
        return ApiResponse.ok(service.enterPod(Sessions.requireUser(session), req.untilDay()));
    }

    /** F374 给对方递一张加油卡（舱外的人才能递，每天一张）。 */
    @PostMapping("/pod/cheer")
    public ApiResponse<CoupleQuestService.QuestVO> podCheer(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.cheerPod(Sessions.requireUser(session), req.id()));
    }

    /** F374 本人出舱（提醒对方补一封长信）。 */
    @PostMapping("/pod/out")
    public ApiResponse<CoupleQuestService.QuestVO> podOut(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.leavePod(Sessions.requireUser(session), req.id()));
    }

    /** F374 对方标记长信已补。 */
    @PostMapping("/pod/letter")
    public ApiResponse<CoupleQuestService.QuestVO> podLetter(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.podLetterDone(Sessions.requireUser(session), req.id()));
    }

    /** F375 给搬家区块起名（slot 1-8，name ≤20）。 */
    public record MoveNameRequest(Integer slot, String name) {
    }

    @PostMapping("/move/name")
    public ApiResponse<CoupleQuestService.QuestVO> moveName(@RequestBody MoveNameRequest req, HttpSession session) {
        return ApiResponse.ok(service.moveName(Sessions.requireUser(session), req.slot(), req.name()));
    }

    /** F375 认领/松手某一格（已被对方认领 400）。 */
    public record MoveSlotRequest(Integer slot) {
    }

    @PostMapping("/move/claim")
    public ApiResponse<CoupleQuestService.QuestVO> moveClaim(@RequestBody MoveSlotRequest req, HttpSession session) {
        return ApiResponse.ok(service.moveClaim(Sessions.requireUser(session), req.slot()));
    }

    /** F375 记这一格打包了几箱（只有认领人能填，0-99 钳制）。 */
    public record MoveBoxesRequest(Integer slot, Integer boxes) {
    }

    @PostMapping("/move/boxes")
    public ApiResponse<CoupleQuestService.QuestVO> moveBoxes(@RequestBody MoveBoxesRequest req, HttpSession session) {
        return ApiResponse.ok(service.moveBoxes(Sessions.requireUser(session), req.slot(), req.boxes()));
    }

    /** F375 这一格打包完成（只有认领人能勾）。 */
    @PostMapping("/move/done")
    public ApiResponse<CoupleQuestService.QuestVO> moveDone(@RequestBody MoveSlotRequest req, HttpSession session) {
        return ApiResponse.ok(service.moveDone(Sessions.requireUser(session), req.slot()));
    }

    /** F375 新家第一晚打卡（双人才算庆祝，note ≤60）。 */
    public record MoveNightRequest(String day, String note) {
    }

    @PostMapping("/move/night")
    public ApiResponse<CoupleQuestService.QuestVO> moveNight(@RequestBody MoveNightRequest req, HttpSession session) {
        return ApiResponse.ok(service.moveNight(Sessions.requireUser(session), req.day(), req.note()));
    }

    /** F376 本人宣布进低谷（回升日 7-30 天后，在途每人 ≤1）。 */
    @PostMapping("/valley")
    public ApiResponse<CoupleQuestService.QuestVO> valley(@RequestBody PodRequest req, HttpSession session) {
        return ApiResponse.ok(service.openValley(Sessions.requireUser(session), req.untilDay()));
    }

    /** F376 递一张「不说话也行」卡（只有对方能递，一天一张）。 */
    @PostMapping("/valley/care")
    public ApiResponse<CoupleQuestService.QuestVO> valleyCare(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.valleyCare(Sessions.requireUser(session), req.id()));
    }

    /** F376 本人宣布回升收尾。 */
    @PostMapping("/valley/rise")
    public ApiResponse<CoupleQuestService.QuestVO> valleyRise(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.valleyRise(Sessions.requireUser(session), req.id()));
    }

    /** F377 记今天做成的一件小事（≤40 字，每人每天一条）。 */
    public record WinRequest(String content, String day) {
    }

    @PostMapping("/win")
    public ApiResponse<CoupleQuestService.QuestVO> win(@RequestBody WinRequest req, HttpSession session) {
        return ApiResponse.ok(service.addWin(Sessions.requireUser(session), req.content(), req.day()));
    }

    /** F377 互颁小赢奖（只能颁对方的记录，一人一周一颁）。 */
    @PostMapping("/win/award")
    public ApiResponse<CoupleQuestService.QuestVO> winAward(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.awardWin(Sessions.requireUser(session), req.id()));
    }

    /** F379 挂一个未来 60 天内的关口（title ≤30）。 */
    public record UpcomingRequest(String day, String title) {
    }

    @PostMapping("/upcoming")
    public ApiResponse<CoupleQuestService.QuestVO> upcoming(@RequestBody UpcomingRequest req, HttpSession session) {
        return ApiResponse.ok(service.addUpcoming(Sessions.requireUser(session), req.day(), req.title()));
    }

    /** F379 对方点「我会到场」（只有非挂单人能点）。 */
    @PostMapping("/upcoming/attend")
    public ApiResponse<CoupleQuestService.QuestVO> upcomingAttend(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.attendUpcoming(Sessions.requireUser(session), req.id()));
    }

    /** F379 撤掉自己挂的关口。 */
    @PostMapping("/upcoming/remove")
    public ApiResponse<CoupleQuestService.QuestVO> upcomingRemove(@RequestBody IdRequest req, HttpSession session) {
        return ApiResponse.ok(service.removeUpcoming(Sessions.requireUser(session), req.id()));
    }
}
