package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 情绪关怀：情绪天气预报、情绪急救箱、和好卡、夸夸墙、生理期关怀。
 * 情绪价值设计：低落时被第一时间看见、吵架有台阶下、欣赏被大声说出来、
 * 特殊日子里被温柔以待。
 */
@Service
public class CoupleCareService {

    /** 负面心情集合（急救箱判定用）。 */
    private static final List<String> NEGATIVE_MOODS = List.of("SAD", "ANGRY", "SICK", "TIRED");

    /** 哄 TA 指南（每天按空间固定推一条）。 */
    private static final List<String> FIRST_AID = List.of(
            "什么都不问，先给 TA 一个 30 秒的拥抱",
            "给 TA 点一杯 TA 爱喝的热饮，附一句「辛苦了」",
            "今晚的晚饭你来做，让 TA 歇着",
            "发一段语音，语速放慢，就说「我在呢」",
            "陪 TA 看一集 TA 爱看的剧，不抢遥控器",
            "帮 TA 把明天的琐事包了，只说「明天你别操心」",
            "递一张手写小纸条，画一个抱抱",
            "让 TA 靠在你肩膀上十分钟，什么都不聊",
            "陪 TA 出门走十分钟，只散步，不讲道理",
            "给 TA 准备一个热水袋——情绪也需要物理温暖",
            "把 TA 最近夸过你的一次，再复述给 TA 听",
            "睡前把「今天辛苦了」换成「有你真好」",
            "不要讲道理，不要分析对错，先共情",
            "问 TA「想倾诉还是想安静」，然后照做",
            "今天早点回家/早点上线，多陪一会儿"
    );

    // ========== VO ==========

    /** 情绪天气预报：今天双方的心情 + 一句贴心提示。 */
    public record WeatherVO(String myMood, String myEmoji, String partnerMood, String partnerEmoji, String tip) {
    }

    /** 情绪急救箱：TA 连续低落天数 + 今天怎么哄 TA。 */
    public record FirstAidVO(int negativeDays, String suggestion, boolean urgent) {
    }

    public record ReconcileVO(String id, String fromUser, String message, Long startAt, String status,
                              Long acceptedAt, Long durationHours, boolean mine, Long created) {
    }

    public record PraiseVO(String id, String fromUser, String content, String status,
                           Long receivedAt, boolean mine, Long created) {
    }

    /** 生理期卡片：双方各自的记录与预告（没记录的是 null）。 */
    public record CycleCardVO(CycleSideVO mine, CycleSideVO partner) {
    }

    public record CycleSideVO(String username, String periodDay, Integer cycleDays, Integer periodDays,
                              String note, String nextDate, Long nextInDays, boolean inPeriod) {
    }

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleMoodMapper moodMapper;
    private final CoupleReconcileMapper reconcileMapper;
    private final CouplePraiseMapper praiseMapper;
    private final CoupleCycleMapper cycleMapper;
    private final ImPushService push;

    public CoupleCareService(CoupleSpaceMapper spaceMapper, CoupleMoodMapper moodMapper,
                             CoupleReconcileMapper reconcileMapper, CouplePraiseMapper praiseMapper,
                             CoupleCycleMapper cycleMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.moodMapper = moodMapper;
        this.reconcileMapper = reconcileMapper;
        this.praiseMapper = praiseMapper;
        this.cycleMapper = cycleMapper;
        this.push = push;
    }

    // ========== F11 情绪天气预报 ==========

    /** 今天双方的心情天气 + 一句贴心提示（首页天气条）。 */
    public WeatherVO weather(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String day = LocalDate.now().toString();
        CoupleMood mine = moodMapper.find(space.getId(), me, day).orElse(null);
        CoupleMood theirs = moodMapper.find(space.getId(), partner, day).orElse(null);
        String myMood = mine == null ? null : mine.getMood();
        String partnerMood = theirs == null ? null : theirs.getMood();
        String myEmoji = myMood == null ? "🌫️" : CoupleMood.emojiOf(myMood);
        String partnerEmoji = partnerMood == null ? "🌫️" : CoupleMood.emojiOf(partnerMood);
        return new WeatherVO(myMood, myEmoji, partnerMood, partnerEmoji, weatherTip(myMood, partnerMood));
    }

    private String weatherTip(String myMood, String partnerMood) {
        boolean mineBad = myMood != null && NEGATIVE_MOODS.contains(myMood);
        boolean theirsBad = partnerMood != null && NEGATIVE_MOODS.contains(partnerMood);
        if (mineBad && theirsBad) {
            return "今天你们都有点丧，抱一下，天气会放晴的 ☁️";
        }
        if (theirsBad) {
            return "TA 今天天气不太好，去贴一个抱抱吧 🤗";
        }
        if (mineBad) {
            return "你今天有点低落，TA 很愿意听你说说 💗";
        }
        if (partnerMood != null && myMood != null) {
            return "今天双晴！这样的日子要存进回忆里 ☀️";
        }
        if (partnerMood == null && myMood != null) {
            return "TA 今天还没记心情，提醒 TA 一下吧 🌈";
        }
        return "记录今天的心情，让 TA 看见你的天气 🌈";
    }

