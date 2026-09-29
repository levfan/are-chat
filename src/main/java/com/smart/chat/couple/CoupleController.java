package com.smart.chat.couple;

import com.smart.chat.common.ApiResponse;
import com.smart.chat.common.Sessions;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 情侣空间：邀请建立 → 双向约定 → 每日小仪式 → 共享空间。
 */
@RestController
@RequestMapping("/api/couple")
public class CoupleController {

    public record InviteRequest(String username, String message) {
    }

    public record AnniversaryDateRequest(String date) {
    }

    public record PromiseCreateRequest(String side, String content, Long dueAt) {
    }

    public record CheckinRequest(String kind) {
    }

    public record AnswerRequest(String answer) {
    }

    public record ItemSaveRequest(String kind, String title, String note, String dueDate, Boolean done) {
    }

    public record AnniversaryCreateRequest(String title, String date, Boolean yearly) {
    }

    public record MoodSaveRequest(String mood, String note) {
    }

    public record LetterCreateRequest(String content, Long deliverAt) {
    }

    public record PactCreateRequest(String content) {
    }

    public record CitySetRequest(String city) {
    }

    public record FundCreateRequest(String title, Long targetAmount) {
    }

    public record FundDepositRequest(Long amount, String note) {
    }

    /** 空间个性化：宣言 / 主题 / 贴纸墙（传 null 表示该项不修改）。 */
    public record ProfileUpdateRequest(String slogan, String theme, String stickers) {
    }

    private final CoupleService coupleService;

    public CoupleController(CoupleService coupleService) {
        this.coupleService = coupleService;
    }

    // ---------- 建立流程 ----------

    /** 总览：未建立时返回待处理邀请（指引建立）；建立后返回空间、双方仪式状态与逾期数。 */
    @GetMapping("/overview")
    public ApiResponse<CoupleService.OverviewVO> overview(HttpSession session) {
        return ApiResponse.ok(coupleService.overview(Sessions.requireUser(session)));
    }

