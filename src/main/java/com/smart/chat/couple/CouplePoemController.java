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

/** 文字浪漫系（F160-F169）：情诗接龙/三行情书/醒来第一条/漂流瓶/密码情书/灵魂提问/贴纸手账/语录机/情书模板/贴纸库。 */
@RestController
@RequestMapping("/api/couple/poem")
public class CouplePoemController {

    private final CouplePoemService service;

    public CouplePoemController(CouplePoemService service) {
        this.service = service;
    }

    /** 我们的诗（情诗接龙）。 */
    @GetMapping("/chain")
    public ApiResponse<CouplePoemService.PoemVO> poem(HttpSession session) {
        return ApiResponse.ok(service.poem(Sessions.requireUser(session)));
    }

    /** 写今天这一句诗。 */
    public record PoemLineRequest(String line) {
    }

    @PostMapping("/chain")
    public ApiResponse<CouplePoemService.PoemVO> addLine(@RequestBody PoemLineRequest req, HttpSession session) {
        return ApiResponse.ok(service.addLine(Sessions.requireUser(session), req.line()));
    }

    /** 三行情书列表。 */
    @GetMapping("/3lines")
    public ApiResponse<List<CouplePoemService.Poem3VO>> poems3(HttpSession session) {
        return ApiResponse.ok(service.poems3(Sessions.requireUser(session)));
    }

    /** 写三行情书。 */
    public record Poem3Request(String line1, String line2, String line3) {
    }

    @PostMapping("/3lines")
    public ApiResponse<List<CouplePoemService.Poem3VO>> addPoem3(@RequestBody Poem3Request req, HttpSession session) {
        return ApiResponse.ok(service.addPoem3(Sessions.requireUser(session), req.line1(), req.line2(), req.line3()));
    }

    /** 给三行情书点赞。 */
    @PostMapping("/3lines/{id}/like")
    public ApiResponse<List<CouplePoemService.Poem3VO>> likePoem3(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.likePoem3(Sessions.requireUser(session), id));
    }

    /** 醒来第一条信箱。 */
    @GetMapping("/morning-notes")
    public ApiResponse<CouplePoemService.MorningBoxVO> morningNotes(HttpSession session) {
        return ApiResponse.ok(service.morningNotes(Sessions.requireUser(session)));
    }

    /** 睡前封一条（次日送达）。 */
    public record MorningNoteRequest(String content) {
    }

    @PostMapping("/morning-notes")
    public ApiResponse<CouplePoemService.MorningBoxVO> sealMorningNote(@RequestBody MorningNoteRequest req, HttpSession session) {
        return ApiResponse.ok(service.sealMorningNote(Sessions.requireUser(session), req.content()));
    }

    /** 已读醒来第一条。 */
    @PostMapping("/morning-notes/{id}/read")
    public ApiResponse<CouplePoemService.MorningBoxVO> readMorningNote(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.readMorningNote(Sessions.requireUser(session), id));
    }

    /** 漂流瓶列表。 */
    @GetMapping("/bottles")
    public ApiResponse<List<CouplePoemService.BottleVO>> bottles(HttpSession session) {
        return ApiResponse.ok(service.bottles(Sessions.requireUser(session)));
    }

    /** 扔漂流瓶。 */
    public record BottleRequest(String mood, String content) {
    }

    @PostMapping("/bottles")
    public ApiResponse<List<CouplePoemService.BottleVO>> tossBottle(@RequestBody BottleRequest req, HttpSession session) {
        return ApiResponse.ok(service.tossBottle(Sessions.requireUser(session), req.mood(), req.content()));
    }

    /** 回漂流瓶。 */
    public record BottleReplyRequest(String reply) {
    }

    @PostMapping("/bottles/{id}/reply")
    public ApiResponse<List<CouplePoemService.BottleVO>> replyBottle(@PathVariable String id, @RequestBody BottleReplyRequest req, HttpSession session) {
        return ApiResponse.ok(service.replyBottle(Sessions.requireUser(session), id, req.reply()));
    }

    /** 密码情书列表。 */
    @GetMapping("/ciphers")
    public ApiResponse<List<CouplePoemService.CipherVO>> cipherNotes(HttpSession session) {
        return ApiResponse.ok(service.cipherNotes(Sessions.requireUser(session)));
    }

    /** 写密码情书。 */
    public record CipherRequest(String cipher, String hint) {
    }

    @PostMapping("/ciphers")
    public ApiResponse<List<CouplePoemService.CipherVO>> makeCipherNote(@RequestBody CipherRequest req, HttpSession session) {
        return ApiResponse.ok(service.makeCipherNote(Sessions.requireUser(session), req.cipher(), req.hint()));
    }

    /** 解码上报。 */
    @PostMapping("/ciphers/{id}/crack")
    public ApiResponse<List<CouplePoemService.CipherVO>> crackCipherNote(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.crackCipherNote(Sessions.requireUser(session), id));
    }

    /** 今日灵魂一问。 */
    @GetMapping("/soul")
    public ApiResponse<CouplePoemService.SoulVO> soul(HttpSession session) {
        return ApiResponse.ok(service.soul(Sessions.requireUser(session)));
    }

    /** 回答灵魂一问。 */
    public record SoulRequest(String answer) {
    }

    @PostMapping("/soul")
    public ApiResponse<CouplePoemService.SoulVO> answerSoul(@RequestBody SoulRequest req, HttpSession session) {
        return ApiResponse.ok(service.answerSoul(Sessions.requireUser(session), req.answer()));
    }

    /** 贴纸手账。 */
    @GetMapping("/journal")
    public ApiResponse<List<CouplePoemService.JournalVO>> journal(HttpSession session) {
        return ApiResponse.ok(service.journal(Sessions.requireUser(session)));
    }

    /** 写手账。 */
    public record JournalRequest(String sticker, String text) {
    }

    @PostMapping("/journal")
    public ApiResponse<List<CouplePoemService.JournalVO>> saveJournal(@RequestBody JournalRequest req, HttpSession session) {
        return ApiResponse.ok(service.saveJournal(Sessions.requireUser(session), req.sticker(), req.text()));
    }

    /** 恋爱语录机。 */
    @GetMapping("/quote")
    public ApiResponse<String> quote(HttpSession session) {
        return ApiResponse.ok(service.quote(Sessions.requireUser(session)));
    }

    /** 情书模板库。 */
    @GetMapping("/letter-templates")
    public ApiResponse<List<CouplePoemBank.LetterTemplate>> letterTemplates() {
        return ApiResponse.ok(service.letterTemplates());
    }

    /** 手账贴纸库。 */
    @GetMapping("/stickers")
    public ApiResponse<List<String>> stickers() {
        return ApiResponse.ok(service.stickers());
    }
}
