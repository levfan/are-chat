package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 明日邮局（F290-F299，批次二十五）：五年后新年卡、人生大事进度、总得有一天拍卖、
 * 想象中的家、退休计划双写、许愿井周问、时光胶囊接龙、解梦局、周年愿望台账、未来信用卡。
 * 情绪价值设计：把「以后我们会怎样」从不敢想变成可以写、可以排期、可以盖章——
 * 给未来寄信的人，都是在给现在上保险。
 */
@Service
public class CouplePostService {

    static final int OATH_MAX = 500;
    static final int BUCKET_NAME_MAX = 40;
    static final int STEP_MAX = 80;
    static final int STEP_LIMIT = 12;
    static final int SOMEDAY_MAX = 80;
    static final int SOMEDAY_SHELF_MAX = 5;
    static final int FIELD_MAX = 100;
    static final int RETIRE_MAX = 200;
    static final int WELL_ANSWER_MAX = 140;
    static final int RELAY_MAX = 500;
    static final int RELAY_INFLIGHT_MAX = 3;
    static final int DREAM_MAX = 300;
    static final int WISH_MAX = 300;
    static final int PROMISE_MAX = 80;
    static final List<String> RETIRE_BANDS = List.of("30", "40", "50");

    private final CoupleSpaceMapper spaceMapper;
    private final CouplePostOathMapper oathMapper;
    private final CoupleBucketMapper bucketMapper;
    private final CoupleBucketStepMapper stepMapper;
    private final CoupleSomedayMapper somedayMapper;
    private final CoupleDreamHomeMapper homeMapper;
    private final CoupleRetirePlanMapper retireMapper;
    private final CoupleWellQaMapper wellMapper;
    private final CoupleRelayCapsuleMapper relayMapper;
    private final CoupleDreamCaseMapper dreamMapper;
    private final CoupleAnnivWishMapper wishMapper;
    private final CoupleFutureCreditMapper creditMapper;
    private final ImPushService push;

    public CouplePostService(CoupleSpaceMapper spaceMapper, CouplePostOathMapper oathMapper,
                             CoupleBucketMapper bucketMapper, CoupleBucketStepMapper stepMapper,
                             CoupleSomedayMapper somedayMapper, CoupleDreamHomeMapper homeMapper,
                             CoupleRetirePlanMapper retireMapper, CoupleWellQaMapper wellMapper,
                             CoupleRelayCapsuleMapper relayMapper, CoupleDreamCaseMapper dreamMapper,
                             CoupleAnnivWishMapper wishMapper, CoupleFutureCreditMapper creditMapper,
                             ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.oathMapper = oathMapper;
        this.bucketMapper = bucketMapper;
        this.stepMapper = stepMapper;
        this.somedayMapper = somedayMapper;
        this.homeMapper = homeMapper;
        this.retireMapper = retireMapper;
        this.wellMapper = wellMapper;
        this.relayMapper = relayMapper;
        this.dreamMapper = dreamMapper;
        this.wishMapper = wishMapper;
        this.creditMapper = creditMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record OathVO(String id, String year, String content, String deliverDay, boolean mine,
                         boolean sent, String partnerContent) {
    }

    public record StepVO(int seq, String text, boolean done, String doneBy) {
    }

    public record BucketVO(String id, String name, String targetDay, String note, boolean mine,
                           String status, int doneSteps, int totalSteps) {
    }

    public record SomedayVO(String id, String thing, boolean mine, String status, String takenBy,
                            String scheduledDay, long daysLeft) {
    }

    public record HomeVO(String year, String rooms, String windowView, String smell, String corner,
                         boolean mine, boolean partnerIn) {
    }

    public record RetireVO(String ageBand, String mine, String partner, boolean bothIn) {
    }

    public record WellVO(String week, String question, String myAnswer, String partnerAnswer, boolean bothIn) {
    }

    public record RelayVO(String id, boolean mine, String openDay, String status, boolean due) {
    }

    public record DreamVO(String id, String day, String dream, boolean mine, String reading,
                          String readBy, Integer good) {
    }

    public record WishVO(String id, String year, String wish, boolean mine, String verdict) {
    }

    public record CreditVO(String id, String promise, String dueDay, boolean mine, String status, long daysLeft) {
    }

    public record CreditLineVO(String tier, int kept, int broken, int open) {
    }

    public record PostVO(String day, String week, String year,
                         List<OathVO> oaths, List<BucketVO> buckets, List<SomedayVO> somedays,
                         List<HomeVO> homes, List<RetireVO> retires, WellVO well, List<WellVO> wellYear,
                         List<RelayVO> relays, List<DreamVO> dreams, List<WishVO> wishes,
                         List<CreditVO> credits, CreditLineVO creditLine) {
    }

    // ========== 读：明日邮局总览（含惰性结算：新年卡放行 / 拍卖逾期下架 / 承诺逾期降额） ==========

    /** 明日邮局总览。 */
    public PostVO post(String me) {
        CoupleSpace space = requireSpace(me);
        settle(space, LocalDate.now());
        return buildPost(space, me, LocalDate.now());
    }

    // ========== F290 五年后新年卡 ==========

    /** 写今年的新年卡（一年一张，未放行前可改）。 */
    public PostVO oath(String me, String content) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String year = String.valueOf(now.getYear());
        String c = trim(content, "给五年后的话总要写一句");
        if (c.length() > OATH_MAX) {
            throw new BusinessException(400, "最多 500 字，未来的信别挤成春运");
        }
        CouplePostOath exist = null;
        for (CouplePostOath o : oathMapper.findBySpace(space.getId())) {
            if (o.getYear().equals(year) && o.getFromUser().equals(me)) {
                exist = o;
            }
        }
        if (exist != null) {
            if ("SENT".equals(exist.getStatus())) {
                throw new BusinessException(400, "这张已经寄出去了，收不进笔了");
            }
            exist.setContent(c);
            oathMapper.updateById(exist);
        } else {
            oathMapper.insert(CouplePostOath.of(space.getId(), year, me, c,
                    now.plusYears(5).withDayOfYear(1).toString()));
        }
        return post(me);
    }

