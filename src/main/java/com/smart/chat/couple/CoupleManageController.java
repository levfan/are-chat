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

/** 生活经营系（F180-F189）：家庭会议、本周主理人、技能交换、月度互评、应急卡、存档点、积分市场、五年计划、策划案、经营周报。 */
@RestController
@RequestMapping("/api/couple/manage")
public class CoupleManageController {

    private final CoupleManageService service;

    public CoupleManageController(CoupleManageService service) {
        this.service = service;
    }

    /** F180 会议列表。 */
    @GetMapping("/meetings")
    public ApiResponse<List<CoupleManageService.MeetingVO>> meetings(HttpSession session) {
        return ApiResponse.ok(service.meetings(Sessions.requireUser(session)));
    }

    /** F180 提出议题。 */
    public record MeetingRequest(String topic, String followDay) {
    }

    @PostMapping("/meetings")
    public ApiResponse<List<CoupleManageService.MeetingVO>> addMeeting(@RequestBody MeetingRequest req, HttpSession session) {
        return ApiResponse.ok(service.addMeeting(Sessions.requireUser(session), req.topic(), req.followDay()));
    }

    /** F180 补充决议。 */
    public record DecisionRequest(String decision, String followDay) {
    }

    @PostMapping("/meetings/{id}/decision")
    public ApiResponse<List<CoupleManageService.MeetingVO>> updateMeeting(@PathVariable String id, @RequestBody DecisionRequest req, HttpSession session) {
        return ApiResponse.ok(service.updateMeeting(Sessions.requireUser(session), id, req.decision(), req.followDay()));
    }

