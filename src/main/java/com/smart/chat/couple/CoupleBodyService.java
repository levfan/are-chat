package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 身体通知系统（F310-F319，批次二十七）：体征互报、呼噜自报、周期共览、戒烟戒糖互助营、
 * 运动链、身体不适 SOS、忌口红线本、体检陪同、情绪药友、早睡军令状。
 * 情绪价值设计：身体的事最容易变成「你怎么不早说」，这套玩法把它变成有人按周记、有人递卡、
 * 有人陪着到场——只陪伴，不诊断（全库无任何医疗建议口径）。
 */
@Service
public class CoupleBodyService {

    static final int METRIC_FIELD_MAX = 10;
    static final int NOTE_MAX = 80;
    static final int DISCOMFORT_MAX = 60;
    static final int CARD_MAX = 100;
    static final int QUIT_NAME_MAX = 40;
    static final int CHEER_MAX = 140;
    static final int SYMPTOM_MAX = 80;
    static final int REDLINE_ITEM_MAX = 30;
    static final int CHECKUP_ITEM_MAX = 60;
    static final int REPORT_MAX = 140;
    static final int SHAKE_MAX = 60;
    static final int SOS_INFLIGHT_MAX = 1;
    static final int CAMP_DAYS_MIN = 7;
    static final int CAMP_DAYS_MAX = 100;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleBodyMetricMapper metricMapper;
    private final CoupleBodySnoreMapper snoreMapper;
    private final CoupleBodyCycleMapper cycleMapper;
    private final CoupleBodyQuitMapper quitMapper;
    private final CoupleBodyFitMapper fitMapper;
    private final CoupleBodySosMapper sosMapper;
    private final CoupleBodyRedlineMapper redlineMapper;
    private final CoupleBodyCheckupMapper checkupMapper;
    private final CoupleBodyMedMapper medMapper;
    private final CoupleBodyOathMapper oathMapper;
    private final CoupleCozyLightoutMapper lightoutMapper;
    private final CoupleDineTicketMapper dineMapper;
    private final ImPushService push;