    // ========== F291 人生大事进度 ==========

    /** 立一件大事。 */
    public PostVO bucketAdd(String me, String name, String targetDay, String note) {
        CoupleSpace space = requireSpace(me);
        String n = trim(name, "大事叫什么要写");
        if (n.length() > BUCKET_NAME_MAX) {
            throw new BusinessException(400, "大事名最多 40 字");
        }
        if (bucketMapper.findName(space.getId(), n) != null) {
            throw new BusinessException(400, "这件大事已经在册了");
        }
        String td = targetDay == null || targetDay.isBlank() ? "" : validDay(targetDay.trim(), "目标日");
        String t = note == null ? "" : note.trim();
        if (t.length() > 200) {
            throw new BusinessException(400, "备注最多 200 字");
        }
        bucketMapper.insert(CoupleBucket.of(space.getId(), n, td, t, me));
        push.pushCoupleEvent("post-bucket", me, space.partnerOf(me), "人生清单上新了一件：「" + n + "」🗳️");
        return post(me);
    }

    /** 给大事拆一步。 */
    public PostVO bucketStepAdd(String me, String bucketId, String text) {
        CoupleSpace space = requireSpace(me);
        CoupleBucket bucket = requireBucket(space, bucketId);
        String t = trim(text, "步骤要写一句");
        if (t.length() > STEP_MAX) {
            throw new BusinessException(400, "步骤最多 80 字");
        }
        List<CoupleBucketStep> steps = stepMapper.findByBucket(bucket.getId());
        if (steps.size() >= STEP_LIMIT) {
            throw new BusinessException(400, "一件大事最多拆 " + STEP_LIMIT + " 步，先走完再说");
        }
        stepMapper.insert(CoupleBucketStep.of(space.getId(), bucket.getId(), steps.size() + 1, t));
        return post(me);
    }

    /** 完成一步（双方都可点：本人完成 / TA 补进展章）。 */
    public PostVO bucketStepDone(String me, String stepId) {
        CoupleSpace space = requireSpace(me);
        CoupleBucketStep step = stepId == null ? null : stepMapper.selectById(stepId);
        if (step == null || !step.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这一步不存在");
        }
        if (step.isDone()) {
            return post(me);
        }
        step.setDone(1);
        step.setDoneBy(me);
        step.setDoneAt(System.currentTimeMillis());
        stepMapper.updateById(step);
        CoupleBucket bucket = bucketMapper.selectById(step.getBucketId());
        List<CoupleBucketStep> siblings = stepMapper.findByBucket(step.getBucketId());
        boolean allDone = bucket != null && siblings.stream().allMatch(CoupleBucketStep::isDone)
                && !siblings.isEmpty();
        if (allDone) {
            bucket.setStatus(CoupleBucket.STATUS_DONE);
            bucket.setUpdatedAt(System.currentTimeMillis());
            bucketMapper.updateById(bucket);
            push.pushCoupleEventBoth("post-bucket-done", me, space.getUserA(), space.getUserB(),
                    "「" + bucket.getName() + "」全部步骤走完，人生大事+1 🎊");
        } else {
            push.pushCoupleEvent("post-step-done", me, space.partnerOf(me), "大事「"
                    + (bucket == null ? "" : bucket.getName()) + "」又走完一步 🪜");
        }
        return post(me);
    }