    @PostMapping("/invites")
    public ApiResponse<CoupleService.InviteVO> invite(@RequestBody InviteRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.invite(Sessions.requireUser(session), req.username(), req.message()));
    }

    @PostMapping("/invites/{id}/accept")
    public ApiResponse<CoupleService.SpaceVO> accept(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(coupleService.accept(Sessions.requireUser(session), id));
    }

    @PostMapping("/invites/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable String id, HttpSession session) {
        coupleService.reject(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    @DeleteMapping("/invites/{id}")
    public ApiResponse<Void> cancel(@PathVariable String id, HttpSession session) {
        coupleService.cancel(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    /** 在一起纪念日（用于计算在一起天数，双方都可改）。 */
    @PutMapping("/anniversary")
    public ApiResponse<CoupleService.SpaceVO> setAnniversary(@RequestBody AnniversaryDateRequest req,
                                                             HttpSession session) {
        return ApiResponse.ok(coupleService.setAnniversary(Sessions.requireUser(session), req.date()));
    }

    /** 空间个性化：我们的宣言 / 空间主题 / 贴纸墙佩戴。 */
    @PutMapping("/profile")
    public ApiResponse<CoupleService.SpaceVO> updateProfile(@RequestBody ProfileUpdateRequest req,
                                                            HttpSession session) {
        return ApiResponse.ok(coupleService.updateProfile(Sessions.requireUser(session),
                req.slogan(), req.theme(), req.stickers()));
    }

    /** F44 恋爱中徽章：查某人是否在恋爱中 + 在一起天数（好友资料卡展示）。 */
    @GetMapping("/relationship-of/{username}")
    public ApiResponse<CoupleService.RelationshipVO> relationshipOf(@PathVariable String username,
                                                                    HttpSession session) {
        return ApiResponse.ok(coupleService.relationshipOf(Sessions.requireUser(session), username));
    }

    @PostMapping("/dissolve")
    public ApiResponse<Void> dissolve(HttpSession session) {
        coupleService.dissolve(Sessions.requireUser(session));
        return ApiResponse.ok();
    }

    // ---------- 1. 双向待办 / 约定 ----------

    @GetMapping("/promises")
    public ApiResponse<List<CoupleService.PromiseVO>> promises(HttpSession session) {
        return ApiResponse.ok(coupleService.listPromises(Sessions.requireUser(session)));
    }

    @PostMapping("/promises")
    public ApiResponse<CoupleService.PromiseVO> createPromise(@RequestBody PromiseCreateRequest req,
                                                              HttpSession session) {
        return ApiResponse.ok(coupleService.createPromise(Sessions.requireUser(session),
                req.side(), req.content(), req.dueAt()));
    }

    @PostMapping("/promises/{id}/done")
    public ApiResponse<CoupleService.PromiseVO> donePromise(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(coupleService.donePromise(Sessions.requireUser(session), id));
    }

    @PostMapping("/promises/{id}/undone")
    public ApiResponse<CoupleService.PromiseVO> undonePromise(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(coupleService.undonePromise(Sessions.requireUser(session), id));
    }

    @DeleteMapping("/promises/{id}")
    public ApiResponse<Void> deletePromise(@PathVariable String id, HttpSession session) {
        coupleService.deletePromise(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    // ---------- 2. 每日小仪式 ----------

    @PostMapping("/checkins")
    public ApiResponse<CoupleService.CheckinStateVO> checkin(@RequestBody CheckinRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.checkin(Sessions.requireUser(session), req.kind()));
    }

    @GetMapping("/question")
    public ApiResponse<CoupleService.QuestionVO> question(HttpSession session) {
        return ApiResponse.ok(coupleService.todayQuestion(Sessions.requireUser(session)));
    }

    @PostMapping("/question")
    public ApiResponse<CoupleService.QuestionVO> answer(@RequestBody AnswerRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.answerQuestion(Sessions.requireUser(session), req.answer()));
    }

    // ---------- 3. 共享空间 ----------

    @GetMapping("/items")
    public ApiResponse<List<CoupleService.ItemVO>> items(HttpSession session) {
        return ApiResponse.ok(coupleService.listItems(Sessions.requireUser(session)));
    }

    @PostMapping("/items")
    public ApiResponse<CoupleService.ItemVO> createItem(@RequestBody ItemSaveRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.createItem(Sessions.requireUser(session),
                req.kind(), req.title(), req.note(), req.dueDate()));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<CoupleService.ItemVO> updateItem(@PathVariable String id, @RequestBody ItemSaveRequest req,
                                                        HttpSession session) {
        return ApiResponse.ok(coupleService.updateItem(Sessions.requireUser(session), id,
                req.title(), req.note(), req.dueDate(), req.done()));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Void> deleteItem(@PathVariable String id, HttpSession session) {
        coupleService.deleteItem(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    @GetMapping("/anniversaries")
    public ApiResponse<List<CoupleService.AnniversaryVO>> anniversaries(HttpSession session) {
        return ApiResponse.ok(coupleService.listAnniversaries(Sessions.requireUser(session)));
    }

    @PostMapping("/anniversaries")
    public ApiResponse<CoupleService.AnniversaryVO> createAnniversary(@RequestBody AnniversaryCreateRequest req,
                                                                      HttpSession session) {
        return ApiResponse.ok(coupleService.createAnniversary(Sessions.requireUser(session),
                req.title(), req.date(), req.yearly()));
    }

    @DeleteMapping("/anniversaries/{id}")
    public ApiResponse<Void> deleteAnniversary(@PathVariable String id, HttpSession session) {
        coupleService.deleteAnniversary(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    // ---------- 4. 心情日记 ----------

    /** 记录/修改今天的心情（每人每天一条，重复提交视为修改）。 */
    @PostMapping("/moods")
    public ApiResponse<CoupleService.MoodVO> saveMood(@RequestBody MoodSaveRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.saveMood(Sessions.requireUser(session), req.mood(), req.note()));
    }

    /** 双方最近 N 天的心情（1-90，默认 14），按日期新→旧。 */
    @GetMapping("/moods")
    public ApiResponse<List<CoupleService.MoodDayVO>> moods(@RequestParam(defaultValue = "14") int days,
                                                            HttpSession session) {
        return ApiResponse.ok(coupleService.listMoods(Sessions.requireUser(session), days));
    }

    // ---------- 5. 恋爱时光轴 ----------

    /** 最近 N 天（1-90，默认 30）的「我们的故事」聚合时间线。 */
    @GetMapping("/timeline")
    public ApiResponse<List<CoupleService.TimelineDay>> timeline(@RequestParam(defaultValue = "30") int days,
                                                                 HttpSession session) {
        return ApiResponse.ok(coupleService.timeline(Sessions.requireUser(session), days));
    }

    // ---------- 6. 心动值 & 恋爱等级 ----------

    @GetMapping("/intimacy")
    public ApiResponse<CoupleService.IntimacyVO> intimacy(HttpSession session) {
        return ApiResponse.ok(coupleService.intimacy(Sessions.requireUser(session)));
    }

    // ---------- 7. 悄悄话信箱 ----------

    /** 写一封悄悄话：deliverAt 空 = 立即可拆，非空 = 慢递（未来 7 天内）。 */
    @PostMapping("/letters")
    public ApiResponse<CoupleService.LetterVO> createLetter(@RequestBody LetterCreateRequest req,
                                                            HttpSession session) {
        return ApiResponse.ok(coupleService.saveLetter(Sessions.requireUser(session),
                req.content(), req.deliverAt()));
    }

    /** 信箱列表（发件+收件，新→旧；未到期慢递对收件人隐藏内容）。 */
    @GetMapping("/letters")
    public ApiResponse<List<CoupleService.LetterVO>> letters(HttpSession session) {
        return ApiResponse.ok(coupleService.listLetters(Sessions.requireUser(session)));
    }

    /** 拆信（只有收件人，且到了可拆时间）。 */
    @PostMapping("/letters/{id}/open")
    public ApiResponse<CoupleService.LetterVO> openLetter(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(coupleService.openLetter(Sessions.requireUser(session), id));
    }

    /** 撤回（只有发件人，且未被拆开）。 */
    @DeleteMapping("/letters/{id}")
    public ApiResponse<Void> deleteLetter(@PathVariable String id, HttpSession session) {
        coupleService.deleteLetter(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    // ---------- 8. 今日一问历史回顾 ----------

    /** 双方都回答过的一问存档（最近 N 天，1-90 默认 30，新→旧）。 */
    @GetMapping("/questions/history")
    public ApiResponse<List<CoupleService.QuestionHistoryVO>> questionHistory(
            @RequestParam(defaultValue = "30") int days, HttpSession session) {
        return ApiResponse.ok(coupleService.questionHistory(Sessions.requireUser(session), days));
    }

    // ---------- 9. 恋爱条约 ----------

    /** 提出一条条约（待对方盖章）。 */
    @PostMapping("/pacts")
    public ApiResponse<CoupleService.PactVO> createPact(@RequestBody PactCreateRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.createPact(Sessions.requireUser(session), req.content()));
    }

    @GetMapping("/pacts")
    public ApiResponse<List<CoupleService.PactVO>> pacts(HttpSession session) {
        return ApiResponse.ok(coupleService.listPacts(Sessions.requireUser(session)));
    }

    /** 盖章生效（只有对方能盖）。 */
    @PostMapping("/pacts/{id}/accept")
    public ApiResponse<CoupleService.PactVO> acceptPact(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(coupleService.acceptPact(Sessions.requireUser(session), id));
    }

    @DeleteMapping("/pacts/{id}")
    public ApiResponse<Void> deletePact(@PathVariable String id, HttpSession session) {
        coupleService.deletePact(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }

    // ---------- 10. 异地恋助手 ----------

    /** 设置/清空我的城市（清空传 null 或空串）。 */
    @PutMapping("/cities")
    public ApiResponse<CoupleService.CityCardVO> setCity(@RequestBody CitySetRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.setCity(Sessions.requireUser(session), req.city()));
    }

    /** 异地恋卡片：双方城市 +（都在城市库时）时差与距离。 */
    @GetMapping("/cities")
    public ApiResponse<CoupleService.CityCardVO> cities(HttpSession session) {
        return ApiResponse.ok(coupleService.cityCard(Sessions.requireUser(session)));
    }

    // ---------- 11. 心愿基金 ----------

    /** 建一个共同存钱目标（targetAmount 单位：分）。 */
    @PostMapping("/funds")
    public ApiResponse<CoupleService.FundVO> createFund(@RequestBody FundCreateRequest req, HttpSession session) {
        return ApiResponse.ok(coupleService.createFund(Sessions.requireUser(session),
                req.title(), req.targetAmount()));
    }

    @GetMapping("/funds")
    public ApiResponse<List<CoupleService.FundVO>> funds(HttpSession session) {
        return ApiResponse.ok(coupleService.listFunds(Sessions.requireUser(session)));
    }

    /** 存一笔钱（amount 单位：分；攒够自动达成并推送庆祝）。 */
    @PostMapping("/funds/{id}/deposits")
    public ApiResponse<CoupleService.FundVO> depositFund(@PathVariable String id,
                                                         @RequestBody FundDepositRequest req,
                                                         HttpSession session) {
        return ApiResponse.ok(coupleService.depositFund(Sessions.requireUser(session),
                id, req.amount(), req.note()));
    }

    @DeleteMapping("/funds/{id}")
    public ApiResponse<Void> deleteFund(@PathVariable String id, HttpSession session) {
        coupleService.deleteFund(Sessions.requireUser(session), id);
        return ApiResponse.ok();
    }
}
