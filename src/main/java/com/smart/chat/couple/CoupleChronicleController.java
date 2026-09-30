package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 回忆资产·聚合系（F80/F81/F82/F85/F86）：恋爱编年史 / 考古卡 / 恋爱问答机 / 周年报告 / 生日回顾。
 */
@RestController
@RequestMapping("/api/couple/chronicle")
public class CoupleChronicleController {

    private final CoupleChronicleService chronicleService;

    public CoupleChronicleController(CoupleChronicleService chronicleService) {
        this.chronicleService = chronicleService;
    }

    /** 恋爱编年史：按年聚合的全部值得记住的事。 */
    @GetMapping
    public ApiResponse<List<CoupleChronicleService.ChronicleYearVO>> chronicle(HttpSession session) {
        return ApiResponse.ok(chronicleService.chronicle(Sessions.requireUser(session)));
    }

    /** 考古卡：随机挖一张 30 天前的旧记录（不足 30 天则从全部记录里挖）。 */
    @GetMapping("/archaeology")
    public ApiResponse<CoupleChronicleService.ArchaeologyCardVO> archaeology(HttpSession session) {
        return ApiResponse.ok(chronicleService.archaeology(Sessions.requireUser(session)));
    }

    /** 恋爱问答机：基于真实数据出 2-3 道选择题。 */
    @GetMapping("/quiz")
    public ApiResponse<List<CoupleChronicleService.QuizQuestionVO>> quiz(HttpSession session) {
        return ApiResponse.ok(chronicleService.quiz(Sessions.requireUser(session)));
    }

    /** 周年报告：最近一个周年以来的「这一年我们」。 */
    @GetMapping("/anniversary-report")
    public ApiResponse<CoupleChronicleService.AnniversaryReportVO> anniversaryReport(HttpSession session) {
        return ApiResponse.ok(chronicleService.anniversaryReport(Sessions.requireUser(session)));
    }

    /** 生日回顾：TA 生日那天，历史上发生过什么。 */
    @GetMapping("/birthday-look")
    public ApiResponse<CoupleChronicleService.BirthdayLookVO> birthdayLook(HttpSession session) {
        return ApiResponse.ok(chronicleService.birthdayLook(Sessions.requireUser(session)));
    }
}
