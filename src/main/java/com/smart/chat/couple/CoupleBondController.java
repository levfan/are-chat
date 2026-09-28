package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 贴贴互动：亲密小动作 / 心情回应 / 专属爱称。
 */
@RestController
@RequestMapping("/api/couple/bond")
public class CoupleBondController {

    public record ActionRequest(String kind) {
    }

    public record MoodReactionRequest(String day, String reaction) {
    }

    public record PetNameRequest(String name) {
    }

    private final CoupleBondService bondService;

    public CoupleBondController(CoupleBondService bondService) {
        this.bondService = bondService;
    }

    /** 发送一个贴贴动作（戳一戳/抱抱/亲亲/捏捏脸/蹭蹭/挠痒痒/在想你）。 */
    @PostMapping("/actions")
    public ApiResponse<CoupleBondService.BondStatsVO> sendAction(@RequestBody ActionRequest req,
                                                                 HttpSession session) {
        return ApiResponse.ok(bondService.sendAction(Sessions.requireUser(session), req.kind()));
    }

    /** 最近动作流（新→旧，默认 50 条）。 */
    @GetMapping("/actions")
    public ApiResponse<List<CoupleBondService.ActionVO>> actions(
            @RequestParam(required = false) Integer limit, HttpSession session) {
        return ApiResponse.ok(bondService.recentActions(Sessions.requireUser(session), limit));
    }

    /** 贴贴统计：各类动作累计/双方占比/最近时间 + 今日双方动作数。 */
    @GetMapping("/stats")
    public ApiResponse<CoupleBondService.BondStatsVO> stats(HttpSession session) {
        return ApiResponse.ok(bondService.stats(Sessions.requireUser(session)));
    }

    /** 回应 TA 某天的心情（默认今天）：抱抱/亲亲/加油/摸摸头。 */
    @PostMapping("/mood-reactions")
    public ApiResponse<CoupleBondService.MoodReactionVO> reactMood(@RequestBody MoodReactionRequest req,
                                                                   HttpSession session) {
        return ApiResponse.ok(bondService.reactMood(Sessions.requireUser(session), req.day(), req.reaction()));
    }

    /** 某天（默认今天）双方给彼此心情的回应。 */
    @GetMapping("/mood-reactions")
    public ApiResponse<CoupleBondService.MoodReactionVO> moodReactions(
            @RequestParam(required = false) String day, HttpSession session) {
        return ApiResponse.ok(bondService.moodReactions(Sessions.requireUser(session), day));
    }

    /** 给 TA 设置专属爱称（空串清除）。 */
    @PutMapping("/pet-name")
    public ApiResponse<String> setPetName(@RequestBody PetNameRequest req, HttpSession session) {
        return ApiResponse.ok(bondService.setPetName(Sessions.requireUser(session), req.name()));
    }
}