    public CoupleBodyService(CoupleSpaceMapper spaceMapper, CoupleBodyMetricMapper metricMapper,
                             CoupleBodySnoreMapper snoreMapper, CoupleBodyCycleMapper cycleMapper,
                             CoupleBodyQuitMapper quitMapper, CoupleBodyFitMapper fitMapper,
                             CoupleBodySosMapper sosMapper, CoupleBodyRedlineMapper redlineMapper,
                             CoupleBodyCheckupMapper checkupMapper, CoupleBodyMedMapper medMapper,
                             CoupleBodyOathMapper oathMapper, CoupleCozyLightoutMapper lightoutMapper,
                             CoupleDineTicketMapper dineMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.metricMapper = metricMapper;
        this.snoreMapper = snoreMapper;
        this.cycleMapper = cycleMapper;
        this.quitMapper = quitMapper;
        this.fitMapper = fitMapper;
        this.sosMapper = sosMapper;
        this.redlineMapper = redlineMapper;
        this.checkupMapper = checkupMapper;
        this.medMapper = medMapper;
        this.oathMapper = oathMapper;
        this.lightoutMapper = lightoutMapper;
        this.dineMapper = dineMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record MetricVO(String day, boolean mine, String temp, String weight, String sleepHours,
                           String tempLimit, String sleepLimit, String note, boolean warn, String warnText) {
    }

    public record SnoreVO(String day, String myLevel, String partnerLevel, String myShake,
                          String partnerShake, boolean iCanShake, int partnerScore) {
    }

    public record CycleVO(String day, boolean mine, String phase, String phaseLabel, String discomfort,
                          String careCard, String careBy, boolean iCanCare) {
    }

    public record QuitVO(String id, String name, boolean mine, int targetDays, String startDay,
                         int campDays, int brokeCount, String cheer, String cheerBy, String status,
                         String milestone) {
    }

    public record FitVO(String day, String kind, String kindLabel, int myCount, int partnerCount,
                        boolean linked, boolean partnerIn, long minutesSincePartner) {
    }

    public record SosVO(String id, boolean mine, String symptom, String since, String status,
                        String comfort, String holdBy, List<String> options) {
    }

    public record RedlineVO(String id, String item, String kind, String note, boolean mine) {
    }

    public record RedlineHitVO(String item, int hits, String line) {
    }

    public record CheckupVO(String id, String day, boolean mine, String item, String status,
                            boolean companioned, String report, long daysLeft, boolean iCanCompany,
                            String companionLine) {
    }

    public record MedVO(String week, boolean mine, String how, String note, String reply, String replyBy,
                        boolean iCanReply) {
    }

    public record OathVO(String week, String myLine, String partnerLine, boolean mineSigned,
                         boolean partnerSigned, boolean bothSigned, int myBreach, int myNights,
                         int partnerBreach, int partnerNights, String line) {
    }

    public record BodyVO(String day, String week, List<MetricVO> metrics, SnoreVO snore,
                         List<CycleVO> cycles, List<QuitVO> quits, List<FitVO> fits, List<SosVO> soss,
                         List<RedlineVO> redlines, List<RedlineHitVO> redlineHits,
                         List<CheckupVO> checkups, List<MedVO> meds, OathVO oath) {
    }

    // ========== 读：身体通知总览 ==========

    /** 身体总览（近 7 天数值与运动链、近 14 天周期、当周军令状违约率）。 */
    public BodyVO body(String me) {
        CoupleSpace space = requireSpace(me);
        return build(space, me, LocalDate.now());
    }

    // ========== F310 体征互报 ==========

    /** 报今天的体温/体重/睡眠（当日本人可改写，首报才判异常推 TA）。 */
    public BodyVO metric(String me, String temp, String weight, String sleepHours,
                         String tempLimit, String sleepLimit, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        CoupleBodyMetric row = metricMapper.findByDayUser(space.getId(), day, me);
        boolean fresh = row == null;
        if (fresh) {
            row = CoupleBodyMetric.of(space.getId(), day, me);
        }
        row.setTemp(capped(temp, METRIC_FIELD_MAX, "体温"));
        row.setWeight(capped(weight, METRIC_FIELD_MAX, "体重"));
        row.setSleepHours(capped(sleepHours, METRIC_FIELD_MAX, "睡眠时长"));
        row.setTempLimit(capped(tempLimit, METRIC_FIELD_MAX, "体温线"));
        row.setSleepLimit(capped(sleepLimit, METRIC_FIELD_MAX, "睡眠下限"));
        row.setNote(capped(note, NOTE_MAX, "状态"));
        if (allBlank(row)) {
            throw new BusinessException(400, "至少报一项：体温/体重/睡眠，空报不算打卡");
        }
        row.setUpdatedAt(System.currentTimeMillis());
        if (fresh) {
            metricMapper.insert(row);
        } else {
            metricMapper.updateById(row);
        }
        List<String> alerts = warnOf(row);
        if (fresh && !alerts.isEmpty()) {
            push.pushCoupleEvent("body-metric-alert", me, space.partnerOf(me),
                    CoupleBodyBank.metricAlertLine(CoupleRitualBank.stableHash(space.getId() + "|metric|" + day),
                            me, String.join("、", alerts)));
        }
        return build(space, me, now);
    }

    private boolean allBlank(CoupleBodyMetric row) {
        return row.getTemp().isEmpty() && row.getWeight().isEmpty() && row.getSleepHours().isEmpty();
    }

    /** 只按本人自设的线判「要不要叫醒 TA」，不做任何医学判断。 */
    private List<String> warnOf(CoupleBodyMetric row) {
        List<String> out = new ArrayList<>();
        Double temp = CoupleBodyMetric.num(row.getTemp());
        Double tempLimit = CoupleBodyMetric.num(row.getTempLimit());
        if (temp != null && tempLimit != null && temp > tempLimit) {
            out.add("体温 " + row.getTemp());
        }
        Double sleep = CoupleBodyMetric.num(row.getSleepHours());
        Double sleepLimit = CoupleBodyMetric.num(row.getSleepLimit());
        if (sleep != null && sleepLimit != null && sleep < sleepLimit) {
            out.add("只睡了 " + row.getSleepHours() + " 小时");
        }
        return out;
    }

    // ========== F311 呼噜自报 ==========

    /** 晨起自报打呼档位（当日本人可改）。 */
    public BodyVO snore(String me, String level) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleBodySnore row = ensureSnore(space, now.toString());
        String lv = level == null ? "" : level.trim().toUpperCase();
        if (!CoupleBodySnore.LEVELS.contains(lv)) {
            throw new BusinessException(400, "档位只有 NONE/TINY/MID/HEAVY");
        }
        boolean isA = me.equals(space.getUserA());
        if (isA) {
            row.setLevelA(lv);
        } else {
            row.setLevelB(lv);
        }
        row.setUpdatedAt(System.currentTimeMillis());
        snoreMapper.updateById(row);
        push.pushCoupleEvent("body-snore", me, space.partnerOf(me),
                CoupleBodyBank.snoreLine(CoupleRitualBank.stableHash(space.getId() + "|snore|" + now),
                        CoupleBodySnore.LEVELS.indexOf(lv)));
        return build(space, me, now);
    }

