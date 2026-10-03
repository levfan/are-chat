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

    public record IdRequest(String id) {
    }

    /** F372 预报今晚忙到几点（13-23 钳制，note ≤40，每人每天一行可改写）。 */
    public record OvertimeRequest(Integer untilHour, String note) {
    }

    @PostMapping("/overtime")
    public ApiResponse<CoupleQuestService.QuestVO> overtime(@RequestBody OvertimeRequest req, HttpSession session) {
        return ApiResponse.ok(service.overtime(Sessions.requireUser(session), req.untilHour(), req.note()));
    }

    /** 给谁留灯（那条加班预报的 id）与灯语。 */
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

}
