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
    private final CoupleLaughJokeMapper jokeMapper;
    private final CoupleLaughCringeMapper cringeMapper;
    private final ImPushService push;

    private static final int LIST_MOMENT = 20;
    private static final int LIST_DAILY = 14;
    private static final int LIST_JOKE = 20;
    private static final int LIST_CRINGE = 14;
    private static final int LIST_ATTACK = 14;
    private static final int LIST_RX = 10;

    public CoupleLaughService(CoupleSpaceMapper spaceMapper, CoupleLaughMomentMapper momentMapper, CoupleLaughJokeMapper jokeMapper, CoupleLaughCringeMapper cringeMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.momentMapper = momentMapper;
        this.jokeMapper = jokeMapper;
        this.cringeMapper = cringeMapper;
        this.push = push;
    }

    // ========== VO ==========

    /** F390 一条笑点。 */
    public record MomentVO(String id, String day, boolean mine, String title, String culprit, String scene,
                           int funLevel, String witness, String witnessBy, boolean witnessed, boolean canWitness) {
    }

    /** F392 一条冷笑话。 */
    public record JokeVO(String id, String day, boolean mine, String content, boolean frozen, boolean judged,
                         String judgedBy, boolean canJudge, boolean canGuess) {
    }

    /** F393 一条社死往事（满一年自动转好笑）。 */
    public record CringeVO(String id, String day, boolean mine, String content, boolean healed, String healedBy,
                           boolean turnedFunny, int daysOld, boolean canHeal) {
    }

    /** 欢笑银行总览。 */
    public record LaughVO(String day, String week, List<MomentVO> moments, List<JokeVO> jokes,
                          List<CringeVO> cringes, int turnedFunny, int myFrozen, int partnerFrozen) {
    }

    // ========== 读 ==========

    /** 欢笑银行总览（GET /bank）。 */
    public LaughVO board(String me) {
        CoupleSpace space = requireSpace(me);
        return build(space, me, LocalDate.now());
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
        if (momentMapper.findByDay(space.getId(), d).stream()
                // 查重按大小写不敏感：uk(space,day,from,title) 里的 title 没写 COLLATE，
                // MariaDB 默认 *_ci 会把「Bo」和「bo」判为同一行，服务层必须和库同一口径，否则 insert 撞唯一键变 500
                .anyMatch(m -> me.equals(m.getFromUser()) && t.equalsIgnoreCase(nz(m.getTitle())))) {
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

    // ========== F392 冷笑话结冰榜 ==========

    /** F392 丢一条冷笑话（≤80 字，内容查重，每人每天 ≤3 条）。 */
    public LaughVO addJoke(String me, String content) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String c = trim(content, "冷笑话总得先有个冷句");
        if (c.length() > CoupleLaughJoke.CONTENT_MAX) {
            throw new BusinessException(400, "冷笑话最多 " + CoupleLaughJoke.CONTENT_MAX + " 字");
        }
        if (jokeMapper.findBySpace(space.getId()).stream()
                // 同 uk_laugh_joke 的 content 比较语义：与库一致按大小写不敏感查重，少一次查询也不多一条 500
                .anyMatch(j -> me.equals(j.getFromUser()) && c.equalsIgnoreCase(nz(j.getContent())))) {
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
                .filter(j -> row.getFromUser().equals(j.getFromUser()) && j.frozenFlag()).count();
        push.pushCoupleEvent("laugh-frozen", me, row.getFromUser(),
                CoupleLaughBank.frozenLine(row.frozenFlag(), tellerFrozen));
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

    // ========== F395 笑点默契考 ==========

    // ========== F396 大笑处方 ==========

    // ========== F397 幽默风格图鉴 ==========

    // ========== 聚合 ==========

    private LaughVO build(CoupleSpace space, String me, LocalDate now) {
        String day = now.toString();
        String partner = space.partnerOf(me);

        List<MomentVO> moments = momentMapper.findBySpace(space.getId()).stream().limit(LIST_MOMENT)
                .map(m -> new MomentVO(m.getId(), nz(m.getDay()), me.equals(m.getFromUser()), nz(m.getTitle()),
                        nz(m.getCulprit()), nz(m.getScene()), m.getFunLevel() == null ? 3 : m.getFunLevel(),
                        nz(m.getWitness()), nz(m.getWitnessBy()), m.witnessed(),
                        !me.equals(m.getFromUser()) && !m.witnessed()))
                .toList();

        List<JokeVO> jokes = jokeMapper.findBySpace(space.getId()).stream().limit(LIST_JOKE)
                .map(j -> new JokeVO(j.getId(), nz(j.getDay()), me.equals(j.getFromUser()), nz(j.getContent()),
                        j.frozenFlag(), j.judged(), nz(j.getJudgedBy()),
                        // canGuess 原先是「不是我的那条」——可 F395 要的是「同一梗两人各自预判对方笑不笑，双判一致=默契+1」，
                        // 只许对方投就永远只有一票，双人一致永远凑不出来。改成判冰之前两边都能投（投过就不再是盲猜）。
                        !me.equals(j.getFromUser()) && !j.judged(), !j.judged()))
                .toList();

        List<CoupleLaughCringe> cringeRows = cringeMapper.findBySpace(space.getId());
        List<CringeVO> cringes = cringeRows.stream().limit(LIST_CRINGE)
                .map(c -> new CringeVO(c.getId(), nz(c.getDay()), me.equals(c.getFromUser()), nz(c.getContent()),
                        c.healed(), nz(c.getHealedBy()), c.turnedFunny(day), (int) daysBetween(nz(c.getDay()), day),
                        !me.equals(c.getFromUser()) && !c.healed()))
                .toList();
        int turnedFunny = (int) cringeRows.stream().filter(c -> c.turnedFunny(day)).count();

        List<CoupleLaughJoke> allJokes = jokeMapper.findBySpace(space.getId());
        int myFrozen = (int) allJokes.stream().filter(j -> j.frozenFlag() && me.equals(j.getFromUser())).count();
        int partnerFrozen = (int) allJokes.stream().filter(j -> j.frozenFlag() && !me.equals(j.getFromUser())).count();

        return new LaughVO(day, weekStart(now).toString(), moments, jokes, cringes, turnedFunny,
                myFrozen, partnerFrozen);
    }

    private LocalDate weekStart(LocalDate day) {
        return day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    // ========== 取行与校验 ==========

    private CoupleLaughMoment requireMoment(CoupleSpace space, String id) {
        CoupleLaughMoment row = id == null || id.isBlank() ? null : momentMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "找不到这条笑点 😂");
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