    /** 给对方补「震感」点评。 */
    public BodyVO snoreShake(String me, String text) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleBodySnore row = ensureSnore(space, now.toString());
        String t = trim(text, "震感报告总得写一句");
        if (t.length() > SHAKE_MAX) {
            throw new BusinessException(400, "震感点评最多 " + SHAKE_MAX + " 字");
        }
        if (me.equals(space.getUserA())) {
            row.setShakeB(t);
        } else {
            row.setShakeA(t);
        }
        row.setUpdatedAt(System.currentTimeMillis());
        snoreMapper.updateById(row);
        push.pushCoupleEvent("body-snore-shake", me, space.partnerOf(me), "TA 给你出了一份震感报告 🌋");
        return build(space, me, now);
    }

    // ========== F312 周期共览 ==========

    /** 标记自己当天的阶段与不适（同日本人可改）。 */
    public BodyVO cycleMark(String me, String day, String phase, String discomfort) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String d = day == null || day.isBlank() ? now.toString() : validDay(day, "标记日");
        String ph = phase == null ? "" : phase.trim().toUpperCase();
        if (!CoupleBodyCycle.PHASES.contains(ph)) {
            throw new BusinessException(400, "阶段只有提前预警/进行中/收尾期/排卵期");
        }
        String dc = discomfort == null ? "" : discomfort.trim();
        if (dc.length() > DISCOMFORT_MAX) {
            throw new BusinessException(400, "不适最多 " + DISCOMFORT_MAX + " 字");
        }
        CoupleBodyCycle row = cycleMapper.findByDayUser(space.getId(), d, me);
        boolean fresh = row == null;
        if (fresh) {
            cycleMapper.insert(CoupleBodyCycle.of(space.getId(), d, me, ph, dc));
            push.pushCoupleEvent("body-cycle", me, space.partnerOf(me),
                    "TA 标了今天：" + CoupleBodyBank.phaseLabel(ph) + (dc.isEmpty() ? "" : "｜" + dc) + "，照顾卡可以递了 🫂");
        } else {
            row.setPhase(ph);
            row.setDiscomfort(dc);
            row.setUpdatedAt(System.currentTimeMillis());
            cycleMapper.updateById(row);
        }
        return build(space, me, now);
    }

    /** 递照顾卡（只能给 TA 标的那天递）。 */
    public BodyVO cycleCare(String me, String day, String card) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String d = day == null || day.isBlank() ? now.toString() : validDay(day, "照顾日");
        CoupleBodyCycle row = cycleMapper.findByDayUser(space.getId(), d, space.partnerOf(me));
        if (row == null) {
            throw new BusinessException(400, "TA 那天没标，卡递过去也没人接");
        }
        String c = trim(card, "照顾卡要写一句我能做");
        if (c.length() > CARD_MAX) {
            throw new BusinessException(400, "照顾卡最多 " + CARD_MAX + " 字");
        }
        row.setCareCard(c);
        row.setCareBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        cycleMapper.updateById(row);
        push.pushCoupleEvent("body-care", me, row.getFromUser(), "照顾卡送到：「" + c + "」🧣");
        return build(space, me, now);
    }

    // ========== F313 戒烟戒糖互助营 ==========

    /** 开一个营（本人目标，营期 7/21/60/100 天）。 */
    public BodyVO quitStart(String me, String name, Integer targetDays, String startDay) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String n = trim(name, "戒什么要写");
        if (n.length() > QUIT_NAME_MAX) {
            throw new BusinessException(400, "目标名最多 " + QUIT_NAME_MAX + " 字");
        }
        int days = targetDays == null ? 21 : targetDays;
        if (days < CAMP_DAYS_MIN || days > CAMP_DAYS_MAX) {
            throw new BusinessException(400, "营期天数在 " + CAMP_DAYS_MIN + "-" + CAMP_DAYS_MAX + " 之间");
        }
        if (quitMapper.findOwnerName(space.getId(), me, n) != null) {
            throw new BusinessException(400, "这个营你已经开过了，先打完这一期");
        }
        String sd = startDay == null || startDay.isBlank() ? now.toString() : validDay(startDay, "开营日");
        quitMapper.insert(CoupleBodyQuit.of(space.getId(), n, me, days, sd));
        push.pushCoupleEvent("body-quit", me, space.partnerOf(me),
                "TA 开了个营：「" + n + "」" + days + " 天，你是陪绑的人 🤝");
        return build(space, me, now);
    }

    /** 记一天破戒（本人，同日幂等）。 */
    public BodyVO quitBroke(String me, String id, String day) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleBodyQuit camp = requireCamp(space, id);
        if (!camp.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "破戒只有本人能记，别替 TA 认");
        }
        if (!CoupleBodyQuit.STATUS_OPEN.equals(camp.getStatus())) {
            throw new BusinessException(400, "这个营已经结营了");
        }
        String d = day == null || day.isBlank() ? now.toString() : validDay(day, "破戒日");
        // 记号压成 MMdd（broke_days 只有 160 宽，ISO 日期十几条就顶满），一期最多留 25 条
        String token = d.replace("-", "").substring(4);
        if (camp.hasBroke(token)) {
            return build(space, me, now);
        }
        if (camp.brokeCount() >= 25) {
            throw new BusinessException(400, "破戒已经记了 25 条，先把这一期结掉再开新的");
        }
        camp.setBrokeDays(camp.getBrokeDays().isEmpty() ? token : camp.getBrokeDays() + "," + token);
        camp.setUpdatedAt(System.currentTimeMillis());
        quitMapper.updateById(camp);
        push.pushCoupleEvent("body-quit-broke", me, space.partnerOf(me),
                "「" + camp.getName() + "」今天破了一次，安慰词该送上了 🍬");
        return build(space, me, now);
    }

    /** 陪绑方送安慰词（只有对方能送）。 */
    public BodyVO quitCheer(String me, String id, String cheer) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleBodyQuit camp = requireCamp(space, id);
        if (camp.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "安慰词是陪绑的人说的，自己夸不算");
        }
        String c = trim(cheer, "安慰词要写一句");
        if (c.length() > CHEER_MAX) {
            throw new BusinessException(400, "安慰词最多 " + CHEER_MAX + " 字");
        }
        camp.setCheer(c);
        camp.setCheerBy(me);
        camp.setUpdatedAt(System.currentTimeMillis());
        quitMapper.updateById(camp);
        push.pushCoupleEvent("body-quit-cheer", me, camp.getOwnerUser(), "陪绑的人说话了：「" + c + "」");
        return build(space, me, now);
    }

    /** 宣布结营（本人：满天数=DONE，提前收=GONE）。 */
    public BodyVO quitClose(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleBodyQuit camp = requireCamp(space, id);
        if (!camp.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "结营要本人宣布");
        }
        if (!CoupleBodyQuit.STATUS_OPEN.equals(camp.getStatus())) {
            throw new BusinessException(400, "这个营已经结过了");
        }
        int campDays = campDays(camp, now);
        camp.setStatus(campDays >= camp.getTargetDays() ? CoupleBodyQuit.STATUS_DONE : CoupleBodyQuit.STATUS_GONE);
        camp.setUpdatedAt(System.currentTimeMillis());
        quitMapper.updateById(camp);
        push.pushCoupleEventBoth("body-quit-close", me, space.getUserA(), space.getUserB(),
                "「" + camp.getName() + "」营期收官：" + campDays + " 天，破戒 " + camp.brokeCount() + " 次 🏁");
        return build(space, me, now);
    }

    // ========== F314 运动链 ==========

    /** 报今天某项目的计数（本人可改；两人 30 分钟内都报=接上链）。 */
    public BodyVO fit(String me, String kind, Integer count) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        String k = kind == null ? "" : kind.trim().toUpperCase();
        if (!CoupleBodyFit.KINDS.contains(k)) {
            throw new BusinessException(400, "项目只有 PUSHUP/SQUAT/PLANK/RUN/STRETCH");
        }
        int c = count == null ? 0 : count;
        if (c < 0 || c > 9999) {
            throw new BusinessException(400, "计数写 0-9999 之间的数就行");
        }
        CoupleBodyFit row = fitMapper.findByDayKind(space.getId(), day, k);
        boolean fresh = row == null;
        if (fresh) {
            row = CoupleBodyFit.of(space.getId(), day, k);
        }
        boolean isA = me.equals(space.getUserA());
        long ts = System.currentTimeMillis();
        if (isA) {
            row.setCountA(c);
            row.setAtA(ts);
        } else {
            row.setCountB(c);
            row.setAtB(ts);
        }
        boolean wasLinked = row.linkedFlag();
        row.setLinked(row.getAtA() > 0 && row.getAtB() > 0
                && Math.abs(row.getAtA() - row.getAtB()) <= CoupleBodyFit.LINK_WINDOW_MS ? 1 : 0);
        row.setUpdatedAt(ts);
        if (fresh) {
            fitMapper.insert(row);
        } else {
            fitMapper.updateById(row);
        }
        if (row.linkedFlag() && !wasLinked) {
            push.pushCoupleEventBoth("body-fit-link", me, space.getUserA(), space.getUserB(),
                    fitLabel(k) + " 链子接上了：" + row.getCountA() + " ↔ " + row.getCountB() + " 🔗");
        } else if (!row.linkedFlag()) {
            push.pushCoupleEvent("body-fit", me, space.partnerOf(me),
                    "TA 报了" + fitLabel(k) + " " + (isA ? row.getCountA() : row.getCountB())
                            + " 个，30 分钟内接上才算链 🔗");
        }
        return build(space, me, now);
    }

    // ========== F315 身体不适 SOS ==========

    /** 一键不舒服（在途一条）。 */
    public BodyVO sos(String me, String symptom, String since) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String s = trim(symptom, "哪里不舒服写一句");
        if (s.length() > SYMPTOM_MAX) {
            throw new BusinessException(400, "症状最多 " + SYMPTOM_MAX + " 字");
        }
        List<CoupleBodySos> open = sosMapper.findOpen(space.getId());
        long mineOpen = open.stream().filter(x -> x.getFromUser().equals(me)).count();
        if (mineOpen >= SOS_INFLIGHT_MAX) {
            throw new BusinessException(400, "你已有一条不舒服还没被接住，先等 TA 回");
        }
        CoupleBodySos row = CoupleBodySos.of(space.getId(), me, s, since == null ? "" : since.trim());
        sosMapper.insert(row);
        push.pushCoupleEvent("body-sos", me, space.partnerOf(me),
                "TA 说不舒服：「" + s + "」" + (row.getSince().isEmpty() ? "" : "（从 " + row.getSince() + " 起）")
                        + "，选一张「我能做」递回去 🫂");
        return build(space, me, now);
    }

    /** 接住（只有对方能接，从 Bank 选项卡里选一句）。 */
    public BodyVO sosHold(String me, String id, String comfort) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleBodySos row = findSos(space, id);
        if (row == null) {
            throw new BusinessException(400, "这条不舒服不存在");
        }
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己发的 SOS 自己接不住");
        }
        if (!CoupleBodySos.STATUS_SENT.equals(row.getStatus())) {
            throw new BusinessException(400, "这条已经有人接过了");
        }
        String c = trim(comfort, "「我能做」要选一句");
        if (c.length() > CARD_MAX) {
            throw new BusinessException(400, "回执最多 " + CARD_MAX + " 字");
        }
        row.setStatus(CoupleBodySos.STATUS_HELD);
        row.setComfort(c);
        row.setHoldBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        sosMapper.updateById(row);
        push.pushCoupleEvent("body-sos-held", me, row.getFromUser(), CoupleBodyBank.sosHeldLine() + "｜" + c);
        return build(space, me, now);
    }

    // ========== F316 忌口红线本 ==========

    /** 登记忌口/过敏项（同空间唯一）。 */
    public BodyVO redlineAdd(String me, String item, String kind, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String it = trim(item, "忌口项要写");
        if (it.length() > REDLINE_ITEM_MAX) {
            throw new BusinessException(400, "忌口项最多 " + REDLINE_ITEM_MAX + " 字");
        }
        if (redlineMapper.findByItem(space.getId(), it) != null) {
            throw new BusinessException(400, "「" + it + "」已经在红线本上了");
        }
        String k = kind == null || kind.isBlank() ? CoupleBodyRedline.KIND_AVOID : kind.trim().toUpperCase();
        if (!CoupleBodyRedline.KIND_ALLERGY.equals(k) && !CoupleBodyRedline.KIND_AVOID.equals(k)) {
            throw new BusinessException(400, "只有过敏 ALLERGY 和忌口 AVOID 两种");
        }
        String n = note == null ? "" : note.trim();
        if (n.length() > DISCOMFORT_MAX) {
            throw new BusinessException(400, "说明最多 " + DISCOMFORT_MAX + " 字");
        }
        redlineMapper.insert(CoupleBodyRedline.of(space.getId(), it, k, n, me));
        push.pushCoupleEventBoth("body-redline", me, space.getUserA(), space.getUserB(),
                "红线本上新增「" + it + "」（" + (CoupleBodyRedline.KIND_ALLERGY.equals(k) ? "过敏，碰都不能碰" : "忌口")
                        + "），点菜前先翻这本 🚫");
        return build(space, me, now);
    }

    /** 划掉一条红线（登记人才能划）。 */
    public BodyVO redlineRemove(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleBodyRedline row = id == null ? null : redlineMapper.findBySpace(space.getId()).stream()
                .filter(r -> r.getId().equals(id)).findFirst().orElse(null);
        if (row == null) {
            throw new BusinessException(400, "这条红线不在本子上");
        }
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "谁登记的谁才能划掉");
        }
        redlineMapper.deleteById(id);
        return build(space, me, now);
    }

    // ========== F317 体检陪同 ==========

    /** 约一次体检（本人一天一次）。 */
    public BodyVO checkupPlan(String me, String day, String item) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String d = day == null || day.isBlank() ? now.toString() : validDay(day, "体检日");
        String it = item == null ? "" : item.trim();
        if (it.length() > CHECKUP_ITEM_MAX) {
            throw new BusinessException(400, "查什么最多 " + CHECKUP_ITEM_MAX + " 字");
        }
        if (checkupMapper.findByDayUser(space.getId(), d, me) != null) {
            throw new BusinessException(400, "那天已经约过一次了");
        }
        checkupMapper.insert(CoupleBodyCheckup.of(space.getId(), d, me, it));
        push.pushCoupleEvent("body-checkup", me, space.partnerOf(me),
                "TA 约了 " + d + " 体检" + (it.isEmpty() ? "" : "：" + it) + "，虚拟陪同可以到场 🩺");
        return build(space, me, now);
    }

    /** TA 虚拟陪同到场打卡（只有对方能到）。 */
    public BodyVO checkupCompany(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleBodyCheckup row = requireCheckup(space, id);
        if (row.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "自己的到场不算陪同，等 TA 来打卡");
        }
        if (row.isCompanioned()) {
            return build(space, me, now);
        }
        row.setCompanion(1);
        row.setUpdatedAt(System.currentTimeMillis());
        checkupMapper.updateById(row);
        push.pushCoupleEvent("body-companion", me, row.getOwnerUser(), CoupleBodyBank.companionLine(
                CoupleRitualBank.stableHash(space.getId() + "|companion|" + row.getId())));
        return build(space, me, now);
    }

    /** 检后一句话报告（本人，互见）。 */
    public BodyVO checkupReport(String me, String id, String report) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleBodyCheckup row = requireCheckup(space, id);
        if (!row.getOwnerUser().equals(me)) {
            throw new BusinessException(400, "报告只有体检本人能写");
        }
        String r = trim(report, "检后一句话总得写");
        if (r.length() > REPORT_MAX) {
            throw new BusinessException(400, "报告最多 " + REPORT_MAX + " 字");
        }
        row.setReport(r);
        row.setStatus(CoupleBodyCheckup.STATUS_REPORTED);
        row.setUpdatedAt(System.currentTimeMillis());
        checkupMapper.updateById(row);
        push.pushCoupleEventBoth("body-report", me, space.getUserA(), space.getUserB(),
                "体检报告一句话：「" + r + "」——看完了，我还在 🫂");
        return build(space, me, now);
    }

    // ========== F318 情绪药友 ==========

    /** 本周记一笔「最近药怎么样」（自愿，同周本人可改）。 */
    public BodyVO medLog(String me, String how, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String week = now.with(DayOfWeek.MONDAY).toString();
        String h = how == null ? "" : how.trim().toUpperCase();
        if (!CoupleBodyMed.HOWS.contains(h)) {
            throw new BusinessException(400, "只有 STEADY/HARD/NONE 三档");
        }
        String n = note == null ? "" : note.trim();
        if (n.length() > CHEER_MAX) {
            throw new BusinessException(400, "随笔最多 " + CHEER_MAX + " 字");
        }
        CoupleBodyMed row = medMapper.findByWeekUser(space.getId(), week, me);
        boolean fresh = row == null;
        if (fresh) {
            row = CoupleBodyMed.of(space.getId(), week, me);
        }
        row.setHow(h);
        row.setNote(n);
        row.setUpdatedAt(System.currentTimeMillis());
        if (fresh) {
            medMapper.insert(row);
            push.pushCoupleEvent("body-med", me, space.partnerOf(me),
                    "TA 记了本周的身体账：" + medLabel(h) + "，可以回一句陪伴话术 💊");
        } else {
            medMapper.updateById(row);
        }
        return build(space, me, now);
    }

    /** 给对方回一句陪伴话术（只陪不诊断，非医嘱）。 */
    public BodyVO medReply(String me, String week, String reply) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String w = week == null || week.isBlank() ? now.with(DayOfWeek.MONDAY).toString() : validDay(week, "周锚");
        CoupleBodyMed row = medMapper.findByWeekUser(space.getId(), w, space.partnerOf(me));
        if (row == null) {
            throw new BusinessException(400, "TA 那周没记，回话没处放");
        }
        String r = trim(reply, "回一句陪伴的话");
        if (r.length() > CHEER_MAX) {
            throw new BusinessException(400, "回话最多 " + CHEER_MAX + " 字");
        }
        row.setReply(r);
        row.setReplyBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        medMapper.updateById(row);
        push.pushCoupleEvent("body-med-reply", me, row.getFromUser(), "TA 回话了：「" + r + "」");
        return build(space, me, now);
    }

    // ========== F319 早睡军令状 ==========

    /** 签本周熄灯线（HH:mm，本人可改自己的；双签齐了才推 both）。 */
    public BodyVO oathSign(String me, String line) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String week = now.with(DayOfWeek.MONDAY).toString();
        String l = trim(line, "熄灯线要写时点");
        if (!l.matches("\\d{2}:\\d{2}") || Integer.parseInt(l.substring(0, 2)) > 23
                || Integer.parseInt(l.substring(3)) > 59) {
            throw new BusinessException(400, "熄灯线写成 HH:mm，比如 23:30");
        }
        CoupleBodyOath row = ensureOath(space, week);
        boolean isA = me.equals(space.getUserA());
        boolean wasBoth = row.isBothSigned();
        if (isA) {
            row.setLineA(l);
            row.setSignedA(1);
        } else {
            row.setLineB(l);
            row.setSignedB(1);
        }
        row.setUpdatedAt(System.currentTimeMillis());
        oathMapper.updateById(row);
        if (row.isBothSigned() && !wasBoth) {
            push.pushCoupleEventBoth("body-oath-sign", me, space.getUserA(), space.getUserB(),
                    CoupleBodyBank.oathSignedLine() + "（" + row.getLineA() + " / " + row.getLineB() + "）");
        } else if (!wasBoth) {
            push.pushCoupleEvent("body-oath", me, space.partnerOf(me),
                    "TA 把本周熄灯线签成 " + l + "，等你签你的那条 🌙");
        }
        return build(space, me, now);
    }

    // ========== 聚合 ==========

    private BodyVO build(CoupleSpace space, String me, LocalDate now) {
        String today = now.toString();
        String week = now.with(DayOfWeek.MONDAY).toString();
        String from7 = now.minusDays(6).toString();
        String from14 = now.minusDays(13).toString();
        boolean isA = me.equals(space.getUserA());

        List<MetricVO> metrics = new ArrayList<>();
        for (CoupleBodyMetric m : metricMapper.findRecent(space.getId(), from7)) {
            List<String> alerts = warnOf(m);
            metrics.add(new MetricVO(m.getDay(), me.equals(m.getFromUser()), m.getTemp(), m.getWeight(),
                    m.getSleepHours(), m.getTempLimit(), m.getSleepLimit(), m.getNote(), !alerts.isEmpty(),
                    alerts.isEmpty() ? "" : String.join("、", alerts)));
        }

        CoupleBodySnore snoreRow = ensureSnore(space, today);
        String myLevel = isA ? snoreRow.getLevelA() : snoreRow.getLevelB();
        String partnerLevel = isA ? snoreRow.getLevelB() : snoreRow.getLevelA();
        String myShake = isA ? snoreRow.getShakeB() : snoreRow.getShakeA();
        String partnerShake = isA ? snoreRow.getShakeA() : snoreRow.getShakeB();
        SnoreVO snoreVO = new SnoreVO(today, myLevel, partnerLevel, myShake, partnerShake,
                !partnerLevel.isEmpty() && myShake.isEmpty(),
                Math.max(snoreRow.shakeScore(isA ? false : true), 0));

        List<CycleVO> cycles = new ArrayList<>();
        for (CoupleBodyCycle c : cycleMapper.findRecent(space.getId(), from14)) {
            cycles.add(new CycleVO(c.getDay(), me.equals(c.getFromUser()), c.getPhase(),
                    CoupleBodyBank.phaseLabel(c.getPhase()), c.getDiscomfort(), c.getCareCard(), c.getCareBy(),
                    !me.equals(c.getFromUser()) && c.getCareCard().isEmpty()));
        }

        List<QuitVO> quits = new ArrayList<>();
        for (CoupleBodyQuit q : quitMapper.findBySpace(space.getId())) {
            int campDays = campDays(q, now);
            quits.add(new QuitVO(q.getId(), q.getName(), me.equals(q.getOwnerUser()), q.getTargetDays(),
                    q.getStartDay(), campDays, q.brokeCount(), q.getCheer(), q.getCheerBy(), q.getStatus(),
                    campDays > 0 && campDays % 7 == 0 ? CoupleBodyBank.campMilestoneLine(campDays) : ""));
        }

        Map<String, CoupleBodyFit> fitToday = new LinkedHashMap<>();
        for (CoupleBodyFit f : fitMapper.findRecent(space.getId(), from7)) {
            fitToday.putIfAbsent(f.getDay() + "|" + f.getKind(), f);
        }
        List<FitVO> fits = new ArrayList<>();
        long ts = System.currentTimeMillis();
        for (CoupleBodyFit f : fitToday.values()) {
            boolean mineA = isA;
            int myCount = mineA ? f.getCountA() : f.getCountB();
            int partnerCount = mineA ? f.getCountB() : f.getCountA();
            long partnerAt = mineA ? f.getAtB() : f.getAtA();
            fits.add(new FitVO(f.getDay(), f.getKind(), fitLabel(f.getKind()), myCount, partnerCount,
                    f.linkedFlag(), partnerAt > 0, partnerAt > 0 ? Math.max(0, (ts - partnerAt) / 60000L) : -1));
        }

        List<SosVO> soss = new ArrayList<>();
        for (CoupleBodySos s : sosMapper.findBySpace(space.getId())) {
            boolean mine = me.equals(s.getFromUser());
            soss.add(new SosVO(s.getId(), mine, s.getSymptom(), s.getSince(), s.getStatus(), s.getComfort(),
                    s.getHoldBy(), mine && CoupleBodySos.STATUS_SENT.equals(s.getStatus())
                    ? List.of() : CoupleBodyBank.SOS_OPTIONS));
        }

        List<CoupleBodyRedline> redlines = redlineMapper.findBySpace(space.getId());
        List<RedlineHitVO> hits = new ArrayList<>();
        List<CoupleDineTicket> tickets = dineMapper.findByDay(space.getId(), today);
        for (CoupleBodyRedline r : redlines) {
            int n = 0;
            for (CoupleDineTicket t : tickets) {
                if (t.getDish() != null && t.getDish().contains(r.getItem())) {
                    n++;
                }
            }
            if (n > 0) {
                hits.add(new RedlineHitVO(r.getItem(), n, CoupleBodyBank.redlineHitLine(r.getItem(), n)));
            }
        }

        List<CheckupVO> checkups = new ArrayList<>();
        for (CoupleBodyCheckup c : checkupMapper.findBySpace(space.getId())) {
            boolean mine = me.equals(c.getOwnerUser());
            long left = ChronoUnit.DAYS.between(now, LocalDate.parse(c.getDay()));
            checkups.add(new CheckupVO(c.getId(), c.getDay(), mine, c.getItem(), c.getStatus(),
                    c.isCompanioned(), c.getReport(), left, !mine && !c.isCompanioned(),
                    c.isCompanioned() ? CoupleBodyBank.companionLine(
                            CoupleRitualBank.stableHash(space.getId() + "|companion|" + c.getId())) : ""));
        }

        List<MedVO> meds = new ArrayList<>();
        for (CoupleBodyMed m : medMapper.findRecent(space.getId(), now.minusDays(70).with(DayOfWeek.MONDAY).toString())) {
            boolean mine = me.equals(m.getFromUser());
            meds.add(new MedVO(m.getWeek(), mine, m.getHow(), m.getNote(), m.getReply(), m.getReplyBy(),
                    !mine && m.getReply().isEmpty()));
        }

        CoupleBodyOath oath = ensureOath(space, week);
        String myLine = isA ? oath.getLineA() : oath.getLineB();
        String partnerLine = isA ? oath.getLineB() : oath.getLineA();
        int[] myStat = breachOf(myLine, isA ? space.getUserA() : space.getUserB(), space.getId(), week, now);
        int[] partnerStat = breachOf(partnerLine, isA ? space.getUserB() : space.getUserA(), space.getId(), week, now);
        int rate = myStat[1] + partnerStat[1] == 0 ? 0
                : (int) Math.round(100.0 * (myStat[0] + partnerStat[0]) / (myStat[1] + partnerStat[1]));
        OathVO oathVO = new OathVO(week, myLine, partnerLine, isSigned(oath, isA), isSigned(oath, !isA),
                oath.isBothSigned(), myStat[0], myStat[1], partnerStat[0], partnerStat[1],
                oath.isBothSigned() ? CoupleBodyBank.oathBreachLine(
                        CoupleRitualBank.stableHash(space.getId() + "|oath|" + week), rate) : "");

        return new BodyVO(today, week, metrics, snoreVO, cycles, quits, fits, soss,
                redlines.stream().map(r -> new RedlineVO(r.getId(), r.getItem(), r.getKind(), r.getNote(),
                        me.equals(r.getFromUser()))).toList(),
                hits, checkups, meds, oathVO);
    }

    // ========== 小工具 ==========

    /** 本周违约夜数与已熄灯夜数（读 F220 晚安同熄灯数据，只比时点字符串）。 */
    private int[] breachOf(String line, String user, String spaceId, String week, LocalDate now) {
        if (line.isEmpty()) {
            return new int[]{0, 0};
        }
        String today = now.toString();
        String weekEnd = now.with(DayOfWeek.SUNDAY).toString();
        String to = weekEnd.compareTo(today) > 0 ? today : weekEnd;
        int nights = 0;
        int breach = 0;
        for (CoupleCozyLightout l : lightoutMapper.findRange(spaceId, week, to)) {
            if (!user.equals(l.getFromUser()) || l.getAtTime() == null || l.getAtTime().isEmpty()) {
                continue;
            }
            nights++;
            if (l.getAtTime().compareTo(line) > 0) {
                breach++;
            }
        }
        return new int[]{breach, nights};
    }

    private boolean isSigned(CoupleBodyOath oath, boolean sideA) {
        return sideA ? oath.signedAFlag() : oath.signedBFlag();
    }

    private int campDays(CoupleBodyQuit camp, LocalDate now) {
        long d = ChronoUnit.DAYS.between(LocalDate.parse(camp.getStartDay()), now) + 1;
        return (int) Math.max(0, Math.min(d, camp.getTargetDays()));
    }

    private String fitLabel(String kind) {
        return switch (kind) {
            case "SQUAT" -> "深蹲";
            case "PLANK" -> "平板支撑";
            case "RUN" -> "跑步";
            case "STRETCH" -> "拉伸";
            default -> "俯卧撑";
        };
    }

    private String medLabel(String how) {
        return switch (how) {
            case "HARD" -> "这周难";
            case "NONE" -> "这周没记";
            default -> "这周稳";
        };
    }

    private CoupleBodySnore ensureSnore(CoupleSpace space, String day) {
        CoupleBodySnore row = snoreMapper.findByDay(space.getId(), day);
        if (row != null) {
            return row;
        }
        CoupleBodySnore fresh = CoupleBodySnore.of(space.getId(), day);
        snoreMapper.insert(fresh);
        return fresh;
    }

    private CoupleBodyOath ensureOath(CoupleSpace space, String week) {
        CoupleBodyOath row = oathMapper.findByWeek(space.getId(), week);
        if (row != null) {
            return row;
        }
        CoupleBodyOath fresh = CoupleBodyOath.of(space.getId(), week);
        oathMapper.insert(fresh);
        return fresh;
    }

    private CoupleBodyQuit requireCamp(CoupleSpace space, String id) {
        CoupleBodyQuit row = id == null ? null : quitMapper.findBySpace(space.getId()).stream()
                .filter(q -> q.getId().equals(id)).findFirst().orElse(null);
        if (row == null) {
            throw new BusinessException(400, "这个营不存在");
        }
        return row;
    }

    private CoupleBodyCheckup requireCheckup(CoupleSpace space, String id) {
        CoupleBodyCheckup row = id == null ? null : checkupMapper.findBySpace(space.getId()).stream()
                .filter(c -> c.getId().equals(id)).findFirst().orElse(null);
        if (row == null) {
            throw new BusinessException(400, "这次体检没约过");
        }
        return row;
    }

    private CoupleBodySos findSos(CoupleSpace space, String id) {
        if (id == null) {
            return null;
        }
        return sosMapper.findBySpace(space.getId()).stream()
                .filter(s -> s.getId().equals(id)).findFirst().orElse(null);
    }

    private String capped(String v, int max, String label) {
        String t = v == null ? "" : v.trim();
        if (t.length() > max) {
            throw new BusinessException(400, label + "最多 " + max + " 字");
        }
        return t;
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private String validDay(String v, String label) {
        try {
            return LocalDate.parse(v.trim()).toString();
        } catch (java.time.format.DateTimeParseException e) {
            throw new BusinessException(400, label + "要写成 yyyy-MM-dd");
        }
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
