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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 回忆资产·收藏系（F83/F88/F89）：甜蜜语录收藏册 / 恋爱电影票根 / 我们的歌单。
 */
@RestController
@RequestMapping("/api/couple/keepsake")
public class CoupleKeepsakeController {

    public record QuoteRequest(String content, String context) {
    }

    public record TicketRequest(String title, String watchDay, Integer rating, String comment) {
    }

    public record SongRequest(String title, String artist, String reason) {
    }

    private final CoupleKeepsakeService keepsakeService;

    public CoupleKeepsakeController(CoupleKeepsakeService keepsakeService) {
        this.keepsakeService = keepsakeService;
    }

    // ---------- F83 甜蜜语录收藏册 ----------

    @GetMapping("/quotes")
    public ApiResponse<List<CoupleKeepsakeService.QuoteVO>> quotes(HttpSession session) {
        return ApiResponse.ok(keepsakeService.quotes(Sessions.requireUser(session)));
    }

    /** 收藏一句甜话（可记场景）。 */
    @PostMapping("/quotes")
    public ApiResponse<List<CoupleKeepsakeService.QuoteVO>> saveQuote(@RequestBody QuoteRequest req, HttpSession session) {
        return ApiResponse.ok(keepsakeService.saveQuote(Sessions.requireUser(session), req.content(), req.context()));
    }

    /** 删除语录。 */
    @DeleteMapping("/quotes/{id}")
    public ApiResponse<List<CoupleKeepsakeService.QuoteVO>> removeQuote(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(keepsakeService.removeQuote(Sessions.requireUser(session), id));
    }

    // ---------- F88 恋爱电影票根 ----------

    @GetMapping("/tickets")
    public ApiResponse<List<CoupleKeepsakeService.TicketVO>> tickets(HttpSession session) {
        return ApiResponse.ok(keepsakeService.tickets(Sessions.requireUser(session)));
    }

    /** 存一张票根。 */
    @PostMapping("/tickets")
    public ApiResponse<List<CoupleKeepsakeService.TicketVO>> saveTicket(@RequestBody TicketRequest req, HttpSession session) {
        return ApiResponse.ok(keepsakeService.saveTicket(Sessions.requireUser(session),
                req.title(), req.watchDay(), req.rating(), req.comment()));
    }

    /** 撕掉票根。 */
    @DeleteMapping("/tickets/{id}")
    public ApiResponse<List<CoupleKeepsakeService.TicketVO>> removeTicket(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(keepsakeService.removeTicket(Sessions.requireUser(session), id));
    }

    // ---------- F89 我们的歌单 ----------

    @GetMapping("/songs")
    public ApiResponse<List<CoupleKeepsakeService.SongVO>> songs(HttpSession session) {
        return ApiResponse.ok(keepsakeService.songs(Sessions.requireUser(session)));
    }

    /** 收藏一首我们的歌。 */
    @PostMapping("/songs")
    public ApiResponse<List<CoupleKeepsakeService.SongVO>> saveSong(@RequestBody SongRequest req, HttpSession session) {
        return ApiResponse.ok(keepsakeService.saveSong(Sessions.requireUser(session),
                req.title(), req.artist(), req.reason()));
    }

    /** 从歌单移除。 */
    @DeleteMapping("/songs/{id}")
    public ApiResponse<List<CoupleKeepsakeService.SongVO>> removeSong(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(keepsakeService.removeSong(Sessions.requireUser(session), id));
    }
}
