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

/** 你画我猜（系统裁剪后沟通增强唯一保留项）。 */
@RestController
@RequestMapping("/api/couple/comm")
public class CoupleCommController {

    private final CoupleCommService commService;

    public CoupleCommController(CoupleCommService commService) {
        this.commService = commService;
    }

    /** 本轮词、线索与猜测。 */
    @GetMapping("/guesses")
    public ApiResponse<List<CoupleCommService.GuessVO>> guesses(HttpSession session) {
        return ApiResponse.ok(commService.guessRounds(Sessions.requireUser(session)));
    }

    /** 本轮抽词出题（词由内容库按空间+日稳定给出）。 */
    @PostMapping("/guesses")
    public ApiResponse<List<CoupleCommService.GuessVO>> startGuess(HttpSession session) {
        return ApiResponse.ok(commService.startGuess(Sessions.requireUser(session)));
    }

    /** 出题人补一句线索。 */
    public record GuessClueRequest(String clue) {
    }

    @PostMapping("/guesses/{id}/clue")
    public ApiResponse<List<CoupleCommService.GuessVO>> clueGuess(@PathVariable String id,
                                                                  @RequestBody GuessClueRequest req,
                                                                  HttpSession session) {
        return ApiResponse.ok(commService.clueGuess(Sessions.requireUser(session), id, req.clue()));
    }

    /** 猜一次，猜中与否都由出题人判。 */
    public record GuessTryRequest(String guess) {
    }

    @PostMapping("/guesses/{id}/guess")
    public ApiResponse<List<CoupleCommService.GuessVO>> doGuess(@PathVariable String id,
                                                                @RequestBody GuessTryRequest req,
                                                                HttpSession session) {
        return ApiResponse.ok(commService.doGuess(Sessions.requireUser(session), id, req.guess()));
    }
}
