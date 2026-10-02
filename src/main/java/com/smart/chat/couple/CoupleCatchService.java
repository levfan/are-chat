package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/**
 * 聆听者（批次三十四 F380-F389）：暗中心愿本、雷区探测器、安全词、敏感日历、「说到哪了」、
 * 真话翻译机、聆听方式协议、话题许愿池、今日一句话、聆听者年报。
 * 情绪命题：爱是「你随口一说，我一直记得」——把捕捉、避雷、喊停、续话头这些动作记成账。
 * 口径：写接口一律返回整份 CatchVO（GET /board 的形状）；未揭晓的心愿对心愿主人不可见（保密靠读时过滤，不靠前端）。
 */
@Service
public class CoupleCatchService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleCatchWishMapper wishMapper;
    private final CoupleCatchMineMapper mineMapper;
    private final CoupleCatchSafewordMapper safewordMapper;
    private final CoupleCatchSafewordUseMapper useMapper;
    private final CoupleCatchSensitiveMapper sensitiveMapper;
    private final CoupleCatchThreadMapper threadMapper;
    private final CoupleCatchSayMapper sayMapper;
    private final CoupleCatchProtocolMapper protocolMapper;
    private final CoupleCatchTopicMapper topicMapper;
    private final CoupleCatchDailyMapper dailyMapper;
    private final ImPushService push;

    private static final int LIST_WISH = 24;
    private static final int LIST_MINE = 12;
    private static final int LIST_USE = 20;
    private static final int LIST_SENSITIVE = 12;
    private static final int LIST_THREAD = 12;
    private static final int LIST_SAY = 20;
    private static final int LIST_TOPIC = 12;
    private static final int LIST_DAILY = 14;

    public CoupleCatchService(CoupleSpaceMapper spaceMapper, CoupleCatchWishMapper wishMapper,
                              CoupleCatchMineMapper mineMapper, CoupleCatchSafewordMapper safewordMapper,
                              CoupleCatchSafewordUseMapper useMapper, CoupleCatchSensitiveMapper sensitiveMapper,
                              CoupleCatchThreadMapper threadMapper, CoupleCatchSayMapper sayMapper,
                              CoupleCatchProtocolMapper protocolMapper, CoupleCatchTopicMapper topicMapper,
                              CoupleCatchDailyMapper dailyMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.wishMapper = wishMapper;
        this.mineMapper = mineMapper;
        this.safewordMapper = safewordMapper;
        this.useMapper = useMapper;
        this.sensitiveMapper = sensitiveMapper;
        this.threadMapper = threadMapper;
        this.sayMapper = sayMapper;
        this.protocolMapper = protocolMapper;
        this.topicMapper = topicMapper;
        this.dailyMapper = dailyMapper;
        this.push = push;
    }

    // ========== VO ==========

    /** F380 心愿条目（secret=还没揭晓，只有记账人能看见）。 */
    public record WishVO(String id, boolean mine, String content, String sourceDay, String scene,
                         boolean secret, boolean filled, long created) {
    }

    /** F381 雷区卡。 */
    public record MineVO(String id, boolean mine, String topic, String trip, String safeWay,
                         boolean acked, String ackBy, int avoided) {
    }

    /** F382 某人的安全词 + 这一年/近期用过几次。 */
    public record SafewordVO(String id, boolean mine, String word, String note, int useCount) {
    }

    /** F382 一次暂停使用。 */
    public record UseVO(String id, String day, boolean mine, String word, String reflect) {
    }

    /** F383 敏感日标注。 */
    public record SensitiveVO(String id, boolean mineAsOwner, String ownerUser, String day, String kind,
                              String kindLabel, String care, int daysLeft, boolean remindTomorrow) {
    }

    /** F384 话头存档。 */
    public record ThreadVO(String id, boolean mine, String topic, String progress, boolean open, long created) {
    }

    /** F385 反话词条。 */
    public record SayVO(String id, boolean mine, String say, String means) {
    }

    /** F386 聆听方式协议。 */
    public record ProtocolVO(String id, boolean mine, String mode, String modeLabel, String note) {
    }

    /** F387 话题许愿。 */
    public record TopicVO(String id, boolean mine, String title, String status, String takenBy,
                          boolean canTake, boolean canTalk, boolean overdue, String talkDay, String reflect) {
    }

    /** F388 某一天的一句话。 */
    public record DailyVO(String day, String content, boolean mine) {
    }

    /** F389 聆听者年报（数字全部直查原始表）。 */
    public record YearVO(int year, int wishes, int fulfilled, int mines, int acked, int avoids, int uses,
                         int reflected, int sensitives, int threads, int finished, int says, int talked,
                         int onTime, int dailies, String title, String summary) {
    }

    /** 聆听者总览：写接口原样返回。 */
    public record CatchVO(String day, String week, List<WishVO> myWishes, List<WishVO> revealedToMe,
                          int wishQuotaLeft, List<MineVO> mines, SafewordVO myWord, SafewordVO partnerWord,
                          List<UseVO> uses, int monthUses, List<SensitiveVO> sensitives,
                          List<ThreadVO> myThreads, List<ThreadVO> partnerThreads,
                          List<SayVO> mySays, List<SayVO> partnerSays,
                          ProtocolVO myProtocol, ProtocolVO partnerProtocol, String protocolHint,
                          List<TopicVO> topics, DailyVO myToday, DailyVO partnerToday, String dailyHint,
                          List<DailyVO> myHistory, YearVO year) {
    }

    // ========== 读 ==========

    /** 聆听者总览（GET /board）。 */
    public CatchVO board(String me) {
        CoupleSpace space = requireSpace(me);
        return build(space, me, LocalDate.now());
    }

    /** F389 聆听者年报（GET /year?year=，缺省当年）。 */
    public YearVO yearReport(String me, String year) {
        CoupleSpace space = requireSpace(me);
        return yearOf(space, normalizeYear(year, LocalDate.now()));
    }

    // ========== F380 暗中心愿本 ==========

    /** F380 偷偷记下 TA 随口说的想要的（不推给任何人——保密靠这里就不推）。 */
    public CatchVO addWish(String me, String content, String sourceDay, String scene) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String c = trim(content, "TA 想要什么，先写一句");
        if (c.length() > CoupleCatchWish.CONTENT_MAX) {
            throw new BusinessException(400, "心愿最多 " + CoupleCatchWish.CONTENT_MAX + " 字");
        }
        String owner = space.partnerOf(me);
        String sd = sourceDay == null || sourceDay.isBlank() ? now.toString() : parseDay(sourceDay, "出处日子").toString();
        if (sd.compareTo(now.toString()) > 0) {
            throw new BusinessException(400, "出处日子不能是将来");
        }
        String sc = scene == null ? "" : scene.trim();
        if (sc.length() > CoupleCatchWish.SCENE_MAX) {
            throw new BusinessException(400, "场合最多 " + CoupleCatchWish.SCENE_MAX + " 字");
        }
        if (wishMapper.find(space.getId(), owner, c) != null) {
            throw new BusinessException(400, "这条心愿已经悄悄记过了 🤫");
        }
        if (secretWishes(space, owner).size() >= CoupleCatchWish.PER_OWNER_MAX) {
            throw new BusinessException(400, "TA 的心愿本最多藏 " + CoupleCatchWish.PER_OWNER_MAX + " 条，兑现一条就腾出一格");
        }
        wishMapper.insert(CoupleCatchWish.of(space.getId(), owner, me, c, sd, sc));
        return build(space, me, now);
    }

    /** F380 兑现登记（只有记账的人能勾，勾完立刻揭晓并推给心愿主人）。 */
    public CatchVO fulfillWish(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleCatchWish wish = requireWish(space, id);
        if (!me.equals(wish.getRecorderUser())) {
            throw new BusinessException(400, "这条是 " + nz(wish.getRecorderUser()) + " 悄悄记的，只有 TA 能勾兑现 🎁");
        }
        if (wish.filled()) {
            return build(space, me, now);
        }
        wish.fulfill();
        wishMapper.updateById(wish);
        push.pushCoupleEvent("catch-wish-fulfilled", me, wish.getOwnerUser(),
                CoupleCatchBank.wishFulfilledLine(nz(wish.getContent()), nz(wish.getSourceDay()), nz(wish.getScene())));
        return build(space, me, now);
    }

    // ========== F381 雷区探测器 ==========

    /** F381 挂一颗雷（topic ≤30、trip/safe 各 ≤60，每人 ≤6 颗）。 */
    public CatchVO addMine(String me, String topic, String trip, String safeWay) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String t = trim(topic, "哪件事一碰就吵，写个话题");
        if (t.length() > CoupleCatchMine.TOPIC_MAX) {
            throw new BusinessException(400, "话题最多 " + CoupleCatchMine.TOPIC_MAX + " 字");
        }
        String tr = limit(trip, CoupleCatchMine.TRIP_MAX, "雷点");
        String sw = limit(safeWay, CoupleCatchMine.SAFE_MAX, "安全说法");
        if (mineMapper.find(space.getId(), me, t) != null) {
            throw new BusinessException(400, "这颗雷已经挂过了");
        }
        if (mineMapper.findBySpace(space.getId()).stream().filter(m -> me.equals(m.getFromUser())).count()
                >= CoupleCatchMine.PER_USER_MAX) {
            throw new BusinessException(400, "一个人最多挂 " + CoupleCatchMine.PER_USER_MAX + " 颗雷，别把日子过成扫雷");
        }
        mineMapper.insert(CoupleCatchMine.of(space.getId(), me, t, tr, sw));
        push.pushCoupleEvent("catch-mine", me, space.partnerOf(me), CoupleCatchBank.minePlantedLine(t));
        return build(space, me, now);
    }

    /** F381 对方盖「已知晓」（自己不能给自己盖，重复盖幂等不重推）。 */
    public CatchVO ackMine(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleCatchMine mine = requireMine(space, id);
        if (me.equals(mine.getFromUser())) {
            throw new BusinessException(400, "这颗雷是你自己挂的，知晓章要 TA 来盖 ✅");
        }
        if (mine.ack(me)) {
            mineMapper.updateById(mine);
            push.pushCoupleEvent("catch-mine-ack", me, mine.getFromUser(),
                    CoupleCatchBank.mineAckLine(nz(mine.getTopic())));
        }
        return build(space, me, now);
    }

    /** F381 记一次成功避雷（先盖过知晓才允许记）。 */
    public CatchVO avoidMine(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleCatchMine mine = requireMine(space, id);
        if (me.equals(mine.getFromUser())) {
            throw new BusinessException(400, "避雷的人是 TA，你自己绕开不算战绩 🛡️");
        }
        if (!mine.acked()) {
            throw new BusinessException(400, "先盖「已知晓」，再记这次绕过去了");
        }
        mine.avoid();
        mineMapper.updateById(mine);
        push.pushCoupleEvent("catch-mine-avoid", me, mine.getFromUser(),
                CoupleCatchBank.mineAvoidLine(nz(mine.getTopic())));
        return build(space, me, now);
    }

    // ========== F382 安全词 ==========

    /** F382 约定/改写自己的安全词（每人一格，word ≤20、note ≤60）。 */
    public CatchVO setSafeword(String me, String word, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String w = trim(word, "暂停词总得有个词");
        if (w.length() > CoupleCatchSafeword.WORD_MAX) {
            throw new BusinessException(400, "安全词最多 " + CoupleCatchSafeword.WORD_MAX + " 字");
        }
        String n = limit(note, CoupleCatchSafeword.NOTE_MAX, "用了之后希望怎样");
        CoupleCatchSafeword row = safewordMapper.find(space.getId(), me);
        if (row == null) {
            safewordMapper.insert(CoupleCatchSafeword.of(space.getId(), me, w, n));
        } else {
            row.setWord(w);
            row.setNote(n);
            row.setUpdatedAt(System.currentTimeMillis());
            safewordMapper.updateById(row);
        }
        push.pushCoupleEvent("catch-safeword", me, space.partnerOf(me), CoupleCatchBank.safewordSetLine(w, n));
        return build(space, me, now);
    }

    /** F382 喊了一次暂停（一天一人只记一次，复盘可后补）。 */
    public CatchVO useSafeword(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleCatchSafeword mine = safewordMapper.find(space.getId(), me);
        if (mine == null) {
            throw new BusinessException(400, "先约一个安全词，才喊得出口 🛑");
        }
        String day = now.toString();
        if (useMapper.find(space.getId(), day, me) != null) {
            throw new BusinessException(400, "今天已经记过一次暂停了，别把安全词用成口头禅");
        }
        useMapper.insert(CoupleCatchSafewordUse.of(space.getId(), day, me));
        push.pushCoupleEvent("catch-safeword-use", me, space.partnerOf(me),
                CoupleCatchBank.safewordUseLine(nz(mine.getWord()), me));
        return build(space, me, now);
    }

    /** F382 事后补一句复盘（只有喊停本人能补自己那天的记录）。 */
    public CatchVO reflectUse(String me, String id, String reflect) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleCatchSafewordUse use = requireUse(space, id);
        if (!me.equals(use.getUserName())) {
            throw new BusinessException(400, "那次是 TA 喊的停，复盘要 TA 自己写 📝");
        }
        String r = trim(reflect, "复盘写一句：当时卡在哪、后来怎么接着聊的");
        if (r.length() > CoupleCatchSafewordUse.REFLECT_MAX) {
            throw new BusinessException(400, "复盘最多 " + CoupleCatchSafewordUse.REFLECT_MAX + " 字");
        }
        use.setReflect(r);
        use.setUpdatedAt(System.currentTimeMillis());
        useMapper.updateById(use);
        push.pushCoupleEventBoth("catch-safeword-reflect", me, space.getUserA(), space.getUserB(),
                CoupleCatchBank.safewordReflectLine(r));
        return build(space, me, now);
    }

    // ========== F383 敏感日历 ==========

    /** F383 给 TA 标一个敏感日（只能标将来或今天，kind 白名单，care ≤60，同同日同类型查重）。 */
    public CatchVO addSensitive(String me, String day, String kind, String care) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        LocalDate d = parseDay(day, "敏感日");
        if (d.isBefore(now)) {
            throw new BusinessException(400, "敏感日要提前标，过去的日子就让它过去 📌");
        }
        String k = kind == null || kind.isBlank() ? CoupleCatchSensitive.KIND_OTHER
                : kind.trim().toUpperCase(Locale.ROOT);
        if (!CoupleCatchSensitive.KINDS.contains(k)) {
            throw new BusinessException(400, "类型只能是周期第一天/考核日/忌日纪念日/其它");
        }
        String c = limit(care, CoupleCatchSensitive.CARE_MAX, "当天想被怎样对待");
        String owner = space.partnerOf(me);
        if (sensitiveMapper.find(space.getId(), owner, d.toString(), k) != null) {
            throw new BusinessException(400, "这一天的这一类已经标过了");
        }
        sensitiveMapper.insert(CoupleCatchSensitive.of(space.getId(), owner, d.toString(), k, c));
        push.pushCoupleEventBoth("catch-sensitive", me, space.getUserA(), space.getUserB(),
                CoupleCatchBank.sensitiveSetLine(d.toString(), CoupleCatchBank.kindLabel(k), c));
        return build(space, me, now);
    }

    /** F383 撤掉帮 TA 标的一天（只有代标的人能撤，敏感日的主人不能替 TA 撤）。 */
    public CatchVO removeSensitive(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleCatchSensitive row = requireSensitive(space, id);
        if (me.equals(row.getOwnerUser())) {
            throw new BusinessException(400, "这个敏感日是 TA 替你标的，要撤也让 TA 来撤 📌");
        }
        sensitiveMapper.deleteById(id);
        return build(space, me, now);
    }

    // ========== F384 「说到哪了」 ==========

    /** F384 存一个被打断的话头（topic ≤40、progress ≤60，在途每人 ≤5）。 */
    public CatchVO addThread(String me, String topic, String progress) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String t = trim(topic, "聊到哪儿的话题，写一句");
        if (t.length() > CoupleCatchThread.TOPIC_MAX) {
            throw new BusinessException(400, "话题最多 " + CoupleCatchThread.TOPIC_MAX + " 字");
        }
        String p = limit(progress, CoupleCatchThread.PROGRESS_MAX, "说到哪了");
        // 查重只在在途里比（(space,from_user,topic) 不是唯一键，续完后允许再存同一话题；
        // 也因此不能用 selectOne 查——同话题有过 DONE 行会抛 TooManyResults）
        List<CoupleCatchThread> inFlight = threadMapper.findByUserStatus(space.getId(), me, CoupleCatchThread.STATUS_OPEN);
        if (inFlight.stream().anyMatch(x -> t.equals(x.getTopic()))) {
            throw new BusinessException(400, "这个话题已经在线轴上了 🧵");
        }
        if (inFlight.size() >= CoupleCatchThread.IN_FLIGHT_MAX) {
            throw new BusinessException(400, "在途的话头最多 " + CoupleCatchThread.IN_FLIGHT_MAX + " 个，先聊完几个再存");
        }
        threadMapper.insert(CoupleCatchThread.of(space.getId(), me, t, p));
        push.pushCoupleEvent("catch-thread", me, space.partnerOf(me), CoupleCatchBank.threadSavedLine(t));
        return build(space, me, now);
    }

    /** F384 续完销档（只有存话头的人能销）。 */
    public CatchVO finishThread(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleCatchThread row = requireThread(space, id);
        if (!me.equals(row.getFromUser())) {
            throw new BusinessException(400, "这个话头是 TA 存的，让 TA 自己销档 ✂️");
        }
        if (!row.open()) {
            return build(space, me, now);
        }
        row.finish();
        threadMapper.updateById(row);
        push.pushCoupleEventBoth("catch-thread-done", me, space.getUserA(), space.getUserB(),
                CoupleCatchBank.threadDoneLine(nz(row.getTopic())));
        return build(space, me, now);
    }

    // ========== F385 真话翻译机 ==========

    /** F385 申报一条口是心非（say ≤20、means ≤60，每人 ≤10 条）。 */
    public CatchVO addSay(String me, String say, String means) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String s = trim(say, "嘴上常说的那句写下来");
        if (s.length() > CoupleCatchSay.SAY_MAX) {
            throw new BusinessException(400, "嘴上那句最多 " + CoupleCatchSay.SAY_MAX + " 字");
        }
        String m = limit(means, CoupleCatchSay.MEANS_MAX, "实际意思");
        if (m.isEmpty()) {
            throw new BusinessException(400, "翻译结果得写，不然对方猜不到 🔤");
        }
        if (sayMapper.find(space.getId(), me, s) != null) {
            throw new BusinessException(400, "这条已经申报过了");
        }
        if (sayMapper.findByUser(space.getId(), me).size() >= CoupleCatchSay.PER_USER_MAX) {
            throw new BusinessException(400, "反话词条最多 " + CoupleCatchSay.PER_USER_MAX + " 条，先删几条");
        }
        sayMapper.insert(CoupleCatchSay.of(space.getId(), me, s, m));
        push.pushCoupleEvent("catch-say", me, space.partnerOf(me), CoupleCatchBank.saySetLine(s, m));
        return build(space, me, now);
    }

    /** F385 删掉自己申报的词条（对方只能看，改不了也删不了）。 */
    public CatchVO removeSay(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleCatchSay row = requireSay(space, id);
        if (!me.equals(row.getFromUser())) {
            throw new BusinessException(400, "这份对照是 TA 本人申报的，对方不能改也不能删");
        }
        sayMapper.deleteById(id);
        return build(space, me, now);
    }

    // ========== F386 聆听方式协议 ==========

    /** F386 写/改「我难过时要的是」（五选一，note ≤60）。 */
    public CatchVO setProtocol(String me, String mode, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String m = mode == null ? "" : mode.trim().toUpperCase(Locale.ROOT);
        if (!CoupleCatchProtocol.MODES.contains(m)) {
            throw new BusinessException(400, "五种里选一个：讲道理/陪骂/抱抱不说话/递吃的/别理我 🎧");
        }
        String n = limit(note, CoupleCatchProtocol.NOTE_MAX, "补充说明");
        CoupleCatchProtocol row = protocolMapper.find(space.getId(), me);
        if (row == null) {
            protocolMapper.insert(CoupleCatchProtocol.of(space.getId(), me, m, n));
        } else {
            row.setMode(m);
            row.setNote(n);
            row.setUpdatedAt(System.currentTimeMillis());
            protocolMapper.updateById(row);
        }
        push.pushCoupleEvent("catch-protocol", me, space.partnerOf(me),
                CoupleCatchBank.protocolLine(CoupleCatchBank.modeLabel(m), n));
        return build(space, me, now);
    }

    // ========== F387 话题许愿池 ==========

    /** F387 许一个「希望我们多聊 XX」（title ≤30，同人不重复）。 */
    public CatchVO addTopic(String me, String title) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String t = trim(title, "想多聊的话题写一个");
        if (t.length() > CoupleCatchTopic.TITLE_MAX) {
            throw new BusinessException(400, "话题最多 " + CoupleCatchTopic.TITLE_MAX + " 字");
        }
        if (topicMapper.find(space.getId(), me, t) != null) {
            throw new BusinessException(400, "这个话题已经许过了 💭");
        }
        topicMapper.insert(CoupleCatchTopic.of(space.getId(), me, t));
        push.pushCoupleEvent("catch-topic", me, space.partnerOf(me), CoupleCatchBank.topicWishLine(t));
        return build(space, me, now);
    }

    /** F387 接单（只有非许愿人能接，接过的不能再接）。 */
    public CatchVO takeTopic(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleCatchTopic row = requireTopic(space, id);
        if (me.equals(row.getFromUser())) {
            throw new BusinessException(400, "自己许的题不能自己接 📥");
        }
        if (!row.pending()) {
            return build(space, me, now);
        }
        row.take(me);
        topicMapper.updateById(row);
        push.pushCoupleEvent("catch-topic-take", me, row.getFromUser(), CoupleCatchBank.topicTakeLine(nz(row.getTitle())));
        return build(space, me, now);
    }

    /** F387 聊完并留一句感想（只有接单的人能勾，超一周记一笔超时）。 */
    public CatchVO talkTopic(String me, String id, String reflect) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleCatchTopic row = requireTopic(space, id);
        if (!row.taken()) {
            throw new BusinessException(400, "这一题还没接单，聊不了");
        }
        if (!me.equals(row.getTakenBy())) {
            throw new BusinessException(400, "单是谁接的，谁来说「聊完了」");
        }
        String r = trim(reflect, "聊完了总得留一句感想");
        if (r.length() > CoupleCatchTopic.REFLECT_MAX) {
            throw new BusinessException(400, "感想最多 " + CoupleCatchTopic.REFLECT_MAX + " 字");
        }
        long takenAt = row.getTakenAt() == null ? System.currentTimeMillis() : row.getTakenAt();
        boolean overdue = System.currentTimeMillis() - takenAt > CoupleCatchTopic.WEEK_MILLIS;
        row.talkDone(now.toString(), r, overdue);
        topicMapper.updateById(row);
        push.pushCoupleEventBoth("catch-topic-talk", me, space.getUserA(), space.getUserB(),
                CoupleCatchBank.topicTalkLine(nz(row.getTitle()), overdue, r));
        return build(space, me, now);
    }

    // ========== F388 今日一句话 ==========

    /** F388 留今天想对 TA 说的一句（≤40 字，每人每天一句可改写）。 */
    public CatchVO daily(String me, String content) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String c = trim(content, "今天想说的那句写下来 💬");
        if (c.length() > CoupleCatchDaily.CONTENT_MAX) {
            throw new BusinessException(400, "今日一句话最多 " + CoupleCatchDaily.CONTENT_MAX + " 字");
        }
        String day = now.toString();
        boolean fresh = dailyMapper.find(space.getId(), day, me) == null;
        CoupleCatchDaily row = dailyMapper.find(space.getId(), day, me);
        if (row == null) {
            dailyMapper.insert(CoupleCatchDaily.of(space.getId(), day, me, c));
        } else {
            row.setContent(c);
            row.setUpdatedAt(System.currentTimeMillis());
            dailyMapper.updateById(row);
        }
        if (fresh) {
            push.pushCoupleEvent("catch-daily", me, space.partnerOf(me), CoupleCatchBank.dailyLine(c));
        }
        return build(space, me, now);
    }

    // ========== 聚合 ==========

    private CatchVO build(CoupleSpace space, String me, LocalDate now) {
        String day = now.toString();
        String week = now.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)).toString();

        List<WishVO> myWishes = wishMapper.findByOwner(space.getId(), space.partnerOf(me)).stream()
                .filter(w -> me.equals(w.getRecorderUser()) && w.secret())
                .limit(LIST_WISH)
                .map(w -> toWish(w, me))
                .toList();
        List<WishVO> revealedToMe = wishMapper.findByOwner(space.getId(), me).stream()
                .filter(w -> !w.secret())
                .limit(LIST_WISH)
                .map(w -> toWish(w, me))
                .toList();
        int wishQuotaLeft = Math.max(0, CoupleCatchWish.PER_OWNER_MAX - secretWishes(space, space.partnerOf(me)).size());

        List<MineVO> mines = mineMapper.findBySpace(space.getId()).stream()
                .limit(LIST_MINE)
                .map(m -> new MineVO(m.getId(), me.equals(m.getFromUser()), nz(m.getTopic()), nz(m.getTrip()),
                        nz(m.getSafeWay()), m.acked(), nz(m.getAckBy()), m.getAvoided() == null ? 0 : m.getAvoided()))
                .toList();

        List<CoupleCatchSafeword> words = safewordMapper.findBySpace(space.getId());
        CoupleCatchSafeword myRow = words.stream().filter(w -> me.equals(w.getFromUser())).findFirst().orElse(null);
        CoupleCatchSafeword partnerRow = words.stream().filter(w -> !me.equals(w.getFromUser())).findFirst().orElse(null);
        List<CoupleCatchSafewordUse> useRows = useMapper.findBySpace(space.getId());
        SafewordVO myWord = myRow == null ? null : new SafewordVO(myRow.getId(), true, nz(myRow.getWord()),
                nz(myRow.getNote()), countBy(useRows, myRow.getFromUser()));
        SafewordVO partnerWord = partnerRow == null ? null : new SafewordVO(partnerRow.getId(), false,
                nz(partnerRow.getWord()), nz(partnerRow.getNote()), countBy(useRows, partnerRow.getFromUser()));
        String myWordText = myRow == null ? "" : nz(myRow.getWord());
        String partnerWordText = partnerRow == null ? "" : nz(partnerRow.getWord());
        List<UseVO> uses = useRows.stream().limit(LIST_USE)
                .map(u -> new UseVO(u.getId(), nz(u.getDay()), me.equals(u.getUserName()),
                        me.equals(u.getUserName()) ? myWordText : partnerWordText, nz(u.getReflect())))
                .toList();
        int monthUses = (int) useRows.stream()
                .filter(u -> nz(u.getDay()).startsWith(day.substring(0, 7))).count();

        List<SensitiveVO> sensitives = sensitiveMapper.findBySpace(space.getId()).stream()
                .filter(s -> nz(s.getDay()).compareTo(day) >= 0)
                .limit(LIST_SENSITIVE)
                .map(s -> {
                    int left = (int) daysBetween(day, nz(s.getDay()));
                    return new SensitiveVO(s.getId(), me.equals(s.getOwnerUser()), nz(s.getOwnerUser()),
                            nz(s.getDay()), nz(s.getKind()), CoupleCatchBank.kindLabel(s.getKind()), nz(s.getCare()),
                            left, left == 1);
                })
                .toList();

        List<ThreadVO> myThreads = threadMapper.findByUserStatus(space.getId(), me, CoupleCatchThread.STATUS_OPEN)
                .stream().limit(LIST_THREAD).map(t -> toThread(t, me)).toList();
        List<ThreadVO> partnerThreads =
                threadMapper.findByUserStatus(space.getId(), space.partnerOf(me), CoupleCatchThread.STATUS_OPEN)
                        .stream().limit(LIST_THREAD).map(t -> toThread(t, me)).toList();

        List<SayVO> mySays = sayMapper.findByUser(space.getId(), me).stream().limit(LIST_SAY)
                .map(s -> new SayVO(s.getId(), true, nz(s.getSay()), nz(s.getMeans()))).toList();
        List<SayVO> partnerSays = sayMapper.findByUser(space.getId(), space.partnerOf(me)).stream().limit(LIST_SAY)
                .map(s -> new SayVO(s.getId(), false, nz(s.getSay()), nz(s.getMeans()))).toList();

        List<CoupleCatchProtocol> protocols = protocolMapper.findBySpace(space.getId());
        CoupleCatchProtocol myP = protocols.stream().filter(p -> me.equals(p.getFromUser())).findFirst().orElse(null);
        CoupleCatchProtocol partnerP = protocols.stream().filter(p -> !me.equals(p.getFromUser())).findFirst()
                .orElse(null);
        ProtocolVO myProtocol = myP == null ? null : new ProtocolVO(myP.getId(), true, nz(myP.getMode()),
                CoupleCatchBank.modeLabel(myP.getMode()), nz(myP.getNote()));
        ProtocolVO partnerProtocol = partnerP == null ? null : new ProtocolVO(partnerP.getId(), false,
                nz(partnerP.getMode()), CoupleCatchBank.modeLabel(partnerP.getMode()), nz(partnerP.getNote()));
        String protocolHint;
        if (myProtocol == null || partnerProtocol == null) {
            protocolHint = "🎧 两个人都写完说明书，下次安慰才有依据——还差" + (myProtocol == null ? "你" : "TA");
        } else if (!myProtocol.mode().equals(partnerProtocol.mode())) {
            protocolHint = CoupleCatchBank.protocolClashLine(myProtocol.modeLabel(), partnerProtocol.modeLabel());
        } else {
            protocolHint = CoupleCatchBank.protocolBothLine();
        }

        List<TopicVO> topics = topicMapper.findBySpace(space.getId()).stream().limit(LIST_TOPIC)
                .map(t -> new TopicVO(t.getId(), me.equals(t.getFromUser()), nz(t.getTitle()), nz(t.getStatus()),
                        nz(t.getTakenBy()), t.pending() && !me.equals(t.getFromUser()),
                        me.equals(t.getTakenBy()) && t.taken(),
                        t.getOverdue() != null && t.getOverdue() == 1, nz(t.getTalkDay()), nz(t.getReflect())))
                .toList();

        CoupleCatchDaily myDaily = dailyMapper.find(space.getId(), day, me);
        CoupleCatchDaily partnerDaily = dailyMapper.find(space.getId(), day, space.partnerOf(me));
        DailyVO myToday = myDaily == null ? null : new DailyVO(day, nz(myDaily.getContent()), true);
        DailyVO partnerToday = partnerDaily == null ? null : new DailyVO(day, nz(partnerDaily.getContent()), false);
        String dailyHint = "";
        if (partnerToday == null) {
            CoupleCatchDaily lastPartner = latestDaily(space.getId(), space.partnerOf(me), day);
            if (lastPartner != null) {
                dailyHint = CoupleCatchBank.dailyFallbackLine(nz(lastPartner.getDay()), nz(lastPartner.getContent()));
            }
        }
        List<DailyVO> myHistory = dailyMapper.findBySpace(space.getId()).stream()
                .filter(d -> me.equals(d.getUserName()))
                .limit(LIST_DAILY)
                .map(d -> new DailyVO(nz(d.getDay()), nz(d.getContent()), true))
                .toList();

        return new CatchVO(day, week, myWishes, revealedToMe, wishQuotaLeft, mines, myWord, partnerWord, uses,
                monthUses, sensitives, myThreads, partnerThreads, mySays, partnerSays, myProtocol, partnerProtocol,
                protocolHint, topics, myToday, partnerToday, dailyHint, myHistory, yearOf(space, String.valueOf(now.getYear())));
    }

    private WishVO toWish(CoupleCatchWish w, String me) {
        return new WishVO(w.getId(), me.equals(w.getRecorderUser()), nz(w.getContent()), nz(w.getSourceDay()),
                nz(w.getScene()), w.secret(), w.filled(), w.getCreated() == null ? 0 : w.getCreated());
    }

    private ThreadVO toThread(CoupleCatchThread t, String me) {
        return new ThreadVO(t.getId(), me.equals(t.getFromUser()), nz(t.getTopic()), nz(t.getProgress()), t.open(),
                t.getCreated() == null ? 0 : t.getCreated());
    }

    private CoupleCatchDaily latestDaily(String spaceId, String user, String beforeDay) {
        return dailyMapper.findBySpace(spaceId).stream()
                .filter(d -> user.equals(d.getUserName()) && nz(d.getDay()).compareTo(beforeDay) < 0)
                .findFirst()
                .orElse(null);
    }

    private int countBy(List<CoupleCatchSafewordUse> rows, String user) {
        return (int) rows.stream().filter(u -> user != null && user.equals(u.getUserName())).count();
    }

    /** F389 年报：数字全部直查原始表，不用已 limit 的列表回算。 */
    private YearVO yearOf(CoupleSpace space, String y) {
        int year = Integer.parseInt(y);
        List<CoupleCatchWish> wishes = wishMapper.findBySpace(space.getId());
        int fulfilled = (int) wishes.stream()
                .filter(w -> w.getRevealedAt() != null && yearOfMillis(w.getRevealedAt()) == year).count();
        List<CoupleCatchMine> mines = mineMapper.findBySpace(space.getId());
        int acked = (int) mines.stream().filter(CoupleCatchMine::acked).count();
        int avoids = mines.stream().mapToInt(m -> m.getAvoided() == null ? 0 : m.getAvoided()).sum();
        List<CoupleCatchSafewordUse> uses = useMapper.findBySpace(space.getId()).stream()
                .filter(u -> yearOf(u.getDay()) == year).toList();
        int reflected = (int) uses.stream().filter(CoupleCatchSafewordUse::reflected).count();
        int sensitives = (int) sensitiveMapper.findBySpace(space.getId()).stream()
                .filter(s -> yearOf(s.getDay()) == year).count();
        List<CoupleCatchThread> threads = threadMapper.findBySpace(space.getId()).stream()
                .filter(t -> t.getCreated() != null && yearOfMillis(t.getCreated()) == year).toList();
        int finished = (int) threads.stream()
                .filter(t -> t.getDoneAt() != null && yearOfMillis(t.getDoneAt()) == year).count();
        int says = (int) sayMapper.findBySpace(space.getId()).stream()
                .filter(s -> s.getCreated() != null && yearOfMillis(s.getCreated()) == year).count();
        List<CoupleCatchTopic> talkedList = topicMapper.findBySpace(space.getId()).stream()
                .filter(t -> t.talked() && yearOf(t.getTalkDay()) == year).toList();
        int talked = talkedList.size();
        int onTime = (int) talkedList.stream().filter(t -> t.getOverdue() == null || t.getOverdue() == 0).count();
        int dailies = (int) dailyMapper.findBySpace(space.getId()).stream()
                .filter(d -> yearOf(d.getDay()) == year).count();
        long seed = CoupleRitualBank.stableHash(space.getId() + "|catch-year|" + y);
        String summary = CoupleCatchBank.yearSummary(y, wishes.size(), fulfilled, mines.size(), acked, avoids,
                uses.size(), reflected, sensitives, threads.size(), finished, says, talked, onTime, dailies, seed);
        return new YearVO(year, wishes.size(), fulfilled, mines.size(), acked, avoids, uses.size(), reflected,
                sensitives, threads.size(), finished, says, talked, onTime, dailies,
                CoupleCatchBank.yearTitle(fulfilled, avoids, uses.size(), talked, dailies), summary);
    }

    // ========== 取行与校验 ==========

    private CoupleCatchWish requireWish(CoupleSpace space, String id) {
        CoupleCatchWish row = id == null || id.isBlank() ? null : wishMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这条心愿 🤫");
        }
        return row;
    }

    private CoupleCatchMine requireMine(CoupleSpace space, String id) {
        CoupleCatchMine row = id == null || id.isBlank() ? null : mineMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这颗雷 💣");
        }
        return row;
    }

    private CoupleCatchSafewordUse requireUse(CoupleSpace space, String id) {
        CoupleCatchSafewordUse row = id == null || id.isBlank() ? null : useMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到那次暂停记录 🛑");
        }
        return row;
    }

    private CoupleCatchSensitive requireSensitive(CoupleSpace space, String id) {
        CoupleCatchSensitive row = id == null || id.isBlank() ? null : sensitiveMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这个敏感日 📌");
        }
        return row;
    }

    private CoupleCatchThread requireThread(CoupleSpace space, String id) {
        CoupleCatchThread row = id == null || id.isBlank() ? null : threadMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到那个话头 🧵");
        }
        return row;
    }

    private CoupleCatchSay requireSay(CoupleSpace space, String id) {
        CoupleCatchSay row = id == null || id.isBlank() ? null : sayMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这条对照词条 🔤");
        }
        return row;
    }

    private CoupleCatchTopic requireTopic(CoupleSpace space, String id) {
        CoupleCatchTopic row = id == null || id.isBlank() ? null : topicMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这一题 💭");
        }
        return row;
    }

    private LocalDate parseDay(String day, String what) {
        String d = day == null ? "" : day.trim();
        if (!d.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BusinessException(400, what + "写成 yyyy-MM-dd");
        }
        try {
            return LocalDate.parse(d);
        } catch (RuntimeException ex) {
            throw new BusinessException(400, what + "写成 yyyy-MM-dd");
        }
    }

    private String normalizeYear(String year, LocalDate now) {
        String y = year == null || year.isBlank() ? String.valueOf(now.getYear()) : year.trim();
        if (!y.matches("\\d{4}")) {
            throw new BusinessException(400, "年份写成 yyyy");
        }
        return y;
    }

    private String limit(String text, int max, String what) {
        String t = text == null ? "" : text.trim();
        if (t.length() > max) {
            throw new BusinessException(400, what + "最多 " + max + " 字");
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

    private long daysBetween(String fromDay, String toDay) {
        try {
            return LocalDate.parse(toDay).toEpochDay() - LocalDate.parse(fromDay).toEpochDay();
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private int yearOf(String day) {
        if (day == null || day.length() < 4) {
            return 0;
        }
        try {
            return Integer.parseInt(day.substring(0, 4));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private int yearOfMillis(Long millis) {
        if (millis == null) {
            return 0;
        }
        return LocalDate.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).getYear();
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    /**
     * 还在藏着的心愿（revealedAt 为 null）——容量按这个数算：兑现即揭晓就不再占格。
     * 原先按全部行数算，于是 400 叫用户「先兑现几条」而兑现根本腾不出格子，建议本身无效。
     */
    private List<CoupleCatchWish> secretWishes(CoupleSpace space, String owner) {
        return wishMapper.findByOwner(space.getId(), owner).stream()
                .filter(CoupleCatchWish::secret)
                .toList();
    }
}