    /** F180 关闭议题。 */
    @PostMapping("/meetings/{id}/close")
    public ApiResponse<List<CoupleManageService.MeetingVO>> closeMeeting(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.closeMeeting(Sessions.requireUser(session), id));
    }

    /** F181 本周主理人。 */
    @GetMapping("/host")
    public ApiResponse<CoupleManageService.WeekHostVO> host(HttpSession session) {
        return ApiResponse.ok(service.host(Sessions.requireUser(session)));
    }

    /** F181 主理人排小计划。 */
    public record HostPlanRequest(String plan) {
    }

    @PostMapping("/host/plan")
    public ApiResponse<CoupleManageService.WeekHostVO> saveHostPlan(@RequestBody HostPlanRequest req, HttpSession session) {
        return ApiResponse.ok(service.saveHostPlan(Sessions.requireUser(session), req.plan()));
    }

    /** F182 技能交换列表。 */
    @GetMapping("/skills")
    public ApiResponse<List<CoupleManageService.SkillVO>> skills(HttpSession session) {
        return ApiResponse.ok(service.skills(Sessions.requireUser(session)));
    }

    /** F182 挂牌交换。 */
    public record SkillRequest(String teach, String learn) {
    }

    @PostMapping("/skills")
    public ApiResponse<List<CoupleManageService.SkillVO>> addSkill(@RequestBody SkillRequest req, HttpSession session) {
        return ApiResponse.ok(service.addSkill(Sessions.requireUser(session), req.teach(), req.learn()));
    }

    /** F182 成交接招。 */
    @PostMapping("/skills/{id}/take")
    public ApiResponse<List<CoupleManageService.SkillVO>> takeSkill(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.takeSkill(Sessions.requireUser(session), id));
    }

    /** F182 教学两清。 */
    @PostMapping("/skills/{id}/done")
    public ApiResponse<List<CoupleManageService.SkillVO>> doneSkill(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.doneSkill(Sessions.requireUser(session), id));
    }

    /** F183 本月互评对照。 */
    @GetMapping("/month-reviews")
    public ApiResponse<CoupleManageService.MonthReviewPairVO> monthReviews(HttpSession session) {
        return ApiResponse.ok(service.monthReviews(Sessions.requireUser(session)));
    }

    /** F183 提交月度互评。 */
    public record MonthReviewRequest(Integer stars, String advice) {
    }

    @PostMapping("/month-reviews")
    public ApiResponse<CoupleManageService.MonthReviewPairVO> saveMonthReview(@RequestBody MonthReviewRequest req, HttpSession session) {
        return ApiResponse.ok(service.saveMonthReview(Sessions.requireUser(session), req.stars(), req.advice()));
    }

    /** F184 双方应急卡。 */
    @GetMapping("/emergency-cards")
    public ApiResponse<List<CoupleManageService.EmergencyCardVO>> emergencyCards(HttpSession session) {
        return ApiResponse.ok(service.emergencyCards(Sessions.requireUser(session)));
    }

    /** F184 填写/更新我的应急卡。 */
    public record EmergencyCardRequest(String contacts, String keysPlace, String medicine) {
    }

    @PostMapping("/emergency-card")
    public ApiResponse<List<CoupleManageService.EmergencyCardVO>> saveEmergencyCard(@RequestBody EmergencyCardRequest req, HttpSession session) {
        return ApiResponse.ok(service.saveEmergencyCard(Sessions.requireUser(session), req.contacts(), req.keysPlace(), req.medicine()));
    }

    /** F185 存档点列表。 */
    @GetMapping("/snapshots")
    public ApiResponse<List<CoupleManageService.SnapshotVO>> snapshots(HttpSession session) {
        return ApiResponse.ok(service.snapshots(Sessions.requireUser(session)));
    }

    /** F185 存本月的档。 */
    public record SnapshotRequest(Integer loveTemp, String work, String health) {
    }

    @PostMapping("/snapshots")
    public ApiResponse<List<CoupleManageService.SnapshotVO>> saveSnapshot(@RequestBody SnapshotRequest req, HttpSession session) {
        return ApiResponse.ok(service.saveSnapshot(Sessions.requireUser(session), req.loveTemp(), req.work(), req.health()));
    }

    /** F186 积分账户（余额+商店+流水）。 */
    @GetMapping("/points")
    public ApiResponse<CoupleManageService.PointAccountVO> points(HttpSession session) {
        return ApiResponse.ok(service.points(Sessions.requireUser(session)));
    }

    /** F186 做家务赚积分。 */
    public record EarnRequest(String item, Integer points) {
    }

    @PostMapping("/points/earn")
    public ApiResponse<CoupleManageService.PointAccountVO> earnPoints(@RequestBody EarnRequest req, HttpSession session) {
        return ApiResponse.ok(service.earnPoints(Sessions.requireUser(session), req.item(), req.points()));
    }

    /** F186 兑换小奖励。 */
    public record RedeemRequest(String rewardCode) {
    }

    @PostMapping("/points/redeem")
    public ApiResponse<CoupleManageService.PointAccountVO> redeemReward(@RequestBody RedeemRequest req, HttpSession session) {
        return ApiResponse.ok(service.redeemReward(Sessions.requireUser(session), req.rewardCode()));
    }

    /** F187 五年计划双轨列表。 */
    @GetMapping("/five-year-plans")
    public ApiResponse<List<CoupleManageService.FiveYearVO>> fiveYearPlans(HttpSession session) {
        return ApiResponse.ok(service.fiveYearPlans(Sessions.requireUser(session)));
    }

    /** F187 写一条五年之约。 */
    public record FiveYearRequest(String track, String content) {
    }

    @PostMapping("/five-year-plans")
    public ApiResponse<List<CoupleManageService.FiveYearVO>> addFiveYearPlan(@RequestBody FiveYearRequest req, HttpSession session) {
        return ApiResponse.ok(service.addFiveYearPlan(Sessions.requireUser(session), req.track(), req.content()));
    }

    /** F187 认领 OURS 约定。 */
    @PostMapping("/five-year-plans/{id}/claim")
    public ApiResponse<List<CoupleManageService.FiveYearVO>> claimPlan(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.claimPlan(Sessions.requireUser(session), id));
    }

    /** F187 标记达成。 */
    @PostMapping("/five-year-plans/{id}/finish")
    public ApiResponse<List<CoupleManageService.FiveYearVO>> finishPlan(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.finishPlan(Sessions.requireUser(session), id));
    }

    /** F188 策划案列表。 */
    @GetMapping("/anniv-plans")
    public ApiResponse<List<CoupleManageService.AnnivPlanVO>> annivPlans(HttpSession session) {
        return ApiResponse.ok(service.annivPlans(Sessions.requireUser(session)));
    }

    /** F188 立一份策划案。 */
    public record AnnivPlanRequest(String day, String title, String idea) {
    }

    @PostMapping("/anniv-plans")
    public ApiResponse<List<CoupleManageService.AnnivPlanVO>> addAnnivPlan(@RequestBody AnnivPlanRequest req, HttpSession session) {
        return ApiResponse.ok(service.addAnnivPlan(Sessions.requireUser(session), req.day(), req.title(), req.idea()));
    }

    /** F188 推进状态 IDEA→LOCKED→DONE。 */
    @PostMapping("/anniv-plans/{id}/advance")
    public ApiResponse<List<CoupleManageService.AnnivPlanVO>> advanceAnnivPlan(@PathVariable String id, HttpSession session) {
        return ApiResponse.ok(service.advanceAnnivPlan(Sessions.requireUser(session), id));
    }

    /** F189 经营周报。 */
    @GetMapping("/weekly")
    public ApiResponse<CoupleManageService.ManageWeeklyVO> weekly(HttpSession session) {
        return ApiResponse.ok(service.weekly(Sessions.requireUser(session)));
    }
}
