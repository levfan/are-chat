package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 加班预报与留灯（保留卡 `couple-quest-overtime`）。写接口一律返回整份看板。
 */
@RestController
@RequestMapping("/api/couple/quest")
public class CoupleQuestController {

    /** 今晚忙到几点 + 一句说明。 */
    public record OvertimeRequest(Integer untilHour, String note) {
    }

    /** 给对方留灯：id 是对方今晚那行预报的 id。 */
    public record LampRequest(String id, String text) {
    }

    private final CoupleQuestService service;

    public CoupleQuestController(CoupleQuestService service) {
        this.service = service;
    }

    /** 加班看板。 */
    @GetMapping("/board")
    public ApiResponse<CoupleQuestService.QuestVO> board(HttpSession session) {
        return ApiResponse.ok(service.board(Sessions.requireUser(session)));
    }

    /** 预报今晚忙到几点。 */
    @PostMapping("/overtime")
    public ApiResponse<CoupleQuestService.QuestVO> overtime(@RequestBody OvertimeRequest req, HttpSession session) {
        return ApiResponse.ok(service.overtime(Sessions.requireUser(session), req.untilHour(), req.note()));
    }

    /** 给对方留一张到家灯卡。 */
    @PostMapping("/overtime/lamp")
    public ApiResponse<CoupleQuestService.QuestVO> lamp(@RequestBody LampRequest req, HttpSession session) {
        return ApiResponse.ok(service.leaveLamp(Sessions.requireUser(session), req.id(), req.text()));
    }
}
