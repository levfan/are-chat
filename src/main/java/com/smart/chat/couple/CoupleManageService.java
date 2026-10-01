package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * 生活经营系（F180-F189，批次十四）：家庭会议纪要、本周主理人、技能交换所、月度互评、
 * 家庭应急卡、情侣存档点、家务积分市场、五年计划双轨、纪念日策划案、经营周报。
 * 情绪价值设计：把日子过成长期事业——有会议、有轮值、有账本、有存档；
 * 浪漫不只是情话，也是「这件事我们商量着来」。
 */
@Service
public class CoupleManageService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleFamilyMeetingMapper meetingMapper;
    private final CoupleWeekHostMapper hostMapper;
    private final CoupleSkillSwapMapper skillMapper;
    private final CoupleMonthReviewMapper reviewMapper;
    private final CoupleEmergencyCardMapper cardMapper;
    private final CoupleMonthSnapshotMapper snapshotMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final CoupleFiveYearPlanMapper planMapper;
    private final CoupleAnnivPlanMapper annivMapper;
    private final ImPushService push;

    public CoupleManageService(CoupleSpaceMapper spaceMapper, CoupleFamilyMeetingMapper meetingMapper,
                               CoupleWeekHostMapper hostMapper, CoupleSkillSwapMapper skillMapper,
                               CoupleMonthReviewMapper reviewMapper, CoupleEmergencyCardMapper cardMapper,
                               CoupleMonthSnapshotMapper snapshotMapper, CouplePointLedgerMapper ledgerMapper,
                               CoupleFiveYearPlanMapper planMapper, CoupleAnnivPlanMapper annivMapper,
                               ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.meetingMapper = meetingMapper;
        this.hostMapper = hostMapper;
        this.skillMapper = skillMapper;
        this.reviewMapper = reviewMapper;
        this.cardMapper = cardMapper;
        this.snapshotMapper = snapshotMapper;
        this.ledgerMapper = ledgerMapper;
        this.planMapper = planMapper;
        this.annivMapper = annivMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record MeetingVO(String id, String week, String topic, String decision, String followDay,
                            String raisedBy, boolean mine, boolean closed, Long created) {
    }

    public record WeekHostVO(String week, String host, boolean mine, String plan) {
    }

    public record SkillVO(String id, String fromUser, boolean mine, String teach, String learn, String status, Long created) {
    }

    public record MonthReviewVO(String id, String month, String fromUser, boolean mine, Integer stars, String advice, Long updatedAt) {
    }

    public record MonthReviewPairVO(String month, MonthReviewVO mine, MonthReviewVO partner, boolean bothDone) {
    }

    public record EmergencyCardVO(String fromUser, boolean mine, String contacts, String keysPlace, String medicine, Long updatedAt) {
    }

    public record SnapshotVO(String id, String month, String fromUser, boolean mine, String work, String health,
                             Integer loveTemp, Long updatedAt) {
    }

    public record PointLedgerVO(String id, String fromUser, boolean mine, String type, String item, Integer points, Long created) {
    }

    public record RewardVO(String code, String name, String emoji, Integer points, boolean affordable) {
    }

    public record PointAccountVO(Integer balance, Integer totalEarned, List<RewardVO> rewards, List<PointLedgerVO> history) {
    }

    public record FiveYearVO(String id, String track, String fromUser, boolean mine, String content,
                             String ownerUser, boolean done, Long created) {
    }

    public record AnnivPlanVO(String id, String day, String title, String planner, boolean mine, String idea, String status, Long updatedAt) {
    }

    public record ManageWeeklyVO(Integer meetings, Integer closedMeetings, Integer earned, Integer spent,
                                 String host, String summary) {
    }

    // ========== F180 家庭会议纪要 ==========

    public List<MeetingVO> meetings(String me) {
        CoupleSpace space = requireSpace(me);
        return meetingMapper.findBySpace(space.getId()).stream().map(m -> toMeetingVO(m, me)).toList();
    }

    /** 开一个议题（当周可多次）。 */
    public List<MeetingVO> addMeeting(String me, String topic, String followDay) {
        CoupleSpace space = requireSpace(me);
        String t = trimLimit(topic, CoupleFamilyMeeting.TOPIC_MAX, "议题写 " + CoupleFamilyMeeting.TOPIC_MAX + " 字以内哦");
        if (t == null) {
            throw new BusinessException(400, "先把这周要商量的事写成一句议题 📋");
        }
        String follow = normalizeDay(followDay, "跟进日要是 2026-01-01 这样的日期哦");
        CoupleFamilyMeeting row = CoupleFamilyMeeting.of(space.getId(), currentWeek(), me, t);
        row.setFollowDay(follow);
        meetingMapper.insert(row);
        push.pushCoupleEvent("meeting-raised", me, space.partnerOf(me),
                "📋 TA 往家庭会议桌上放了个新议题：「" + t + "」，等你来商量。");
        return meetings(me);
    }

    /** 补充决议。 */
    public List<MeetingVO> updateMeeting(String me, String id, String decision, String followDay) {
        CoupleSpace space = requireSpace(me);
        CoupleFamilyMeeting row = requireMeeting(space, id);
        String d = trimLimit(decision, CoupleFamilyMeeting.DECISION_MAX, "决议写 " + CoupleFamilyMeeting.DECISION_MAX + " 字以内哦");
        row.setDecision(d == null ? "" : d);
        if (followDay != null && !followDay.isBlank()) {
            row.setFollowDay(normalizeDay(followDay, "跟进日要是 2026-01-01 这样的日期哦"));
        }
        row.setUpdatedAt(System.currentTimeMillis());
        meetingMapper.updateById(row);
        return meetings(me);
    }

    /** 关闭议题：商量完了就翻篇。 */
    public List<MeetingVO> closeMeeting(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleFamilyMeeting row = requireMeeting(space, id);
        if (Integer.valueOf(1).equals(row.getClosed())) {
            return meetings(me);
        }
        row.setClosed(1);
        row.setUpdatedAt(System.currentTimeMillis());
        meetingMapper.updateById(row);
        push.pushCoupleEventBoth("meeting-closed", me, space.getUserA(), space.getUserB(),
                "🤝 家庭会议议题「" + row.getTopic() + "」已达成一致，翻篇！");
        return meetings(me);
    }

    // ========== F181 本周主理人 ==========

    public WeekHostVO host(String me) {
        CoupleSpace space = requireSpace(me);
        String week = currentWeek();
        String host = hostOf(space, week);
        CoupleWeekHost row = hostMapper.find(space.getId(), week);
        return new WeekHostVO(week, host, host.equals(me), row == null ? "" : row.getPlan());
    }

    /** 主理人排本周小计划（只有当家的人能排）。 */
    public WeekHostVO saveHostPlan(String me, String plan) {
        CoupleSpace space = requireSpace(me);
        String week = currentWeek();
        String host = hostOf(space, week);
        if (!host.equals(me)) {
            throw new BusinessException(403, "本周是「" + host + "」当家，小计划先听 TA 排 😌");
        }
        String p = trimLimit(plan, CoupleWeekHost.PLAN_MAX, "小计划写 " + CoupleWeekHost.PLAN_MAX + " 字以内哦");
        if (p == null) {
            throw new BusinessException(400, "给这周排一件小事吧，哪怕只是周五一顿火锅 🍲");
        }
        CoupleWeekHost row = hostMapper.find(space.getId(), week);
        if (row == null) {
            row = CoupleWeekHost.of(space.getId(), week);
            row.setPlan(p);
            hostMapper.insert(row);
        } else {
            row.setPlan(p);
            row.setUpdatedAt(System.currentTimeMillis());
            hostMapper.updateById(row);
        }
        push.pushCoupleEvent("host-plan", me, space.partnerOf(me),
                "👑 本周主理人排好了小计划，就等执行啦。");
        return host(me);
    }

    /** 按周轮换：单双周各当家一次，谁也别说偏心。 */
    static String hostOf(CoupleSpace space, String week) {
        int parity = Integer.parseInt(week.substring(8, 10)) % 2;
        return parity == 0 ? space.getUserA() : space.getUserB();
    }

    // ========== F182 技能交换所 ==========

    public List<SkillVO> skills(String me) {
        CoupleSpace space = requireSpace(me);
        return skillMapper.findBySpace(space.getId()).stream()
                .map(s -> new SkillVO(s.getId(), s.getFromUser(), s.getFromUser().equals(me), s.getTeach(), s.getLearn(), s.getStatus(), s.getCreated()))
                .toList();
    }

    public List<SkillVO> addSkill(String me, String teach, String learn) {
        CoupleSpace space = requireSpace(me);
        String t = trimLimit(teach, CoupleSkillSwap.SKILL_MAX, "教学技能写 " + CoupleSkillSwap.SKILL_MAX + " 字以内哦");
        String l = trimLimit(learn, CoupleSkillSwap.SKILL_MAX, "想学技能写 " + CoupleSkillSwap.SKILL_MAX + " 字以内哦");
        if (t == null || l == null) {
            throw new BusinessException(400, "既要说清你能教什么，也要说清你想学什么 🛠️");
        }
        skillMapper.insert(CoupleSkillSwap.of(space.getId(), me, t, l));
        push.pushCoupleEvent("skill-listed", me, space.partnerOf(me),
                "🛠️ TA 在技能交换所挂了新摊位：教你「" + t + "」，想学你的「" + l + "」。");
        return skills(me);
    }

    /** 成交：对方接招。 */
    public List<SkillVO> takeSkill(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSkillSwap row = requireSkill(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己不能揭自己的摊，等 TA 来接招");
        }
        if (!CoupleSkillSwap.STATUS_OPEN.equals(row.getStatus())) {
            throw new BusinessException(400, "这笔交换已经成交啦");
        }
        row.setStatus(CoupleSkillSwap.STATUS_TAKEN);
        row.setUpdatedAt(System.currentTimeMillis());
        skillMapper.updateById(row);
        push.pushCoupleEvent("skill-taken", me, row.getFromUser(),
                "🛠️ TA 接了你的技能交换单，说干就干！");
        return skills(me);
    }

    /** 两清：互相教完，握手结账。 */
    public List<SkillVO> doneSkill(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSkillSwap row = requireSkill(space, id);
        if (!CoupleSkillSwap.STATUS_TAKEN.equals(row.getStatus())) {
            throw new BusinessException(400, "先成交才能两清哦");
        }
        row.setStatus(CoupleSkillSwap.STATUS_DONE);
        row.setUpdatedAt(System.currentTimeMillis());
        skillMapper.updateById(row);
        push.pushCoupleEventBoth("skill-done", me, space.getUserA(), space.getUserB(),
                "🎓 技能交换完成：你们又多会了一件事。");
        return skills(me);
    }

    // ========== F183 月度互评 ==========

    public MonthReviewPairVO monthReviews(String me) {
        CoupleSpace space = requireSpace(me);
        String month = YearMonth.now().toString();
        CoupleMonthReview mine = reviewMapper.find(space.getId(), month, me);
        CoupleMonthReview partner = reviewMapper.find(space.getId(), month, space.partnerOf(me));
        // 双评互见：都评完才看得到对方的星
        return new MonthReviewPairVO(month,
                mine == null ? null : toReviewVO(mine, me),
                (mine != null && partner != null) ? toReviewVO(partner, me) : null,
                mine != null && partner != null);
    }

    /** 给本月这段关系打星+建议（当月可改）。 */
    public MonthReviewPairVO saveMonthReview(String me, Integer stars, String advice) {
        CoupleSpace space = requireSpace(me);
        int s = stars == null ? 5 : Math.max(CoupleMonthReview.STARS_MIN, Math.min(CoupleMonthReview.STARS_MAX, stars));
        String a = trimLimit(advice, CoupleMonthReview.ADVICE_MAX, "建议写 " + CoupleMonthReview.ADVICE_MAX + " 字以内哦");
        String month = YearMonth.now().toString();
        CoupleMonthReview partnerRow = reviewMapper.find(space.getId(), month, space.partnerOf(me));
        CoupleMonthReview row = reviewMapper.find(space.getId(), month, me);
        if (row == null) {
            reviewMapper.insert(CoupleMonthReview.of(space.getId(), month, me, s, a));
            if (partnerRow != null) {
                push.pushCoupleEventBoth("month-review-both", me, space.getUserA(), space.getUserB(),
                        "🌙 本月互评双方都完成了，去对照一下你们各打了几星。");
            } else {
                push.pushCoupleEvent("month-review-done", me, space.partnerOf(me),
                        "🌙 TA 完成了本月互评，就差你的一颗星了。");
            }
        } else {
            row.setStars(s);
            row.setAdvice(a == null ? "" : a);
            row.setUpdatedAt(System.currentTimeMillis());
            reviewMapper.updateById(row);
        }
        return monthReviews(me);
    }

    // ========== F184 家庭应急卡 ==========

    public List<EmergencyCardVO> emergencyCards(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleEmergencyCard mine = cardMapper.find(space.getId(), me);
        CoupleEmergencyCard partner = cardMapper.find(space.getId(), space.partnerOf(me));
        List<EmergencyCardVO> list = new ArrayList<>();
        if (mine != null) {
            list.add(toCardVO(mine, me));
        }
        if (partner != null) {
            list.add(toCardVO(partner, me));
        }
        return list;
    }

    /** 填写/更新我的应急卡。 */
    public List<EmergencyCardVO> saveEmergencyCard(String me, String contacts, String keysPlace, String medicine) {
        CoupleSpace space = requireSpace(me);
        String c = trimLimit(contacts, CoupleEmergencyCard.CONTACTS_MAX, "联系人清单写 " + CoupleEmergencyCard.CONTACTS_MAX + " 字以内哦");
        String k = trimLimit(keysPlace, CoupleEmergencyCard.KEYS_MAX, "钥匙存放写 " + CoupleEmergencyCard.KEYS_MAX + " 字以内哦");
        String m = trimLimit(medicine, CoupleEmergencyCard.MEDICINE_MAX, "药品清单写 " + CoupleEmergencyCard.MEDICINE_MAX + " 字以内哦");
        if (c == null && k == null && m == null) {
            throw new BusinessException(400, "至少写一项：紧急联系人、钥匙存放或常备药 💊");
        }
        CoupleEmergencyCard row = cardMapper.find(space.getId(), me);
        if (row == null) {
            cardMapper.insert(CoupleEmergencyCard.of(space.getId(), me, c, k, m));
            push.pushCoupleEvent("emergency-card", me, space.partnerOf(me),
                    "💊 TA 填好了家庭应急卡，关键时刻这张卡就是定心丸。");
        } else {
            row.setContacts(c == null ? "" : c);
            row.setKeysPlace(k == null ? "" : k);
            row.setMedicine(m == null ? "" : m);
            row.setUpdatedAt(System.currentTimeMillis());
            cardMapper.updateById(row);
        }
        return emergencyCards(me);
    }

    // ========== F185 情侣存档点 ==========

    public List<SnapshotVO> snapshots(String me) {
        CoupleSpace space = requireSpace(me);
        return snapshotMapper.findBySpace(space.getId()).stream()
                .map(s -> new SnapshotVO(s.getId(), s.getMonth(), s.getFromUser(), s.getFromUser().equals(me), s.getWork(), s.getHealth(), s.getLoveTemp(), s.getUpdatedAt()))
                .toList();
    }

    /** 存这个月的档：工作、健康、感情温度。 */
    public List<SnapshotVO> saveSnapshot(String me, Integer loveTemp, String work, String health) {
        CoupleSpace space = requireSpace(me);
        int temp = loveTemp == null ? 60 : Math.max(CoupleMonthSnapshot.TEMP_MIN, Math.min(CoupleMonthSnapshot.TEMP_MAX, loveTemp));
        String w = trimLimit(work, CoupleMonthSnapshot.FIELD_MAX, "工作状态写 " + CoupleMonthSnapshot.FIELD_MAX + " 字以内哦");
        String h = trimLimit(health, CoupleMonthSnapshot.FIELD_MAX, "健康状态写 " + CoupleMonthSnapshot.FIELD_MAX + " 字以内哦");
        String month = YearMonth.now().toString();
        CoupleMonthSnapshot row = snapshotMapper.find(space.getId(), month, me);
        if (row == null) {
            snapshotMapper.insert(CoupleMonthSnapshot.of(space.getId(), month, me, w, h, temp));
            push.pushCoupleEvent("snapshot-saved", me, space.partnerOf(me),
                "💾 TA 存了本月的恋爱存档点，去看看你们各自的状态。");
        } else {
            row.setWork(w == null ? "" : w);
            row.setHealth(h == null ? "" : h);
            row.setLoveTemp(temp);
            row.setUpdatedAt(System.currentTimeMillis());
            snapshotMapper.updateById(row);
        }
        return snapshots(me);
    }

    // ========== F186 家务积分市场 ==========

    public PointAccountVO points(String me) {
        CoupleSpace space = requireSpace(me);
        List<CouplePointLedger> ledger = ledgerMapper.findBySpace(space.getId());
        int earned = 0;
        int spent = 0;
        for (CouplePointLedger row : ledger) {
            if (row.getFromUser().equals(me)) {
                if (CouplePointLedger.TYPE_EARN.equals(row.getType())) {
                    earned += row.getPoints();
                } else {
                    spent += row.getPoints();
                }
            }
        }
        int balance = earned - spent;
        List<RewardVO> rewards = CoupleManageBank.REWARDS.stream()
                .map(r -> new RewardVO(r.code(), r.name(), r.emoji(), r.points(), balance >= r.points()))
                .toList();
        List<PointLedgerVO> history = ledger.stream().limit(50)
                .map(l -> new PointLedgerVO(l.getId(), l.getFromUser(), l.getFromUser().equals(me), l.getType(), l.getItem(), l.getPoints(), l.getCreated()))
                .toList();
        return new PointAccountVO(balance, earned, rewards, history);
    }

    /** 做一件家务，赚积分。 */
    public PointAccountVO earnPoints(String me, String item, Integer points) {
        CoupleSpace space = requireSpace(me);
        String i = trimLimit(item, CouplePointLedger.ITEM_MAX, "家务事项写 " + CouplePointLedger.ITEM_MAX + " 字以内哦");
        if (i == null) {
            throw new BusinessException(400, "写下你刚做的那件家务，才领得到积分 🧹");
        }
        int p = points == null ? 5 : Math.max(CouplePointLedger.POINTS_MIN, Math.min(CouplePointLedger.POINTS_MAX, points));
        ledgerMapper.insert(CouplePointLedger.of(space.getId(), me, CouplePointLedger.TYPE_EARN, i, p));
        push.pushCoupleEvent("point-earn", me, space.partnerOf(me),
                "🧹 TA 刚干了「" + i + "」，+" + p + " 积分——家务不再是白干啦。");
        return points(me);
    }

    /** 用积分兑换一个小奖励。 */
    public PointAccountVO redeemReward(String me, String rewardCode) {
        CoupleSpace space = requireSpace(me);
        CoupleManageBank.Reward reward = CoupleManageBank.rewardOf(rewardCode);
        if (reward == null) {
            throw new BusinessException(400, "商店里没有这件奖励哦");
        }
        PointAccountVO account = points(me);
        if (account.balance() < reward.points()) {
            throw new BusinessException(400, "积分还不够，这个奖励要 " + reward.points() + " 分，你还差 " + (reward.points() - account.balance()) + " 分");
        }
        ledgerMapper.insert(CouplePointLedger.of(space.getId(), me, CouplePointLedger.TYPE_SPEND, reward.name(), reward.points()));
        push.pushCoupleEvent("point-redeem", me, space.partnerOf(me),
                "🎁 TA 兑换了「" + reward.name() + "」，记得兑现哦——这是用家务换来的浪漫。");
        return points(me);
    }

    // ========== F187 五年计划双轨 ==========

    public List<FiveYearVO> fiveYearPlans(String me) {
        CoupleSpace space = requireSpace(me);
        return planMapper.findBySpace(space.getId()).stream()
                .map(p -> new FiveYearVO(p.getId(), p.getTrack(), p.getFromUser(), p.getFromUser().equals(me),
                        p.getContent(), p.getOwnerUser(), Integer.valueOf(1).equals(p.getDone()), p.getCreated()))
                .toList();
    }

    /** 写一条五年之约：MINE 自己的 / OURS 我们的。 */
    public List<FiveYearVO> addFiveYearPlan(String me, String track, String content) {
        CoupleSpace space = requireSpace(me);
        String tr = CoupleFiveYearPlan.TRACK_OURS.equalsIgnoreCase(track == null ? "" : track.trim())
                ? CoupleFiveYearPlan.TRACK_OURS : CoupleFiveYearPlan.TRACK_MINE;
        String c = trimLimit(content, CoupleFiveYearPlan.CONTENT_MAX, "约定写 " + CoupleFiveYearPlan.CONTENT_MAX + " 字以内哦");
        if (c == null) {
            throw new BusinessException(400, "写下那条想在五年后回看的约定吧 🗺️");
        }
        planMapper.insert(CoupleFiveYearPlan.of(space.getId(), tr, me, c));
        if (CoupleFiveYearPlan.TRACK_OURS.equals(tr)) {
            push.pushCoupleEvent("five-year-ours", me, space.partnerOf(me),
                    "🗺️ TA 写下了一个「我们的」五年之约，等你来认领你那一半。");
        }
        return fiveYearPlans(me);
    }

    /** 认领 OURS 轨道的约定：一人认领一半的承诺。 */
    public List<FiveYearVO> claimPlan(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleFiveYearPlan row = requirePlan(space, id);
        if (!CoupleFiveYearPlan.TRACK_OURS.equals(row.getTrack())) {
            throw new BusinessException(400, "「自己的」那条路不用认领，自己走就好");
        }
        if (row.getOwnerUser() != null && !row.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "这条已经被「" + row.getOwnerUser() + "」认领了");
        }
        row.setOwnerUser(me);
        row.setUpdatedAt(System.currentTimeMillis());
        planMapper.updateById(row);
        return fiveYearPlans(me);
    }

    /** 标记达成。 */
    public List<FiveYearVO> finishPlan(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleFiveYearPlan row = requirePlan(space, id);
        if (Integer.valueOf(1).equals(row.getDone())) {
            return fiveYearPlans(me);
        }
        row.setDone(1);
        row.setUpdatedAt(System.currentTimeMillis());
        planMapper.updateById(row);
        push.pushCoupleEventBoth("five-year-done", me, space.getUserA(), space.getUserB(),
                "🏆 五年之约达成一条：「" + row.getContent() + "」。");
        return fiveYearPlans(me);
    }

    // ========== F188 纪念日策划案 ==========

    public List<AnnivPlanVO> annivPlans(String me) {
        CoupleSpace space = requireSpace(me);
        return annivMapper.findBySpace(space.getId()).stream()
                .map(p -> new AnnivPlanVO(p.getId(), p.getDay(), p.getTitle(), p.getPlanner(), p.getPlanner().equals(me), p.getIdea(), p.getStatus(), p.getUpdatedAt()))
                .toList();
    }

    /** 给未来的纪念日立一份策划案（日期须在 400 天内）。 */
    public List<AnnivPlanVO> addAnnivPlan(String me, String day, String title, String idea) {
        CoupleSpace space = requireSpace(me);
        LocalDate target = parseDay(day, "纪念日日期要是 2026-05-20 这样的日期哦");
        if (target.isBefore(LocalDate.now()) || target.isAfter(LocalDate.now().plusDays(400))) {
            throw new BusinessException(400, "只能策划未来 400 天内的纪念日哦");
        }
        String t = trimLimit(title, CoupleAnnivPlan.TITLE_MAX, "纪念日名称写 " + CoupleAnnivPlan.TITLE_MAX + " 字以内哦");
        if (t == null) {
            throw new BusinessException(400, "先给这个纪念日起个名字 🎯");
        }
        String i = trimLimit(idea, CoupleAnnivPlan.IDEA_MAX, "点子写 " + CoupleAnnivPlan.IDEA_MAX + " 字以内哦");
        annivMapper.insert(CoupleAnnivPlan.of(space.getId(), target.toString(), t, me, i));
        push.pushCoupleEvent("anniv-planned", me, space.partnerOf(me),
                "🎯 TA 立了一份纪念日策划案：「" + t + "」，保密工作开始。");
        return annivPlans(me);
    }

    /** 推进状态：IDEA → LOCKED → DONE，只进不退。 */
    public List<AnnivPlanVO> advanceAnnivPlan(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleAnnivPlan row = requireAnniv(space, id);
        String next = switch (row.getStatus()) {
            case CoupleAnnivPlan.STATUS_IDEA -> CoupleAnnivPlan.STATUS_LOCKED;
            case CoupleAnnivPlan.STATUS_LOCKED -> CoupleAnnivPlan.STATUS_DONE;
            default -> CoupleAnnivPlan.STATUS_DONE;
        };
        if (next.equals(row.getStatus())) {
            return annivPlans(me);
        }
        row.setStatus(next);
        row.setUpdatedAt(System.currentTimeMillis());
        annivMapper.updateById(row);
        if (CoupleAnnivPlan.STATUS_DONE.equals(next)) {
            push.pushCoupleEventBoth("anniv-done", me, space.getUserA(), space.getUserB(),
                    "🎉 纪念日策划案「" + row.getTitle() + "」已落地，这个日子有了新回忆。");
        }
        return annivPlans(me);
    }

    // ========== F189 经营周报（聚合，无新表） ==========

    public ManageWeeklyVO weekly(String me) {
        CoupleSpace space = requireSpace(me);
        String monday = currentWeek();

        List<CoupleFamilyMeeting> weekMeetings = meetingMapper.findBySpace(space.getId()).stream()
                .filter(m -> m.getWeek().compareTo(monday) >= 0).toList();
        long closed = weekMeetings.stream().filter(m -> Integer.valueOf(1).equals(m.getClosed())).count();

        List<CouplePointLedger> weekLedger = ledgerMapper.findBySpace(space.getId()).stream()
                .filter(l -> LocalDate.ofInstant(java.time.Instant.ofEpochMilli(l.getCreated()), java.time.ZoneId.systemDefault()).toString().compareTo(monday) >= 0)
                .toList();
        int earned = weekLedger.stream().filter(l -> CouplePointLedger.TYPE_EARN.equals(l.getType())).mapToInt(CouplePointLedger::getPoints).sum();
        int spent = weekLedger.stream().filter(l -> CouplePointLedger.TYPE_SPEND.equals(l.getType())).mapToInt(CouplePointLedger::getPoints).sum();

        WeekHostVO host = host(me);

        StringBuilder summary = new StringBuilder("本周经营：");
        List<String> parts = new ArrayList<>();
        if (!weekMeetings.isEmpty()) {
            parts.add("议了 " + weekMeetings.size() + " 个议题、关掉 " + closed + " 个");
        }
        if (earned > 0) {
            parts.add("赚积分 " + earned + "、花积分 " + spent);
        }
        if (host.plan() != null && !host.plan().isBlank()) {
            parts.add("主理人排了本周小计划");
        }
        if (parts.isEmpty()) {
            summary.append("还没开张——先立一个议题、干一件家务，家就活了 🏠");
        } else {
            summary.append(String.join("，", parts)).append("。日子就是这样一周一周经营好的。");
        }
        return new ManageWeeklyVO(weekMeetings.size(), (int) closed, earned, spent, host.host(), summary.toString());
    }

    // ========== 内部工具 ==========

    static String currentWeek() {
        return LocalDate.now().with(DayOfWeek.MONDAY).toString();
    }

    private MeetingVO toMeetingVO(CoupleFamilyMeeting m, String me) {
        return new MeetingVO(m.getId(), m.getWeek(), m.getTopic(), m.getDecision(), m.getFollowDay(),
                m.getRaisedBy(), m.getRaisedBy().equals(me), Integer.valueOf(1).equals(m.getClosed()), m.getCreated());
    }

    private MonthReviewVO toReviewVO(CoupleMonthReview r, String me) {
        return new MonthReviewVO(r.getId(), r.getMonth(), r.getFromUser(), r.getFromUser().equals(me), r.getStars(), r.getAdvice(), r.getUpdatedAt());
    }

    private EmergencyCardVO toCardVO(CoupleEmergencyCard c, String me) {
        return new EmergencyCardVO(c.getFromUser(), c.getFromUser().equals(me), c.getContacts(), c.getKeysPlace(), c.getMedicine(), c.getUpdatedAt());
    }

    private CoupleFamilyMeeting requireMeeting(CoupleSpace space, String id) {
        CoupleFamilyMeeting row = meetingMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "找不到这个议题");
        }
        return row;
    }

    private CoupleSkillSwap requireSkill(CoupleSpace space, String id) {
        CoupleSkillSwap row = skillMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "找不到这笔技能交换");
        }
        return row;
    }

    private CoupleFiveYearPlan requirePlan(CoupleSpace space, String id) {
        CoupleFiveYearPlan row = planMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "找不到这条五年之约");
        }
        return row;
    }

    private CoupleAnnivPlan requireAnniv(CoupleSpace space, String id) {
        CoupleAnnivPlan row = annivMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "找不到这份策划案");
        }
        return row;
    }

    private LocalDate parseDay(String day, String message) {
        try {
            return LocalDate.parse(day.trim());
        } catch (Exception e) {
            throw new BusinessException(400, message);
        }
    }

    private String normalizeDay(String day, String message) {
        if (day == null || day.isBlank()) {
            return null;
        }
        return parseDay(day, message).toString();
    }

    private String trimLimit(String text, int max, String message) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        if (t.length() > max) {
            throw new BusinessException(400, message);
        }
        return t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
