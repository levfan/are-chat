package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;

/**
 * 欢笑银行（批次三十五 F390-F399）：笑点存档、每日一逗、冷笑话结冰榜、尴尬回收站、快乐突袭、
 * 笑点默契考、大笑处方、幽默风格图鉴、欢乐周报、年度欢笑榜。
 * 情绪命题：幽默是关系的复利——笑要存，社死要埋，满一年自动转成好笑的事。
 * 口径：写接口一律返回整份 LaughVO（GET /bank 的形状）；判定类动作（证词/判分/盖章/中弹/服用）
 * 一律「只有对方能做、一人一次、重复幂等不重推」；F393 的转档与 F398/F399 的统计全部读时算，不建定时任务。
 */
@Service
public class CoupleLaughService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleLaughMomentMapper momentMapper;
    private final CoupleLaughDailyMapper dailyMapper;
    private final CoupleLaughJokeMapper jokeMapper;
    private final CoupleLaughCringeMapper cringeMapper;
    private final CoupleLaughAttackMapper attackMapper;
    private final CoupleLaughGuessMapper guessMapper;
    private final CoupleLaughRxMapper rxMapper;
    private final CoupleLaughStyleMapper styleMapper;
    private final ImPushService push;

    private static final int LIST_MOMENT = 20;
    private static final int LIST_DAILY = 14;
    private static final int LIST_JOKE = 20;
    private static final int LIST_CRINGE = 14;
    private static final int LIST_ATTACK = 14;
    private static final int LIST_RX = 10;

    public CoupleLaughService(CoupleSpaceMapper spaceMapper, CoupleLaughMomentMapper momentMapper,
                              CoupleLaughDailyMapper dailyMapper, CoupleLaughJokeMapper jokeMapper,
                              CoupleLaughCringeMapper cringeMapper, CoupleLaughAttackMapper attackMapper,
                              CoupleLaughGuessMapper guessMapper, CoupleLaughRxMapper rxMapper,
                              CoupleLaughStyleMapper styleMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.momentMapper = momentMapper;
        this.dailyMapper = dailyMapper;
        this.jokeMapper = jokeMapper;
        this.cringeMapper = cringeMapper;
        this.attackMapper = attackMapper;
        this.guessMapper = guessMapper;
        this.rxMapper = rxMapper;
        this.styleMapper = styleMapper;
        this.push = push;
    }

    // ========== VO ==========

    /** F390 一条笑点。 */
    public record MomentVO(String id, String day, boolean mine, String title, String culprit, String scene,
                           int funLevel, String witness, String witnessBy, boolean witnessed, boolean canWitness) {
    }

    /** F391 某一天的节目单。 */
    public record DailyVO(String id, String day, boolean mineOwner, String ownerUser, String content,
                          String verdict, String verdictLabel, boolean judged, boolean canServe, boolean canJudge) {
    }

    /** F392 一条冷笑话。 */
    public record JokeVO(String id, String day, boolean mine, String content, boolean frozen, boolean judged,
                         String judgedBy, boolean canJudge, boolean canGuess) {
    }

    /** F393 一条社死往事（满一年自动转好笑）。 */
    public record CringeVO(String id, String day, boolean mine, String content, boolean healed, String healedBy,
                           boolean turnedFunny, int daysOld, boolean canHeal) {
    }

    /** F394 一次快乐突袭。 */
    public record AttackVO(String id, String day, boolean mine, String kind, String kindLabel, String content,
                           boolean hit, String hitBy, boolean canHit) {
    }

    /** F395 对某条冷笑话的预判。 */
    public record GuessVO(String jokeId, boolean minePredicted, boolean partnerPredicted, boolean predictsLaugh,
                          boolean partnerPredictsLaugh, boolean twin, int predictCount) {
    }

    /** F396 一张大笑处方。 */
    public record RxVO(String id, String day, boolean mine, String targetKind, String targetLabel, String targetId,
                       String targetTitle, String note, boolean taken, String takenBy) {
    }

    /** F397 幽默风格一行（自评或互评）。 */
    public record StyleVO(String id, String aboutUser, String rater, boolean mine, boolean selfRated, String style,
                          String styleLabel, String note) {
    }

    /** F398 欢乐周报。 */
    public record WeekVO(String week, String fromDay, String toDay, int moments, int served, int happy, int fake,
                         int frozen, int hits, int guesses, String summary) {
    }

    /** F399 年度欢笑榜。 */
    public record YearVO(int year, int moments, int laughs, int dailyDone, int happy, int frozen, String kingOfCold,
                         int cringe, int cringeHealed, int turns, int attacks, int hits, int guessTwin, int rxTaken,
                         String bestLine, String title, String summary) {
    }

    /** 欢笑银行总览。 */
    public record LaughVO(String day, String week, DailyVO today, List<MomentVO> moments, List<JokeVO> jokes,
                          List<CringeVO> cringes, int turnedFunny, List<AttackVO> attacks, List<GuessVO> guesses,
                          List<RxVO> rxList, List<StyleVO> styles, String styleHint, String rotationHint,
                          int myFrozen, int partnerFrozen, WeekVO weekReport, YearVO year) {
    }

    // ========== 读 ==========

    /** 欢笑银行总览（GET /bank）。 */
    public LaughVO board(String me) {
        CoupleSpace space = requireSpace(me);
        return build(space, me, LocalDate.now());
    }

    /** F398 欢乐周报（GET /week，周一锚）。 */
    public WeekVO weekReport(String me) {
        CoupleSpace space = requireSpace(me);
        return weekOf(space, LocalDate.now());
    }

    /** F399 年度欢笑榜（GET /year?year=，缺省当年）。 */
    public YearVO yearReport(String me, String year) {
        CoupleSpace space = requireSpace(me);
        return yearOf(space, normalizeYear(year, LocalDate.now()), LocalDate.now());
    }

    // ========== F390 笑点存档 ==========

    /** F390 存一条笑到肚子疼的时刻（day ≤今天、title ≤30、scene ≤100、funLevel 1-5 钳制、每人每天 ≤3 条）。 */
    public LaughVO addMoment(String me, String day, String title, String culprit, String scene, Integer funLevel) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String d = day == null || day.isBlank() ? now.toString() : parseDay(day, "发生的日子").toString();
        if (d.compareTo(now.toString()) > 0) {
            throw new BusinessException(400, "笑点要是还没发生，就先别存 🤔");
        }
        String t = trim(title, "这条笑点叫什么，起个名");
        if (t.length() > CoupleLaughMoment.TITLE_MAX) {
            throw new BusinessException(400, "名字最多 " + CoupleLaughMoment.TITLE_MAX + " 字");
        }
        String c = limit(culprit, CoupleLaughMoment.CULPRIT_MAX, "谁干的");
        String s = limit(scene, CoupleLaughMoment.SCENE_MAX, "现场还原");
        int lv = funLevel == null ? 3 : Math.max(CoupleLaughMoment.LEVEL_MIN,
                Math.min(CoupleLaughMoment.LEVEL_MAX, funLevel));
        if (momentMapper.find(space.getId(), d, me, t) != null) {
            throw new BusinessException(400, "这条笑点已经存过了");
        }
        long sameDay = momentMapper.findByDay(space.getId(), d).stream().filter(m -> me.equals(m.getFromUser())).count();
        if (sameDay >= CoupleLaughMoment.PER_DAY_MAX) {
            throw new BusinessException(400, "一天最多存 " + CoupleLaughMoment.PER_DAY_MAX + " 条，笑也要节制 😂");
        }
        momentMapper.insert(CoupleLaughMoment.of(space.getId(), d, me, t, c, s, lv));
        push.pushCoupleEvent("laugh-moment", me, space.partnerOf(me), CoupleLaughBank.momentLine(t, lv));
        return build(space, me, now);
    }

    /** F390 对方补一份现场证词（只有对方能补，一条只补一次）。 */
    public LaughVO witnessMoment(String me, String id, String text) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleLaughMoment row = requireMoment(space, id);
        if (me.equals(row.getFromUser())) {
            throw new BusinessException(400, "证词要对方补——自己夸现场没意思 🎤");
        }
        String t = trim(text, "现场证词写一句");
        if (t.length() > CoupleLaughMoment.WITNESS_MAX) {
            throw new BusinessException(400, "证词最多 " + CoupleLaughMoment.WITNESS_MAX + " 字");
        }
        if (row.witnessed()) {
            throw new BusinessException(400, "这条已经有人补过证词了");
        }
        row.witness(me, t);
        momentMapper.updateById(row);
        push.pushCoupleEvent("laugh-witness", me, row.getFromUser(), CoupleLaughBank.witnessLine(nz(row.getTitle())));
        return build(space, me, now);
    }

    // ========== F391 每日一逗 ==========

    /** F391 上台交今天的节目（只有轮到的人能交，一天一格；没判分前可改写）。 */
    public LaughVO serveDaily(String me, String content) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String d = now.toString();
        String owner = onDuty(space, now);
        if (!me.equals(owner)) {
            throw new BusinessException(400, "今天轮不到你——" + owner + " 才是值班喜剧人 🎪");
        }
        String c = trim(content, "今天打算用什么逗，写出来");
        if (c.length() > CoupleLaughDaily.CONTENT_MAX) {
            throw new BusinessException(400, "节目内容最多 " + CoupleLaughDaily.CONTENT_MAX + " 字");
        }
        CoupleLaughDaily row = dailyMapper.findByDay(space.getId(), d);
        if (row == null) {
            dailyMapper.insert(CoupleLaughDaily.of(space.getId(), d, owner, c));
            push.pushCoupleEvent("laugh-daily", me, space.partnerOf(me), CoupleLaughBank.dailyServeLine(owner));
        } else {
            if (row.judged()) {
                throw new BusinessException(400, "今天已经判过分了，节目就定格在这了");
            }
            row.setContent(c);
            row.setUpdatedAt(System.currentTimeMillis());
            dailyMapper.updateById(row);
        }
        return build(space, me, now);
    }

    /** F391 对方判分（HAPPY/FLAT/FAKE；值班人不能自己判，一天只判一次）。 */
    public LaughVO judgeDaily(String me, String id, String verdict) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleLaughDaily row = requireDaily(space, id);
        if (me.equals(row.getOwnerUser())) {
            throw new BusinessException(400, "自己逗的不能自己判，让 TA 来 🏅");
        }
        String v = verdict == null ? "" : verdict.trim().toUpperCase(Locale.ROOT);
        if (!CoupleLaughDaily.VERDICTS.contains(v)) {
            throw new BusinessException(400, "只能判三种：真笑了 / 没笑 / 强撑的笑");
        }
        if (row.judged()) {
            return build(space, me, now);
        }
        row.judge(me, v);
        dailyMapper.updateById(row);
        push.pushCoupleEvent("laugh-daily-judge", me, row.getOwnerUser(),
                CoupleLaughBank.dailyJudgeLine(CoupleLaughBank.verdictLabel(v)));
        return build(space, me, now);
    }

    // ========== F392 冷笑话结冰榜 ==========

    /** F392 丢一条冷笑话（≤80 字，内容查重，每人每天 ≤3 条）。 */
    public LaughVO addJoke(String me, String content) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String c = trim(content, "冷笑话总得先有个冷句");
        if (c.length() > CoupleLaughJoke.CONTENT_MAX) {
            throw new BusinessException(400, "冷笑话最多 " + CoupleLaughJoke.CONTENT_MAX + " 字");
        }
        if (jokeMapper.find(space.getId(), me, c) != null) {
            throw new BusinessException(400, "这条已经丢过一次了，冷笑话不许重播");
        }
        long today = jokeMapper.findBySpace(space.getId()).stream()
                .filter(j -> me.equals(j.getFromUser()) && now.toString().equals(j.getDay())).count();
        if (today >= CoupleLaughJoke.PER_USER_DAY_MAX) {
            throw new BusinessException(400, "今天已经丢了 " + CoupleLaughJoke.PER_USER_DAY_MAX + " 条，够冷了 🧊");
        }
        jokeMapper.insert(CoupleLaughJoke.of(space.getId(), now.toString(), me, c));
        push.pushCoupleEvent("laugh-joke", me, space.partnerOf(me), CoupleLaughBank.jokeLine());
        return build(space, me, now);
    }

    /** F392 对方判结没结冰（一条只判一次，重复判 400）。 */
    public LaughVO judgeJoke(String me, String id, Boolean frozen) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleLaughJoke row = requireJoke(space, id);
        if (me.equals(row.getFromUser())) {
            throw new BusinessException(400, "自己讲的不能自己判冰 🧊");
        }
        if (row.judged()) {
            throw new BusinessException(400, "这条已经判过了，结冰榜不许翻案");
        }
        row.judge(me, frozen != null && frozen);
        jokeMapper.updateById(row);
        int tellerFrozen = (int) jokeMapper.findBySpace(space.getId()).stream()
                .filter(j -> row.getFromUser().equals(j.getFromUser()) && j.isFrozen()).count();
        push.pushCoupleEvent("laugh-frozen", me, row.getFromUser(),
                CoupleLaughBank.frozenLine(row.isFrozen(), tellerFrozen));
        return build(space, me, now);
    }

    // ========== F393 尴尬回收站 ==========

    /** F393 交一条社死往事（day ≤今天、≤100 字、每人每天一条）。 */
    public LaughVO addCringe(String me, String day, String content) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String d = day == null || day.isBlank() ? now.toString() : parseDay(day, "社死的日子").toString();
        if (d.compareTo(now.toString()) > 0) {
            throw new BusinessException(400, "社死是过去发生的事，不能预约");
        }
        String c = trim(content, "当时发生了什么，写下来");
        if (c.length() > CoupleLaughCringe.CONTENT_MAX) {
            throw new BusinessException(400, "社死现场最多 " + CoupleLaughCringe.CONTENT_MAX + " 字");
        }
        if (cringeMapper.find(space.getId(), d, me) != null) {
            throw new BusinessException(400, "那天已经交过一条了，一天一条 😖");
        }
        cringeMapper.insert(CoupleLaughCringe.of(space.getId(), d, me, c));
        push.pushCoupleEvent("laugh-cringe", me, space.partnerOf(me), CoupleLaughBank.cringeLine());
        return build(space, me, now);
    }

    /** F393 对方盖「抱抱你」章（只有对方能盖，重复盖幂等不重推）。 */
    public LaughVO healCringe(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleLaughCringe row = requireCringe(space, id);
        if (me.equals(row.getFromUser())) {
            throw new BusinessException(400, "抱抱章要 TA 盖，自己抱抱不算 🫂");
        }
        if (row.heal(me)) {
            cringeMapper.updateById(row);
            push.pushCoupleEvent("laugh-healed", me, row.getFromUser(), CoupleLaughBank.healedLine());
        }
        return build(space, me, now);
    }

    // ========== F394 快乐突袭 ==========

    /** F394 发动一次突袭（kind 白名单、≤100 字、每人每天一次）。 */
    public LaughVO addAttack(String me, String kind, String content) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String k = kind == null || kind.isBlank() ? CoupleLaughAttack.KIND_PRAISE : kind.trim().toUpperCase(Locale.ROOT);
        if (!CoupleLaughAttack.KINDS.contains(k)) {
            throw new BusinessException(400, "突袭只有三种：一串夸奖 / 一个梗 / 一段回忆杀 💥");
        }
        String c = trim(content, "突袭内容写一句");
        if (c.length() > CoupleLaughAttack.CONTENT_MAX) {
            throw new BusinessException(400, "突袭内容最多 " + CoupleLaughAttack.CONTENT_MAX + " 字");
        }
        String d = now.toString();
        if (attackMapper.find(space.getId(), me, d) != null) {
            throw new BusinessException(400, "今天已经突袭过一次了，明天再来 💥");
        }
        attackMapper.insert(CoupleLaughAttack.of(space.getId(), d, me, k, c));
        push.pushCoupleEvent("laugh-attack", me, space.partnerOf(me),
                CoupleLaughBank.attackLine(CoupleLaughBank.kindLabel(k), c));
        return build(space, me, now);
    }

    /** F394 对方「中弹」盖章（只有收方能盖，重复盖幂等）。 */
    public LaughVO hitAttack(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleLaughAttack row = requireAttack(space, id);
        if (me.equals(row.getFromUser())) {
            throw new BusinessException(400, "自己发的弹不能自己认 🎯");
        }
        if (row.markHit(me)) {
            attackMapper.updateById(row);
            push.pushCoupleEvent("laugh-hit", me, row.getFromUser(), CoupleLaughBank.hitLine(me));
        }
        return build(space, me, now);
    }

    // ========== F395 笑点默契考 ==========

    /** F395 预判对方会不会笑（一条梗每人一票，可改判到自己那一票）。 */
    public LaughVO guessJoke(String me, String jokeId, Boolean predict) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleLaughJoke joke = requireJoke(space, jokeId);
        int p = predict != null && predict ? 1 : 0;
        CoupleLaughGuess row = guessMapper.find(space.getId(), joke.getId(), me);
        if (row == null) {
            guessMapper.insert(CoupleLaughGuess.of(space.getId(), joke.getId(), me, p));
        } else {
            row.setPredict(p);
            row.setUpdatedAt(System.currentTimeMillis());
            guessMapper.updateById(row);
        }
        return build(space, me, now);
    }

    // ========== F396 大笑处方 ==========

    /** F396 开一张大笑处方（指向本空间的一条笑点/社死/突袭，每人每天一张）。 */
    public LaughVO addRx(String me, String targetKind, String targetId, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String k = targetKind == null ? "" : targetKind.trim().toUpperCase(Locale.ROOT);
        if (!CoupleLaughRx.TARGET_KINDS.contains(k)) {
            throw new BusinessException(400, "处方只能指向笑点 / 社死往事 / 快乐突袭 💊");
        }
        String title = requireTargetTitle(space, k, targetId);
        String d = now.toString();
        if (rxMapper.find(space.getId(), me, d) != null) {
            throw new BusinessException(400, "今天的处方已经开过了，明天再复诊");
        }
        String n = limit(note, CoupleLaughRx.NOTE_MAX, "医嘱");
        rxMapper.insert(CoupleLaughRx.of(space.getId(), d, me, k, targetId, n));
        push.pushCoupleEvent("laugh-rx", me, space.partnerOf(me),
                CoupleLaughBank.rxLine(CoupleLaughBank.targetLabel(k), n) + "（" + title + "）");
        return build(space, me, now);
    }

    /** F396 收方回执「已服用」（只有对方能回执，重复幂等）。 */
    public LaughVO takeRx(String me, String id) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        CoupleLaughRx row = requireRx(space, id);
        if (me.equals(row.getFromUser())) {
            throw new BusinessException(400, "药是给对方吃的，自己不能回执 ✅");
        }
        if (row.markTaken(me)) {
            rxMapper.updateById(row);
            push.pushCoupleEvent("laugh-rx-taken", me, row.getFromUser(), CoupleLaughBank.takenLine());
        }
        return build(space, me, now);
    }

    // ========== F397 幽默风格图鉴 ==========

    /** F397 记一份幽默风格（aboutUser 只能是两人之一，rater=自己；自评与互评各一行，可改写）。 */
    public LaughVO setStyle(String me, String aboutUser, String style, String note) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String about = aboutUser == null || aboutUser.isBlank() ? me : aboutUser.trim();
        if (!about.equals(space.getUserA()) && !about.equals(space.getUserB())) {
            throw new BusinessException(400, "只能给你们俩评幽默风格 🎭");
        }
        String s = style == null ? "" : style.trim().toUpperCase(Locale.ROOT);
        if (!CoupleLaughStyle.STYLES.contains(s)) {
            throw new BusinessException(400, "类型只有五种：谐音梗 / 冷幽默 / 自嘲派 / 动作派 / 模仿派");
        }
        String n = limit(note, CoupleLaughStyle.NOTE_MAX, "补一句");
        CoupleLaughStyle row = styleMapper.find(space.getId(), about, me);
        if (row == null) {
            styleMapper.insert(CoupleLaughStyle.of(space.getId(), about, me, s, n));
        } else {
            row.setStyle(s);
            row.setNote(n);
            row.setUpdatedAt(System.currentTimeMillis());
            styleMapper.updateById(row);
        }
        push.pushCoupleEvent("laugh-style", me, space.partnerOf(me),
                "🎭 幽默风格图鉴更新了一条：" + about + " 被评成「" + CoupleLaughBank.styleLabel(s) + "」。");
        return build(space, me, now);
    }

    // ========== 聚合 ==========

    private LaughVO build(CoupleSpace space, String me, LocalDate now) {
        String day = now.toString();
        String partner = space.partnerOf(me);

        CoupleLaughDaily todayRow = dailyMapper.findByDay(space.getId(), day);
        String onDuty = onDuty(space, now);
        DailyVO today = todayRow == null ? null : new DailyVO(todayRow.getId(), nz(todayRow.getDay()),
                me.equals(todayRow.getOwnerUser()), nz(todayRow.getOwnerUser()), nz(todayRow.getContent()),
                nz(todayRow.getVerdict()), CoupleLaughBank.verdictLabel(todayRow.getVerdict()), todayRow.judged(),
                me.equals(todayRow.getOwnerUser()), !me.equals(todayRow.getOwnerUser()) && !nz(todayRow.getContent()).isEmpty());
        String rotationHint = today == null ? "🎪 今天轮到 " + onDuty + " 上台逗" : "";

        List<MomentVO> moments = momentMapper.findBySpace(space.getId()).stream().limit(LIST_MOMENT)
                .map(m -> new MomentVO(m.getId(), nz(m.getDay()), me.equals(m.getFromUser()), nz(m.getTitle()),
                        nz(m.getCulprit()), nz(m.getScene()), m.getFunLevel() == null ? 3 : m.getFunLevel(),
                        nz(m.getWitness()), nz(m.getWitnessBy()), m.witnessed(),
                        !me.equals(m.getFromUser()) && !m.witnessed()))
                .toList();

        List<JokeVO> jokes = jokeMapper.findBySpace(space.getId()).stream().limit(LIST_JOKE)
                .map(j -> new JokeVO(j.getId(), nz(j.getDay()), me.equals(j.getFromUser()), nz(j.getContent()),
                        j.isFrozen(), j.judged(), nz(j.getJudgedBy()),
                        !me.equals(j.getFromUser()) && !j.judged(), !me.equals(j.getFromUser())))
                .toList();

        List<CoupleLaughCringe> cringeRows = cringeMapper.findBySpace(space.getId());
        List<CringeVO> cringes = cringeRows.stream().limit(LIST_CRINGE)
                .map(c -> new CringeVO(c.getId(), nz(c.getDay()), me.equals(c.getFromUser()), nz(c.getContent()),
                        c.healed(), nz(c.getHealedBy()), c.turnedFunny(day), (int) daysBetween(nz(c.getDay()), day),
                        !me.equals(c.getFromUser()) && !c.healed()))
                .toList();
        int turnedFunny = (int) cringeRows.stream().filter(c -> c.turnedFunny(day)).count();

        List<AttackVO> attacks = attackMapper.findBySpace(space.getId()).stream().limit(LIST_ATTACK)
                .map(a -> new AttackVO(a.getId(), nz(a.getDay()), me.equals(a.getFromUser()), nz(a.getKind()),
                        CoupleLaughBank.kindLabel(a.getKind()), nz(a.getContent()), a.hit(), nz(a.getHitBy()),
                        !me.equals(a.getFromUser()) && !a.hit()))
                .toList();

        List<CoupleLaughGuess> guesses = guessMapper.findBySpace(space.getId());
        List<GuessVO> guessVos = jokes.stream().map(j -> {
            List<CoupleLaughGuess> pair = guesses.stream().filter(g -> j.id().equals(g.getJokeId())).toList();
            CoupleLaughGuess mineRow = pair.stream().filter(g -> me.equals(g.getFromUser())).findFirst().orElse(null);
            CoupleLaughGuess otherRow = pair.stream().filter(g -> !me.equals(g.getFromUser())).findFirst().orElse(null);
            boolean twin = mineRow != null && otherRow != null
                    && mineRow.predictsLaugh() == otherRow.predictsLaugh();
            return new GuessVO(j.id(), mineRow != null, otherRow != null, mineRow != null && mineRow.predictsLaugh(),
                    otherRow != null && otherRow.predictsLaugh(), twin, pair.size());
        }).toList();

        List<RxVO> rxList = rxMapper.findBySpace(space.getId()).stream().limit(LIST_RX)
                .map(r -> new RxVO(r.getId(), nz(r.getDay()), me.equals(r.getFromUser()), nz(r.getTargetKind()),
                        CoupleLaughBank.targetLabel(r.getTargetKind()), nz(r.getTargetId()),
                        safeTargetTitle(space, r.getTargetKind(), r.getTargetId()), nz(r.getNote()), r.taken(),
                        nz(r.getTakenBy())))
                .toList();

        List<StyleVO> styles = styleMapper.findBySpace(space.getId()).stream()
                .map(s -> new StyleVO(s.getId(), nz(s.getAboutUser()), nz(s.getRater()), me.equals(s.getRater()),
                        s.selfRated(), nz(s.getStyle()), CoupleLaughBank.styleLabel(s.getStyle()), nz(s.getNote())))
                .toList();
        String mySelf = styles.stream().filter(s -> me.equals(s.aboutUser()) && me.equals(s.rater()))
                .map(StyleVO::style).findFirst().orElse("");
        String partnerSaysMe = styles.stream().filter(s -> me.equals(s.aboutUser()) && !me.equals(s.rater()))
                .map(StyleVO::style).findFirst().orElse("");
        String styleHint;
        if (mySelf.isEmpty() || partnerSaysMe.isEmpty()) {
            styleHint = CoupleLaughBank.styleMissingLine();
        } else if (mySelf.equals(partnerSaysMe)) {
            styleHint = CoupleLaughBank.styleSameLine(CoupleLaughBank.styleLabel(mySelf));
        } else {
            styleHint = CoupleLaughBank.styleDiffLine(CoupleLaughBank.styleLabel(mySelf),
                    CoupleLaughBank.styleLabel(partnerSaysMe));
        }

        List<CoupleLaughJoke> allJokes = jokeMapper.findBySpace(space.getId());
        int myFrozen = (int) allJokes.stream().filter(j -> j.isFrozen() && me.equals(j.getFromUser())).count();
        int partnerFrozen = (int) allJokes.stream().filter(j -> j.isFrozen() && !me.equals(j.getFromUser())).count();

        return new LaughVO(day, weekStart(now).toString(), today, moments, jokes, cringes, turnedFunny, attacks,
                guessVos, rxList, styles, styleHint, rotationHint, myFrozen, partnerFrozen,
                weekOf(space, now), yearOf(space, String.valueOf(now.getYear()), now));
    }

    /** F398 欢乐周报（周一锚）。 */
    private WeekVO weekOf(CoupleSpace space, LocalDate now) {
        LocalDate from = weekStart(now);
        LocalDate to = from.plusDays(6);
        String fromDay = from.toString();
        String toDay = to.toString();
        int moments = (int) momentMapper.findBySpace(space.getId()).stream()
                .filter(m -> inRange(m.getDay(), fromDay, toDay)).count();
        List<CoupleLaughDaily> dailies = dailyMapper.findBySpace(space.getId()).stream()
                .filter(d -> inRange(d.getDay(), fromDay, toDay) && !nz(d.getContent()).isEmpty()).toList();
        int happy = (int) dailies.stream().filter(d -> CoupleLaughDaily.VERDICT_HAPPY.equals(d.getVerdict())).count();
        int fake = (int) dailies.stream().filter(d -> CoupleLaughDaily.VERDICT_FAKE.equals(d.getVerdict())).count();
        int frozen = (int) jokeMapper.findBySpace(space.getId()).stream()
                .filter(j -> inRange(j.getDay(), fromDay, toDay) && j.isFrozen()).count();
        int hits = (int) attackMapper.findBySpace(space.getId()).stream()
                .filter(a -> inRange(a.getDay(), fromDay, toDay) && a.hit()).count();
        List<String> jokeIds = jokeMapper.findBySpace(space.getId()).stream()
                .filter(j -> inRange(j.getDay(), fromDay, toDay)).map(CoupleLaughJoke::getId).toList();
        List<CoupleLaughGuess> weekGuess = guessMapper.findBySpace(space.getId()).stream()
                .filter(g -> jokeIds.contains(g.getJokeId())).toList();
        int guesses = (int) jokeIds.stream().filter(jid -> {
            List<CoupleLaughGuess> pair = weekGuess.stream().filter(g -> jid.equals(g.getJokeId())).toList();
            return pair.size() >= 2 && pair.get(0).predictsLaugh() == pair.get(1).predictsLaugh();
        }).count();
        long seed = CoupleRitualBank.stableHash(space.getId() + "|laugh-week|" + fromDay);
        return new WeekVO(fromDay, fromDay, toDay, moments, dailies.size(), happy, fake, frozen, hits, guesses,
                CoupleLaughBank.weekSummary(fromDay, fromDay, toDay, moments, dailies.size(), happy, fake, frozen,
                        hits, guesses, seed));
    }

    /** F399 年度欢笑榜（数字全部直查原始表，不用已 limit 的列表回算）。 */
    private YearVO yearOf(CoupleSpace space, String y, LocalDate now) {
        int year = Integer.parseInt(y);
        String today = now.toString();
        List<CoupleLaughMoment> moments = momentMapper.findBySpace(space.getId()).stream()
                .filter(m -> yearOf(m.getDay()) == year).toList();
        int laughs = (int) moments.stream().filter(CoupleLaughMoment::witnessed).count();
        String bestLine = moments.stream()
                .max((a, b) -> Integer.compare(a.getFunLevel() == null ? 0 : a.getFunLevel(),
                        b.getFunLevel() == null ? 0 : b.getFunLevel()))
                .map(CoupleLaughMoment::getTitle).orElse("");
        List<CoupleLaughDaily> dailies = dailyMapper.findBySpace(space.getId()).stream()
                .filter(d -> yearOf(d.getDay()) == year && !nz(d.getContent()).isEmpty()).toList();
        int happy = (int) dailies.stream().filter(d -> CoupleLaughDaily.VERDICT_HAPPY.equals(d.getVerdict())).count();
        List<CoupleLaughJoke> jokes = jokeMapper.findBySpace(space.getId()).stream()
                .filter(j -> yearOf(j.getDay()) == year).toList();
        List<CoupleLaughJoke> frozen = jokes.stream().filter(CoupleLaughJoke::isFrozen).toList();
        String kingOfCold = frozen.stream()
                .collect(java.util.stream.Collectors.groupingBy(CoupleLaughJoke::getFromUser,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream().max(java.util.Map.Entry.comparingByValue())
                .map(java.util.Map.Entry::getKey).orElse("还没人");
        List<CoupleLaughCringe> cringes = cringeMapper.findBySpace(space.getId()).stream()
                .filter(c -> yearOf(c.getDay()) == year).toList();
        int cringeHealed = (int) cringes.stream().filter(CoupleLaughCringe::healed).count();
        int turns = (int) cringeMapper.findBySpace(space.getId()).stream()
                .filter(c -> yearOf(c.getDay()) == year && c.turnedFunny(today)).count();
        List<CoupleLaughAttack> attacks = attackMapper.findBySpace(space.getId()).stream()
                .filter(a -> yearOf(a.getDay()) == year).toList();
        int hits = (int) attacks.stream().filter(CoupleLaughAttack::hit).count();
        int guessTwin = (int) guessMapper.findBySpace(space.getId()).stream()
                .collect(java.util.stream.Collectors.groupingBy(CoupleLaughGuess::getJokeId, java.util.stream.Collectors.toList()))
                .values().stream()
                .filter(pair -> pair.size() >= 2 && pair.get(0).predictsLaugh() == pair.get(1).predictsLaugh())
                .count();
        int rxTaken = (int) rxMapper.findBySpace(space.getId()).stream()
                .filter(r -> yearOf(r.getDay()) == year && r.taken()).count();
        long seed = CoupleRitualBank.stableHash(space.getId() + "|laugh-year|" + y);
        String summary = CoupleLaughBank.yearSummary(y, moments.size(), laughs, dailies.size(), happy,
                frozen.size(), kingOfCold, cringes.size(), cringeHealed, turns, attacks.size(), hits, guessTwin,
                rxTaken, bestLine, seed);
        return new YearVO(year, moments.size(), laughs, dailies.size(), happy, frozen.size(), kingOfCold,
                cringes.size(), cringeHealed, turns, attacks.size(), hits, guessTwin, rxTaken, bestLine,
                CoupleLaughBank.yearTitle(moments.size(), hits, happy, frozen.size()), summary);
    }

    /** F391 周轮换：以本周一为锚，周序号奇偶决定 userA 还是 userB 上台。 */
    private String onDuty(CoupleSpace space, LocalDate now) {
        long weekIndex = weekStart(now).toEpochDay() / 7;
        return weekIndex % 2 == 0 ? space.getUserA() : space.getUserB();
    }

    private LocalDate weekStart(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** 处方指向的条目标题（用于展示与回执文案）。 */
    private String requireTargetTitle(CoupleSpace space, String kind, String targetId) {
        String title = safeTargetTitle(space, kind, targetId);
        if (title == null) {
            throw new BusinessException(400, "处方指向的那条已经不在这儿了 💊");
        }
        return title;
    }

    private String safeTargetTitle(CoupleSpace space, String kind, String targetId) {
        if (targetId == null || targetId.isBlank() || kind == null) {
            return null;
        }
        return switch (kind) {
            case CoupleLaughRx.TARGET_MOMENT -> {
                CoupleLaughMoment row = momentMapper.selectById(targetId);
                yield row == null || !space.getId().equals(row.getSpaceId()) ? null : nz(row.getTitle());
            }
            case CoupleLaughRx.TARGET_CRINGE -> {
                CoupleLaughCringe row = cringeMapper.selectById(targetId);
                yield row == null || !space.getId().equals(row.getSpaceId()) ? null : nz(row.getContent());
            }
            case CoupleLaughRx.TARGET_ATTACK -> {
                CoupleLaughAttack row = attackMapper.selectById(targetId);
                yield row == null || !space.getId().equals(row.getSpaceId()) ? null : nz(row.getContent());
            }
            default -> null;
        };
    }

    // ========== 取行与校验 ==========

    private CoupleLaughMoment requireMoment(CoupleSpace space, String id) {
        CoupleLaughMoment row = id == null || id.isBlank() ? null : momentMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这条笑点 😂");
        }
        return row;
    }

    private CoupleLaughDaily requireDaily(CoupleSpace space, String id) {
        CoupleLaughDaily row = id == null || id.isBlank() ? null : dailyMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到今天的节目单 🎪");
        }
        return row;
    }

    private CoupleLaughJoke requireJoke(CoupleSpace space, String id) {
        CoupleLaughJoke row = id == null || id.isBlank() ? null : jokeMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这条冷笑话 🧊");
        }
        return row;
    }

    private CoupleLaughCringe requireCringe(CoupleSpace space, String id) {
        CoupleLaughCringe row = id == null || id.isBlank() ? null : cringeMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这条社死往事 😖");
        }
        return row;
    }

    private CoupleLaughAttack requireAttack(CoupleSpace space, String id) {
        CoupleLaughAttack row = id == null || id.isBlank() ? null : attackMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这次突袭 💥");
        }
        return row;
    }

    private CoupleLaughRx requireRx(CoupleSpace space, String id) {
        CoupleLaughRx row = id == null || id.isBlank() ? null : rxMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这张处方 💊");
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

    private boolean inRange(String day, String fromDay, String toDay) {
        return day != null && day.compareTo(fromDay) >= 0 && day.compareTo(toDay) <= 0;
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

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