    /** 放弃大事（发起人可弃，留档 GONE）。 */
    public PostVO bucketAbandon(String me, String bucketId) {
        CoupleSpace space = requireSpace(me);
        CoupleBucket bucket = requireBucket(space, bucketId);
        if (!bucket.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "谁立的大事谁才有资格鸽");
        }
        if (!CoupleBucket.STATUS_OPEN.equals(bucket.getStatus())) {
            return post(me);
        }
        bucket.setStatus(CoupleBucket.STATUS_GONE);
        bucket.setUpdatedAt(System.currentTimeMillis());
        bucketMapper.updateById(bucket);
        push.pushCoupleEvent("post-bucket-gone", me, space.partnerOf(me), "「" + bucket.getName() + "」先放下了，不勉强 🍃");
        return post(me);
    }

    // ========== F292 总得有一天拍卖 ==========

    /** 把「改天一定」上拍（在架≤5，7 天无人认领自动下架）。 */
    public PostVO somedayShelf(String me, String thing) {
        CoupleSpace space = requireSpace(me);
        String t = trim(thing, "改天做的事总要写一件");
        if (t.length() > SOMEDAY_MAX) {
            throw new BusinessException(400, "最多 80 字");
        }
        List<CoupleSomeday> active = activeSomedays(space.getId());
        boolean mineActive = active.stream().anyMatch(s -> s.getOwnerUser().equals(me));
        if (mineActive && active.size() >= SOMEDAY_SHELF_MAX * 2) {
            throw new BusinessException(400, "货架快满了，先清几件再上");
        }
        somedayMapper.insert(CoupleSomeday.of(space.getId(), t, me));
        push.pushCoupleEvent("post-shelf", me, space.partnerOf(me), "TA 把「" + t + "」挂上了架，7 天内认领 🛒");
        return post(me);
    }

    /** 认领并排期（不能接自己的架）。 */
    public PostVO somedayTake(String me, String id, String scheduledDay) {
        CoupleSpace space = requireSpace(me);
        CoupleSomeday row = requireSomeday(space, id);
        if (row.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "自己上的架不能自己接，去催 TA");
        }
        if (!"SHELF".equals(row.getStatus())) {
            throw new BusinessException(400, "这单已经有人抢了");
        }
        String d = validDay(scheduledDay, "排期日");
        if (d.compareTo(LocalDate.now().toString()) < 0) {
            throw new BusinessException(400, "排期日得在今天之后");
        }
        row.setStatus("TAKEN");
        row.setTakenBy(me);
        row.setScheduledDay(d);
        row.setUpdatedAt(System.currentTimeMillis());
        somedayMapper.updateById(row);
        push.pushCoupleEventBoth("post-taken", me, space.getUserA(), space.getUserB(),
                "「" + row.getThing() + "」排到 " + d + "，改天变成了某天 📅");
        return post(me);
    }

    /** 完成销单（任一方都可点）。 */
    public PostVO somedayDone(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSomeday row = requireSomeday(space, id);
        if (!"TAKEN".equals(row.getStatus())) {
            throw new BusinessException(400, "还没认领的单不能直接完成");
        }
        row.setStatus("DONE");
        row.setDoneAt(System.currentTimeMillis());
        row.setUpdatedAt(row.getDoneAt());
        somedayMapper.updateById(row);
        push.pushCoupleEventBoth("post-someday-done", me, space.getUserA(), space.getUserB(),
                "「" + row.getThing() + "」今天真的做了，货架清出一格 ✨");
        return post(me);
    }

    // ========== F293 想象中的家 ==========

    /** 填今年的梦想家（本人版，可改写）。 */
    public PostVO dreamHome(String me, String year, String rooms, String windowView, String smell, String corner) {
        CoupleSpace space = requireSpace(me);
        String y = year == null || year.isBlank() ? String.valueOf(LocalDate.now().getYear()) : year.trim();
        if (!y.matches("\\d{4}")) {
            throw new BusinessException(400, "版本年格式应为 yyyy");
        }
        CoupleDreamHome row = homeMapper.find(space.getId(), y, me);
        boolean isNew = row == null;
        if (isNew) {
            row = CoupleDreamHome.of(space.getId(), y, me);
        }
        row.setRooms(capped(rooms, FIELD_MAX, "房间"));
        row.setWindowView(capped(windowView, FIELD_MAX, "窗外"));
        row.setSmell(capped(smell, FIELD_MAX, "味道"));
        row.setCorner(capped(corner, FIELD_MAX, "角落"));
        row.setUpdatedAt(System.currentTimeMillis());
        if (isNew) {
            homeMapper.insert(row);
        } else {
            homeMapper.updateById(row);
        }
        if (isNew) {
            push.pushCoupleEvent("post-home", me, space.partnerOf(me), "TA 交了 " + y + " 版「想象中的家」，去对照 🏡");
        }
        return post(me);
    }

    // ========== F294 退休计划双写 ==========

    /** 写某一岁的「那时候我们在干嘛」。 */
    public PostVO retire(String me, String ageBand, String text) {
        CoupleSpace space = requireSpace(me);
        if (ageBand == null || !RETIRE_BANDS.contains(ageBand.trim())) {
            throw new BusinessException(400, "档位只有 30 / 40 / 50");
        }
        String t = trim(text, "总得写一句");
        if (t.length() > RETIRE_MAX) {
            throw new BusinessException(400, "最多 200 字");
        }
        String band = ageBand.trim();
        CoupleRetirePlan exist = retireMapper.find(space.getId(), band, me);
        if (exist != null) {
            exist.setText(t);
            exist.setUpdatedAt(System.currentTimeMillis());
            retireMapper.updateById(exist);
            return post(me);
        }
        retireMapper.insert(CoupleRetirePlan.of(space.getId(), band, me, t));
        String partner = space.partnerOf(me);
        if (retireMapper.find(space.getId(), band, partner) != null) {
            push.pushCoupleEventBoth("post-retire-both", me, space.getUserA(), space.getUserB(),
                    band + " 岁的计划双写完成，去对照分歧点 🛫");
        } else {
            push.pushCoupleEvent("post-retire", me, partner, "TA 写好了 " + band + " 岁的我们，等你那一份");
        }
        return post(me);
    }

    // ========== F295 许愿井周问 ==========

    /** 答本周井题（可改写）。 */
    public PostVO wellAnswer(String me, String answer) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String week = now.with(DayOfWeek.MONDAY).toString();
        String a = trim(answer, "答案总要写一点");
        if (a.length() > WELL_ANSWER_MAX) {
            throw new BusinessException(400, "答案最多 140 字");
        }
        String q = CouplePostBank.wellQuestion(now.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()));
        CoupleWellQa exist = wellMapper.find(space.getId(), week, me);
        if (exist != null) {
            exist.setAnswer(a);
            exist.setUpdatedAt(System.currentTimeMillis());
            wellMapper.updateById(exist);
            return post(me);
        }
        wellMapper.insert(CoupleWellQa.of(space.getId(), week, me, q, a));
        String partner = space.partnerOf(me);
        if (wellMapper.find(space.getId(), week, partner) != null) {
            push.pushCoupleEventBoth("post-well-both", me, space.getUserA(), space.getUserB(),
                    "本周井题双答完成，投井下钱 💫");
        } else {
            push.pushCoupleEvent("post-well", me, partner, "TA 答了本周井题，去答你的那份 🪣");
        }
        return post(me);
    }

    // ========== F296 时光胶囊接龙 ==========

    /** 给 TA 写一笔未来的信（1/2/3 年后到点，在途≤3）。 */
    public PostVO relaySeal(String me, String content, Integer years) {
        CoupleSpace space = requireSpace(me);
        String c = trim(content, "给未来的话要写");
        if (c.length() > RELAY_MAX) {
            throw new BusinessException(400, "最多 500 字");
        }
        int y = years == null ? 1 : years;
        if (y < 1 || y > 3) {
            throw new BusinessException(400, "只能寄往 1 / 2 / 3 年后");
        }
        LocalDate now = LocalDate.now();
        long mineInflight = relayMapper.findBySpace(space.getId()).stream()
                .filter(r -> "SEALED".equals(r.getStatus()) && r.getFromUser().equals(me)).count();
        if (mineInflight >= RELAY_INFLIGHT_MAX) {
            throw new BusinessException(400, "你名下在途 " + RELAY_INFLIGHT_MAX + " 笔已经封顶，等 TA 拆了再写");
        }
        relayMapper.insert(CoupleRelayCapsule.of(space.getId(), me, c, now.plusYears(y).toString()));
        push.pushCoupleEvent("post-relay-seal", me, space.partnerOf(me), CouplePostBank.RELAY_SEAL_LINE);
        return post(me);
    }

    /** 拆到期的、TA 写给我的那笔（按开启日最早一笔）。 */
    public PostVO relayOpen(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleRelayCapsule row = requireRelay(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "你写的那笔不归你拆，等 TA 先");
        }
        if (!"SEALED".equals(row.getStatus())) {
            return post(me);
        }
        if (row.getOpenDay().compareTo(LocalDate.now().toString()) > 0) {
            throw new BusinessException(400, "还没到开启日（" + row.getOpenDay() + "）");
        }
        row.setStatus("OPENED");
        row.setOpenedAt(System.currentTimeMillis());
        relayMapper.updateById(row);
        push.pushCoupleEvent("post-relay-opened", me, row.getFromUser(), "你留的那笔未来信被拆开了 ✉️");
        return post(me);
    }

    // ========== F297 解梦局 ==========

    /** 记一笔梦投稿解梦局。 */
    public PostVO dreamAdd(String me, String dream) {
        CoupleSpace space = requireSpace(me);
        String d = trim(dream, "梦到什么要写");
        if (d.length() > DREAM_MAX) {
            throw new BusinessException(400, "梦境最多 300 字");
        }
        String day = LocalDate.now().toString();
        CoupleDreamCase exist = dreamMapper.find(space.getId(), day, me);
        if (exist != null) {
            throw new BusinessException(400, "今天的案子已经投过了，明晚的梦明早再排");
        }
        dreamMapper.insert(CoupleDreamCase.of(space.getId(), day, me, d));
        push.pushCoupleEvent("post-dream", me, space.partnerOf(me), "解梦局来了新案子，请官方一本正经解读 🔮");
        return post(me);
    }

    /** 解梦官出点评（不能审自己的案）。 */
    public PostVO dreamRead(String me, String id, String reading) {
        CoupleSpace space = requireSpace(me);
        CoupleDreamCase row = requireDream(space, id);
        if (row.getDreamerUser().equals(me)) {
            throw new BusinessException(400, "自己解读自己，那叫日记");
        }
        if (!row.getReadBy().isEmpty()) {
            throw new BusinessException(400, "这案已结，解梦官换人也没用");
        }
        String r = trim(reading, "点评总要写一句");
        if (r.length() > DREAM_MAX) {
            throw new BusinessException(400, "点评最多 300 字");
        }
        row.setReading(r);
        row.setReadBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        dreamMapper.updateById(row);
        push.pushCoupleEvent("post-dream-read", me, row.getDreamerUser(), "解梦局出结果了，去判「灵不灵」🔮");
        return post(me);
    }

    /** 做梦人盖章：解得灵 / 胡说八道。 */
    public PostVO dreamJudge(String me, String id, Boolean good) {
        CoupleSpace space = requireSpace(me);
        CoupleDreamCase row = requireDream(space, id);
        if (!row.getDreamerUser().equals(me)) {
            throw new BusinessException(400, "案子是我的，章也只能我盖");
        }
        if (row.getReadBy().isEmpty()) {
            throw new BusinessException(400, "还没有解梦点评，先等等");
        }
        if (row.getGood() != null || good == null) {
            return post(me);
        }
        row.setGood(good ? 1 : 0);
        row.setUpdatedAt(System.currentTimeMillis());
        dreamMapper.updateById(row);
        push.pushCoupleEvent("post-dream-judge", me, row.getReadBy(),
                good ? "解梦官盖章：解得灵！🏅" : "解梦官被驳回：胡说八道 😝 重审等下一晚");
        return post(me);
    }

    // ========== F298 周年愿望台账 ==========

    /** 立/改今年的周年愿望。 */
    public PostVO wish(String me, String year, String wish) {
        CoupleSpace space = requireSpace(me);
        String y = year == null || year.isBlank() ? String.valueOf(LocalDate.now().getYear()) : year.trim();
        if (!y.matches("\\d{4}")) {
            throw new BusinessException(400, "年份格式应为 yyyy");
        }
        String w = trim(wish, "愿望要写");
        if (w.length() > WISH_MAX) {
            throw new BusinessException(400, "最多 300 字");
        }
        CoupleAnnivWish exist = wishMapper.find(space.getId(), y, me);
        if (exist != null) {
            if (!exist.getVerdict().isEmpty()) {
                throw new BusinessException(400, "这一年已经盖过章，尘埃落定");
            }
            exist.setWish(w);
            wishMapper.updateById(exist);
        } else {
            wishMapper.insert(CoupleAnnivWish.of(space.getId(), y, me, w));
            push.pushCoupleEvent("post-wish", me, space.partnerOf(me), "TA 立了 " + y + " 年的周年愿望，也可以偷偷看 🎋");
        }
        return post(me);
    }

    /** 给往年愿望盖章：圆上了 / 鸽了。 */
    public PostVO wishVerdict(String me, String id, Boolean kept) {
        CoupleSpace space = requireSpace(me);
        CoupleAnnivWish row = requireWish(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "TA 的愿望让 TA 自己盖章");
        }
        if (!row.getVerdict().isEmpty() || kept == null) {
            return post(me);
        }
        if (row.getYear().compareTo(String.valueOf(LocalDate.now().getYear())) >= 0) {
            throw new BusinessException(400, "今年的愿望还没到期，明年再盖");
        }
        row.setVerdict(kept ? "KEPT" : "PIGEON");
        row.setVerdictAt(System.currentTimeMillis());
        wishMapper.updateById(row);
        push.pushCoupleEvent("post-wish-verdict", me, space.partnerOf(me),
                kept ? row.getYear() + " 年的愿望圆上了 ⭕" : row.getYear() + " 年的愿望鸽了 🕊️ 但说出来也被记住了");
        return post(me);
    }

    // ========== F299 未来信用卡 ==========

    /** 立一张未来承诺（期限必须放未来）。 */
    public PostVO creditPromise(String me, String promise, String dueDay) {
        CoupleSpace space = requireSpace(me);
        String p = trim(promise, "承诺总要写一句");
        if (p.length() > PROMISE_MAX) {
            throw new BusinessException(400, "承诺最多 80 字");
        }
        String d = validDay(dueDay, "兑现期限");
        if (d.compareTo(LocalDate.now().toString()) <= 0) {
            throw new BusinessException(400, "期限要放在未来，不然叫什么「未来」卡");
        }
        creditMapper.insert(CoupleFutureCredit.of(space.getId(), p, d, me));
        push.pushCoupleEvent("post-credit", me, space.partnerOf(me), "TA 立了旗：「" + p + "」，" + d + " 前兑现 🏳️");
        return post(me);
    }

    /** 兑现销旗（本人点）。 */
    public PostVO creditKeep(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleFutureCredit row = requireCredit(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "旗是谁立的谁销，TA 的旗等 TA 圆");
        }
        if (!"OPEN".equals(row.getStatus())) {
            return post(me);
        }
        row.setStatus("KEPT");
        row.setKeptAt(System.currentTimeMillis());
        row.setUpdatedAt(row.getKeptAt());
        creditMapper.updateById(row);
        push.pushCoupleEventBoth("post-credit-kept", me, space.getUserA(), space.getUserB(),
                "「" + row.getPromise() + "」圆上了，未来信用卡提额 📈");
        return post(me);
    }

    // ========== 内部 ==========

    /** 读时惰性结算：新年卡到期放行、拍卖逾期下架、承诺逾期降额。 */
    private void settle(CoupleSpace space, LocalDate now) {
        String today = now.toString();
        for (CouplePostOath o : oathMapper.findDue(space.getId(), today)) {
            o.setStatus("SENT");
            o.setSentAt(System.currentTimeMillis());
            oathMapper.updateById(o);
            push.pushCoupleEventBoth("post-oath-opened", o.getFromUser(), space.getUserA(), space.getUserB(),
                    CouplePostBank.oathDeliverLine(o.getYear()));
        }
        long shelfCutoff = now.minusDays(CoupleSomeday.SHELF_DAYS)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        for (CoupleSomeday s : somedayMapper.findStaleShelf(space.getId(), shelfCutoff)) {
            s.setStatus("EXPIRED");
            s.setUpdatedAt(System.currentTimeMillis());
            somedayMapper.updateById(s);
            push.pushCoupleEvent("post-shelf-expired", s.getOwnerUser(), space.partnerOf(s.getOwnerUser()),
                    "「" + s.getThing() + "」7 天没人接，先落灰下架了 🕸️");
        }
        for (CoupleFutureCredit c : creditMapper.findOpen(space.getId())) {
            if (c.getDueDay().compareTo(today) < 0) {
                c.setStatus("BROKEN");
                c.setUpdatedAt(System.currentTimeMillis());
                creditMapper.updateById(c);
                push.pushCoupleEventBoth("post-credit-break", c.getFromUser(), space.getUserA(), space.getUserB(),
                        "「" + c.getPromise() + "」逾期没圆上，额度降一格 📉 下次小点声立旗");
            }
        }
    }

    private PostVO buildPost(CoupleSpace space, String me, LocalDate now) {
        String partner = space.partnerOf(me);
        String today = now.toString();
        String week = now.with(DayOfWeek.MONDAY).toString();
        String year = String.valueOf(now.getYear());

        List<OathVO> oaths = new ArrayList<>();
        String partnerOath = "";
        for (CouplePostOath o : oathMapper.findBySpace(space.getId())) {
            if (o.getYear().equals(year) && o.getFromUser().equals(partner)) {
                partnerOath = "SENT".equals(o.getStatus()) ? o.getContent() : "";
            }
        }
        for (CouplePostOath o : oathMapper.findBySpace(space.getId())) {
            if (o.getFromUser().equals(me)) {
                oaths.add(new OathVO(o.getId(), o.getYear(), o.getContent(), o.getDeliverDay(), true,
                        "SENT".equals(o.getStatus()), partnerOath));
            }
        }

        List<BucketVO> buckets = new ArrayList<>();
        for (CoupleBucket b : bucketMapper.findOpen(space.getId())) {
            List<CoupleBucketStep> steps = stepMapper.findByBucket(b.getId());
            int done = (int) steps.stream().filter(CoupleBucketStep::isDone).count();
            buckets.add(new BucketVO(b.getId(), b.getName(), b.getTargetDay(), b.getNote(),
                    b.getOwnerUser().equals(me), b.getStatus(), done, steps.size()));
        }

        List<SomedayVO> somedays = new ArrayList<>();
        for (CoupleSomeday s : activeSomedays(space.getId())) {
            long left = "TAKEN".equals(s.getStatus())
                    ? Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(now, LocalDate.parse(s.getScheduledDay())))
                    : Math.max(0, CoupleSomeday.SHELF_DAYS
                    - java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.Instant.ofEpochMilli(s.getCreated()).atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                    now));
            somedays.add(new SomedayVO(s.getId(), s.getThing(), s.getOwnerUser().equals(me),
                    s.getStatus(), s.getTakenBy(), s.getScheduledDay(), left));
        }

        List<HomeVO> homes = new ArrayList<>();
        for (CoupleDreamHome h : homeMapper.findBySpace(space.getId())) {
            if (h.getFromUser().equals(me)) {
                CoupleDreamHome p = homeMapper.find(space.getId(), h.getYear(), partner);
                homes.add(new HomeVO(h.getYear(), h.getRooms(), h.getWindowView(), h.getSmell(), h.getCorner(),
                        true, p != null));
            }
        }

        List<RetireVO> retires = new ArrayList<>();
        for (String band : RETIRE_BANDS) {
            CoupleRetirePlan mine = retireMapper.find(space.getId(), band, me);
            CoupleRetirePlan other = retireMapper.find(space.getId(), band, partner);
            if (mine != null || other != null) {
                retires.add(new RetireVO(band, mine == null ? "" : mine.getText(),
                        other == null ? "" : other.getText(), mine != null && other != null));
            }
        }

        CoupleWellQa mineW = wellMapper.find(space.getId(), week, me);
        CoupleWellQa otherW = wellMapper.find(space.getId(), week, partner);
        String q = mineW != null ? mineW.getQuestion() : otherW != null ? otherW.getQuestion()
                : CouplePostBank.wellQuestion(now.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()));
        WellVO well = new WellVO(week, q, mineW == null ? "" : mineW.getAnswer(),
                otherW == null ? "" : otherW.getAnswer(), mineW != null && otherW != null);
        List<WellVO> wellYear = new ArrayList<>();
        for (CoupleWellQa w : wellMapper.findYear(space.getId(), now.withDayOfYear(1).with(DayOfWeek.MONDAY).toString(),
                now.plusDays(7).with(DayOfWeek.MONDAY).toString())) {
            if (w.getFromUser().equals(me)) {
                CoupleWellQa p = wellMapper.find(space.getId(), w.getWeek(), partner);
                wellYear.add(new WellVO(w.getWeek(), w.getQuestion(), w.getAnswer(),
                        p == null ? "" : p.getAnswer(), p != null));
            }
        }

        List<RelayVO> relays = new ArrayList<>();
        for (CoupleRelayCapsule r : relayMapper.findBySpace(space.getId())) {
            relays.add(new RelayVO(r.getId(), r.getFromUser().equals(me), r.getOpenDay(), r.getStatus(),
                    "SEALED".equals(r.getStatus()) && !r.getFromUser().equals(me)
                            && r.getOpenDay().compareTo(today) <= 0));
        }

        List<DreamVO> dreams = new ArrayList<>();
        for (CoupleDreamCase d : dreamMapper.findBySpace(space.getId())) {
            if (dreams.size() < 15) {
                dreams.add(new DreamVO(d.getId(), d.getDay(), d.getDream(), d.getDreamerUser().equals(me),
                        d.getReading(), d.getReadBy(), d.getGood()));
            }
        }

        List<WishVO> wishes = new ArrayList<>();
        for (CoupleAnnivWish w : wishMapper.findBySpace(space.getId())) {
            wishes.add(new WishVO(w.getId(), w.getYear(), w.getWish(), w.getFromUser().equals(me), w.getVerdict()));
        }

        List<CreditVO> credits = new ArrayList<>();
        int kept = 0;
        int broken = 0;
        int open = 0;
        for (CoupleFutureCredit c : creditMapper.findBySpace(space.getId())) {
            if ("KEPT".equals(c.getStatus())) {
                kept++;
            } else if ("BROKEN".equals(c.getStatus())) {
                broken++;
            } else {
                open++;
            }
            if ("OPEN".equals(c.getStatus())) {
                credits.add(new CreditVO(c.getId(), c.getPromise(), c.getDueDay(), c.getFromUser().equals(me),
                        c.getStatus(), java.time.temporal.ChronoUnit.DAYS.between(now, LocalDate.parse(c.getDueDay()))));
            }
        }
        int mineKeptN = (int) creditMapper.findBySpace(space.getId()).stream()
                .filter(c -> c.getFromUser().equals(me) && "KEPT".equals(c.getStatus())).count();
        int mineBrokenN = (int) creditMapper.findBySpace(space.getId()).stream()
                .filter(c -> c.getFromUser().equals(me) && "BROKEN".equals(c.getStatus())).count();
        CreditLineVO line = new CreditLineVO(CouplePostBank.creditTier(mineKeptN - mineBrokenN),
                mineKeptN, mineBrokenN, open);

        return new PostVO(today, week, year, oaths, buckets, somedays, homes, retires, well, wellYear,
                relays, dreams, wishes, credits, line);
    }

    private List<CoupleSomeday> activeSomedays(String spaceId) {
        List<CoupleSomeday> out = new ArrayList<>();
        for (CoupleSomeday s : somedayMapper.findAll(spaceId)) {
            if ("SHELF".equals(s.getStatus()) || "TAKEN".equals(s.getStatus())) {
                out.add(s);
            }
        }
        return out;
    }

    private CoupleBucket requireBucket(CoupleSpace space, String id) {
        CoupleBucket row = id == null ? null : bucketMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这件大事不在册");
        }
        return row;
    }

    private CoupleSomeday requireSomeday(CoupleSpace space, String id) {
        CoupleSomeday row = id == null ? null : somedayMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这单已经不存在了");
        }
        return row;
    }

    private CoupleRelayCapsule requireRelay(CoupleSpace space, String id) {
        CoupleRelayCapsule row = id == null ? null : relayMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这笔未来信不存在");
        }
        return row;
    }

    private CoupleDreamCase requireDream(CoupleSpace space, String id) {
        CoupleDreamCase row = id == null ? null : dreamMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这个梦案不存在");
        }
        return row;
    }

    private CoupleAnnivWish requireWish(CoupleSpace space, String id) {
        CoupleAnnivWish row = id == null ? null : wishMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这条愿望不存在");
        }
        return row;
    }

    private CoupleFutureCredit requireCredit(CoupleSpace space, String id) {
        CoupleFutureCredit row = id == null ? null : creditMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这张旗不存在");
        }
        return row;
    }

    private String capped(String v, int max, String label) {
        String t = v == null ? "" : v.trim();
        if (t.length() > max) {
            throw new BusinessException(400, label + "最多 " + max + " 字");
        }
        return t;
    }

    private String validDay(String v, String label) {
        try {
            return LocalDate.parse(v).toString();
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, label + "格式应为 yyyy-MM-dd");
        }
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty() && !failMessage.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