    // ========== F12 情绪急救箱 ==========

    /** TA 最近连续低落天数 + 今天怎么哄 TA（TA 无记录返回 0）。 */
    public FirstAidVO firstAid(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        int streak = negativeStreak(space, partner);
        String suggestion = FIRST_AID.get(Math.floorMod(
                CoupleRitualBank.stableHash(space.getId() + "|aid|" + LocalDate.now()), FIRST_AID.size()));
        return new FirstAidVO(streak, suggestion, streak >= 2);
    }

    /** 某人最近连续「负面心情」的自然日数（从今天往前数，最多看 14 天）。 */
    private int negativeStreak(CoupleSpace space, String username) {
        int streak = 0;
        LocalDate cursor = LocalDate.now();
        for (int i = 0; i < 14; i++) {
            CoupleMood row = moodMapper.find(space.getId(), username, cursor.toString()).orElse(null);
            if (row != null && NEGATIVE_MOODS.contains(row.getMood())) {
                streak++;
                cursor = cursor.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    /**
     * 情绪急救主动推送（每天 10:00 由提醒任务调用）：TA 连续 2 天及以上心情低落时，
     * 给对方推一条「TA 最近有点低落」+ 今天怎么哄 TA 的具体建议。
     */
    public void remindLowMoods() {
        String suggestion = FIRST_AID.get(Math.floorMod(
                CoupleRitualBank.stableHash("aid|" + LocalDate.now()), FIRST_AID.size()));
        for (CoupleSpace space : spaceMapper.findAllActive()) {
            for (String user : new String[]{space.getUserA(), space.getUserB()}) {
                int streak = negativeStreak(space, space.partnerOf(user));
                if (streak >= 2) {
                    push.pushCoupleEvent("first-aid", "system", user,
                            "TA 最近连续 " + streak + " 天心情都有点低落 💧 今天试试：" + suggestion);
                }
            }
        }
    }

    // ========== F13 和好卡 ==========

    /** 递一张和好卡（有一张还没被接受时不允许再递）。 */
    public ReconcileVO sendReconcile(String me, String message, Long startAt) {
        CoupleSpace space = requireSpace(me);
        String text = requireText(message, "写下和好的话（1-200 字）", CoupleReconcile.MESSAGE_MAX);
        if (startAt != null && startAt > System.currentTimeMillis()) {
            throw new BusinessException(400, "别扭开始时间不能在未来哦");
        }
        // 已接受的历史卡都算完；只挡「还没接受」的
        boolean pending = reconcileMapper.findBySpace(space.getId()).stream()
                .anyMatch(r -> CoupleReconcile.STATUS_SENT.equals(r.getStatus()));
        if (pending) {
            throw new BusinessException(409, "已经有一张和好卡在路上了，等 TA 接受吧");
        }
        CoupleReconcile card = CoupleReconcile.of(space.getId(), me, text, startAt);
        reconcileMapper.insert(card);
        push.pushCoupleEvent("reconcile-sent", me, space.partnerOf(me),
                "TA 给你递了一张和好卡 🤍：" + text);
        return toReconcileVO(card, me);
    }

    /** 和好卡列表（新→旧）。 */
    public List<ReconcileVO> listReconciles(String me) {
        CoupleSpace space = requireSpace(me);
        return reconcileMapper.findBySpace(space.getId()).stream()
                .map(r -> toReconcileVO(r, me))
                .toList();
    }

    /** 接受和好卡：只有收卡人（非递卡人）能接受。 */
    public ReconcileVO acceptReconcile(String me, String cardId) {
        CoupleSpace space = requireSpace(me);
        CoupleReconcile card = reconcileMapper.selectById(cardId);
        if (card == null || !card.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这张和好卡不存在");
        }
        if (card.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己递的卡要等 TA 来接受哦");
        }
        if (CoupleReconcile.STATUS_ACCEPTED.equals(card.getStatus())) {
            throw new BusinessException(409, "这张卡已经被接受过啦");
        }
        card.setStatus(CoupleReconcile.STATUS_ACCEPTED);
        card.setAcceptedBy(me);
        card.setAcceptedAt(System.currentTimeMillis());
        reconcileMapper.updateById(card);
        Long hours = card.durationHours();
        String detail = "你们和好啦 🤗 这次小别扭" + (hours == null ? "" : "持续了 " + hours + " 小时，") + "现在又是晴天了！";
        push.pushCoupleEventBoth("reconcile-accepted", me, space.getUserA(), space.getUserB(), detail);
        return toReconcileVO(card, me);
    }

    // ========== F14 夸夸墙 ==========

    /** 贴一张夸夸卡上墙。 */
    public PraiseVO postPraise(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String text = requireText(content, "写下夸夸内容（1-200 字），要具体哦", CouplePraise.CONTENT_MAX);
        CouplePraise praise = CouplePraise.of(space.getId(), me, text);
        praiseMapper.insert(praise);
        push.pushCoupleEvent("praise-posted", me, space.partnerOf(me),
                "TA 在夸夸墙贴了一张夸你的小卡片 🌟 快去拆开看看！");
        return toPraiseVO(praise, me);
    }

    /** 夸夸墙（新→旧）。 */
    public List<PraiseVO> listPraises(String me) {
        CoupleSpace space = requireSpace(me);
        return praiseMapper.findBySpace(space.getId()).stream()
                .map(p -> toPraiseVO(p, me))
                .toList();
    }

    /** 收下夸夸卡：只有被夸的人能点「收到啦」。 */
    public PraiseVO receivePraise(String me, String praiseId) {
        CoupleSpace space = requireSpace(me);
        CouplePraise praise = praiseMapper.selectById(praiseId);
        if (praise == null || !praise.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这张夸夸卡不存在");
        }
        if (praise.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己写的夸夸卡不用自己签收哦");
        }
        if (CouplePraise.STATUS_RECEIVED.equals(praise.getStatus())) {
            throw new BusinessException(409, "这张卡已经签收过啦");
        }
        praise.setStatus(CouplePraise.STATUS_RECEIVED);
        praise.setReceivedAt(System.currentTimeMillis());
        praiseMapper.updateById(praise);
        push.pushCoupleEvent("praise-received", me, space.partnerOf(me), "TA 收到了你的夸夸卡 💌 开心！继续夸！");
        return toPraiseVO(praise, me);
    }

    // ========== F15 生理期关怀 ==========

    /** 生理期卡片：双方记录 + 预告 + 是否特殊时期。 */
    public CycleCardVO cycleCard(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleCycle mine = cycleMapper.find(space.getId(), me);
        CoupleCycle theirs = cycleMapper.find(space.getId(), space.partnerOf(me));
        return new CycleCardVO(toSideVO(mine), toSideVO(theirs));
    }

    /** 记录/修改我的生理期（更新后对方收到「温柔模式」提醒）。 */
    public CycleCardVO saveCycle(String me, String periodDay, Integer cycleDays, Integer periodDays, String note) {
        CoupleSpace space = requireSpace(me);
        String day = normalizeDay(periodDay, "生理期开始日期格式应为 yyyy-MM-dd");
        if (cycleDays != null && (cycleDays < 20 || cycleDays > 45)) {
            throw new BusinessException(400, "周期长度一般在 20-45 天之间");
        }
        if (periodDays != null && (periodDays < 1 || periodDays > 10)) {
            throw new BusinessException(400, "经期天数一般在 1-10 天之间");
        }
        String memo = requireOptional(note, "备注最多 100 字", 100);
        CoupleCycle row = cycleMapper.find(space.getId(), me);
        if (row == null) {
            row = CoupleCycle.of(space.getId(), me, day, cycleDays, periodDays, memo);
            cycleMapper.insert(row);
        } else {
            row.setPeriodDay(day);
            if (cycleDays != null) {
                row.setCycleDays(cycleDays);
            }
            if (periodDays != null) {
                row.setPeriodDays(periodDays);
            }
            row.setNote(memo);
            row.setUpdatedAt(System.currentTimeMillis());
            cycleMapper.updateById(row);
        }
        push.pushCoupleEvent("cycle-updated", me, space.partnerOf(me),
                "TA 更新了生理期记录 🌸 接下来几天请开启温柔模式：多关心、少讲理。");
        return cycleCard(me);
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    private CycleSideVO toSideVO(CoupleCycle row) {
        if (row == null) {
            return null;
        }
        LocalDate today = LocalDate.now();
        LocalDate next = row.nextStart(today);
        return new CycleSideVO(row.getUsername(), row.getPeriodDay(), row.getCycleDays(), row.getPeriodDays(),
                row.getNote() == null ? "" : row.getNote(),
                next == null ? null : next.toString(),
                next == null ? null : java.time.temporal.ChronoUnit.DAYS.between(today, next),
                row.inPeriod(today));
    }

    private ReconcileVO toReconcileVO(CoupleReconcile card, String me) {
        return new ReconcileVO(card.getId(), card.getFromUser(), card.getMessage(), card.getStartAt(),
                card.getStatus(), card.getAcceptedAt(), card.durationHours(), card.getFromUser().equals(me),
                card.getCreated());
    }

    private PraiseVO toPraiseVO(CouplePraise praise, String me) {
        return new PraiseVO(praise.getId(), praise.getFromUser(), praise.getContent(), praise.getStatus(),
                praise.getReceivedAt(), praise.getFromUser().equals(me), praise.getCreated());
    }

    private String requireText(String value, String message, int max) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.length() > max) {
            throw new BusinessException(400, message);
        }
        return text;
    }

    private String requireOptional(String value, String message, int max) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > max) {
            throw new BusinessException(400, message);
        }
        return text;
    }

    private String normalizeDay(String value, String message) {
        try {
            return LocalDate.parse(value.trim()).toString();
        } catch (Exception e) {
            throw new BusinessException(400, message);
        }
    }
}
