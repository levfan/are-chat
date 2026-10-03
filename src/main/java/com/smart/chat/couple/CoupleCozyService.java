package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 体温同步·作息与健康（F220-F229，批次十八）：晚安同熄灯、睡眠报告单、数羊房、喝水接力、
 * 冷暖互报、熬夜守护、周末慢生活、疼痛对策本、抱抱计量器、月度安眠小结。
 * 情绪价值设计：把「你睡了吗/冷不冷/喝水没」这些说不出口的惦记变成低门槛的小动作——
 * 一次点亮、一杯水、一下按键，都是「我在照顾我们的作息」。
 */
@Service
public class CoupleCozyService {

    static final int SHEEP_TARGET = 10;
    static final long SHEEP_WINDOW_MS = 60_000L;
    static final long WATER_NUDGE_MS = 3 * 60 * 60 * 1000L;
    static final int LIGHTOUT_MILESTONE = 7;
    static final int[] HUG_MILESTONES = {10, 50, 100, 520, 1000};

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleCozyLightoutMapper lightoutMapper;
    private final CoupleCozySleepMapper sleepMapper;
    private final CoupleCozySheepMapper sheepMapper;
    private final CoupleCozyWaterMapper waterMapper;
    private final CoupleCozyWeatherMapper weatherMapper;
    private final CoupleCozyLatenightMapper latenightMapper;
    private final CoupleCozySlowMapper slowMapper;
    private final CoupleCozyRemedyMapper remedyMapper;
    private final CoupleCozyHugMapper hugMapper;
    private final ImPushService push;

    public CoupleCozyService(CoupleSpaceMapper spaceMapper, CoupleCozyLightoutMapper lightoutMapper,
                             CoupleCozySleepMapper sleepMapper, CoupleCozySheepMapper sheepMapper,
                             CoupleCozyWaterMapper waterMapper, CoupleCozyWeatherMapper weatherMapper,
                             CoupleCozyLatenightMapper latenightMapper, CoupleCozySlowMapper slowMapper,
                             CoupleCozyRemedyMapper remedyMapper, CoupleCozyHugMapper hugMapper,
                             ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.lightoutMapper = lightoutMapper;
        this.sleepMapper = sleepMapper;
        this.sheepMapper = sheepMapper;
        this.waterMapper = waterMapper;
        this.weatherMapper = weatherMapper;
        this.latenightMapper = latenightMapper;
        this.slowMapper = slowMapper;
        this.remedyMapper = remedyMapper;
        this.hugMapper = hugMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record LightoutVO(boolean mine, boolean partner, int streak) {
    }

    public record SleepVO(String fromUser, boolean mine, Integer stars, String dream) {
    }

    public record SheepVO(Integer mineTaps, Integer partnerTaps, boolean mineDone, boolean partnerDone,
                          Integer mineElapsedMs, Integer partnerElapsedMs) {
    }

    public record WaterVO(Integer mine, Integer partner, boolean nudge) {
    }

    public record WeatherVO(String fromUser, boolean mine, String city, String feel, String tempText, boolean advised) {
    }

    public record LatenightVO(boolean sentToday, String card) {
    }

    public record SlowVO(String fromUser, boolean mine, String thing, String doneDay) {
    }

    public record RemedyVO(String forUser, boolean mine, String body, Long updatedAt) {
    }

    public record HugVO(Integer total, Integer today, Integer milestone) {
    }

    public record TodayVO(String day, LightoutVO lightout, List<SleepVO> sleeps, SheepVO sheep, WaterVO water,
                          List<WeatherVO> weathers, LatenightVO latenight, List<SlowVO> slows,
                          List<RemedyVO> remedies, HugVO hug) {
    }

    // ========== 读：今日体温总览 ==========

    /** 今日体温同步总览（所有卡片一次拉齐）。 */
    public TodayVO today(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        return new TodayVO(day, lightoutState(space, me, day), sleepsOf(space, me, day), sheepState(space, me, day),
                waterState(space, me, day), weathersOf(space, me, day),
                new LatenightVO(latenightMapper.find(space.getId(), day, me) != null,
                        CoupleCozyBank.latenightCard(space.getId(), day, me)),
                slowsOf(space, me), remediesOf(space, me), hugState(space, day));
    }

    // ========== F220 晚安同熄灯 ==========

    /** 发晚安点灯；双方当晚都点=当日熄灯，连击满 7 天推 both 里程碑。 */
    public TodayVO lightout(String me, String atTime) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        if (lightoutMapper.find(space.getId(), day, me) == null) {
            lightoutMapper.insert(CoupleCozyLightout.of(space.getId(), day, me, atTime));
            push.pushCoupleEvent("cozy-lightout", me, space.partnerOf(me),
                    "🌙 TA 已经道过晚安熄灯了，就等你啦。");
        }
        LightoutVO st = lightoutState(space, me, day);
        if (st.streak() == LIGHTOUT_MILESTONE) {
            push.pushCoupleEventBoth("cozy-lightout-week", me, space.getUserA(), space.getUserB(),
                    "🏆 我们已连续 " + LIGHTOUT_MILESTONE + " 晚一起熄灯，作息同频达成！");
        }
        return today(me);
    }

    private LightoutVO lightoutState(CoupleSpace space, String me, String today) {
        boolean mine = lightoutMapper.find(space.getId(), today, me) != null;
        boolean partner = lightoutMapper.find(space.getId(), today, space.partnerOf(me)) != null;
        int streak = 0;
        LocalDate d = LocalDate.parse(today);
        while (streak < 60) {
            String day = d.minusDays(streak).toString();
            List<CoupleCozyLightout> rows = lightoutMapper.findByDay(space.getId(), day);
            boolean both = !rows.isEmpty() && rows.size() >= 2
                    && rows.stream().anyMatch(r -> r.getFromUser().equals(space.getUserA()))
                    && rows.stream().anyMatch(r -> r.getFromUser().equals(space.getUserB()));
            if (!both) {
                break;
            }
            streak++;
        }
        return new LightoutVO(mine, partner, streak);
    }

    // ========== F221 睡眠报告单 ==========

    /** 晨间报昨夜：睡龄 1-5 钳制 + 一句梦话，本人当日可改。 */
    public TodayVO reportSleep(String me, String day, Integer stars, String dream) {
        CoupleSpace space = requireSpace(me);
        String d = normalizeDay(day, "那一晚要是 2026-10-01 这样的日期哦");
        int s = stars == null ? 5 : Math.min(5, Math.max(1, stars));
        String dm = trimLimit(dream, 70, "梦话 70 字以内哦");
        CoupleCozySleep row = sleepMapper.find(space.getId(), d, me);
        if (row == null) {
            sleepMapper.insert(CoupleCozySleep.of(space.getId(), d, me, s, dm == null ? "" : dm));
            push.pushCoupleEvent("cozy-sleep", me, space.partnerOf(me),
                    "🛏️ TA 报了昨晚的睡眠单：" + s + " 星" + (dm == null ? "" : "，梦话是「" + dm + "」") + "。");
        } else {
            row.setStars(s);
            row.setDream(dm == null ? "" : dm);
            row.setUpdatedAt(System.currentTimeMillis());
            sleepMapper.updateById(row);
        }
        return today(me);
    }

    private List<SleepVO> sleepsOf(CoupleSpace space, String me, String day) {
        List<SleepVO> out = new ArrayList<>();
        for (String u : List.of(me, space.partnerOf(me))) {
            CoupleCozySleep r = sleepMapper.find(space.getId(), day, u);
            if (r != null) {
                out.add(new SleepVO(r.getFromUser(), me.equals(r.getFromUser()), r.getStars(), r.getDream()));
            }
        }
        return out;
    }

    // ========== F222 数羊房 ==========

    /** 数一下：60s 窗口内累计，满 10 下算数完；双方都数完推 both 默契用时。 */
    public TodayVO sheepTap(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        long now = System.currentTimeMillis();
        CoupleCozySheep row = sheepMapper.find(space.getId(), day, me);
        if (row == null) {
            row = CoupleCozySheep.of(space.getId(), day, me);
            row.setTaps(1);
            row.setUpdatedAt(now);
            sheepMapper.insert(row);
        } else if (Integer.valueOf(1).equals(row.getDone())) {
            return today(me);
        } else if (now - row.getUpdatedAt() > SHEEP_WINDOW_MS) {
            row.setTaps(1);
            row.setCreated(now);
            row.setUpdatedAt(now);
            sheepMapper.updateById(row);
        } else {
            row.setTaps(row.getTaps() + 1);
            row.setUpdatedAt(now);
            if (row.getTaps() >= SHEEP_TARGET) {
                row.setDone(1);
                row.setElapsedMs((int) (now - row.getCreated()));
            }
            sheepMapper.updateById(row);
        }
        CoupleCozySheep partner = sheepMapper.find(space.getId(), day, space.partnerOf(me));
        if (partner != null && Integer.valueOf(1).equals(partner.getDone())
                && Integer.valueOf(1).equals(row.getDone())) {
            push.pushCoupleEventBoth("cozy-sheep-done", me, space.getUserA(), space.getUserB(),
                    "🐑 今晚一起数完了一群羊！用时 " + row.getElapsedMs() / 1000 + "s vs "
                            + partner.getElapsedMs() / 1000 + "s，谁先睡着还不一定呢。");
        }
        return today(me);
    }

    private SheepVO sheepState(CoupleSpace space, String me, String day) {
        CoupleCozySheep mine = sheepMapper.find(space.getId(), day, me);
        CoupleCozySheep partner = sheepMapper.find(space.getId(), day, space.partnerOf(me));
        return new SheepVO(mine == null ? 0 : mine.getTaps(), partner == null ? 0 : partner.getTaps(),
                mine != null && Integer.valueOf(1).equals(mine.getDone()),
                partner != null && Integer.valueOf(1).equals(partner.getDone()),
                mine == null ? null : mine.getElapsedMs(), partner == null ? null : partner.getElapsedMs());
    }

    // ========== F223 喝水接力 ==========

    /** 我干一杯，TA 的杯子亮一格。 */
    public TodayVO water(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        CoupleCozyWater row = waterMapper.find(space.getId(), day, me);
        if (row == null) {
            row = CoupleCozyWater.of(space.getId(), day, me);
            row.setCups(1);
            row.setUpdatedAt(System.currentTimeMillis());
            waterMapper.insert(row);
        } else {
            row.setCups(row.getCups() + 1);
            row.setUpdatedAt(System.currentTimeMillis());
            waterMapper.updateById(row);
        }
        push.pushCoupleEvent("cozy-water", me, space.partnerOf(me),
                "💧 TA 干了一杯水，你的杯子又攒了一格，回一口？");
        return today(me);
    }

    private WaterVO waterState(CoupleSpace space, String me, String day) {
        CoupleCozyWater mine = waterMapper.find(space.getId(), day, me);
        CoupleCozyWater partner = waterMapper.find(space.getId(), day, space.partnerOf(me));
        boolean nudge = partner != null && partner.getCups() > 0
                && (mine == null || mine.getCups() == 0)
                && System.currentTimeMillis() - partner.getUpdatedAt() > WATER_NUDGE_MS;
        return new WaterVO(mine == null ? 0 : mine.getCups(), partner == null ? 0 : partner.getCups(), nudge);
    }

    // ========== F224 冷暖互报 ==========

    /** 互报今日体感（纯文字），当日可改。 */
    public TodayVO weather(String me, String city, String feel, String tempText) {
        CoupleSpace space = requireSpace(me);
        String c = trimLimit(city, 30, "城市名 30 字以内哦");
        if (c == null) {
            throw new BusinessException(400, "先写下你在哪个城市 🌆");
        }
        String f = trimLimit(feel, 10, "体感词 10 字以内哦");
        if (f == null) {
            throw new BusinessException(400, "体感选一个：冷 / 暖 / 刚好");
        }
        String day = LocalDate.now().toString();
        CoupleCozyWeather row = weatherMapper.find(space.getId(), day, me);
        if (row == null) {
            weatherMapper.insert(CoupleCozyWeather.of(space.getId(), day, me, c, f,
                    orEmpty(trimLimit(tempText, 10, "气温 10 字以内哦"))));
            push.pushCoupleEvent("cozy-weather", me, space.partnerOf(me),
                    "🌡️ TA 互报了今天：「" + c + "」" + f + "，去看看要不要叮嘱一句。");
        } else {
            row.setCity(c);
            row.setFeel(f);
            row.setTempText(orEmpty(trimLimit(tempText, 10, "气温 10 字以内哦")));
            weatherMapper.updateById(row);
        }
        return today(me);
    }

    /** 对 TA 今天的体感一键「叮嘱添衣」，送达一句土味关怀（同一人一天只叮嘱一次）。 */
    public TodayVO adviseWeather(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        CoupleCozyWeather partner = weatherMapper.find(space.getId(), day, space.partnerOf(me));
        if (partner == null) {
            throw new BusinessException(400, "TA 今天还没互报冷暖呢");
        }
        if (!partner.getAdvisedBy().isEmpty()) {
            return today(me);
        }
        partner.setAdvisedBy(me);
        weatherMapper.updateById(partner);
        push.pushCoupleEvent("cozy-advise", me, space.partnerOf(me),
                "🧣 叮嘱送达：" + CoupleCozyBank.adviseLine(space.getId(), day, me));
        return today(me);
    }

    private List<WeatherVO> weathersOf(CoupleSpace space, String me, String day) {
        List<WeatherVO> out = new ArrayList<>();
        for (String u : List.of(me, space.partnerOf(me))) {
            CoupleCozyWeather w = weatherMapper.find(space.getId(), day, u);
            if (w != null) {
                out.add(new WeatherVO(w.getFromUser(), me.equals(w.getFromUser()), w.getCity(), w.getFeel(),
                        w.getTempText(), !w.getAdvisedBy().isEmpty()));
            }
        }
        return out;
    }

    // ========== F225 熬夜守护 ==========

    /** 递一张「早点睡」陪伴卡：一天一张，重复递幂等不再推。 */
    public TodayVO latenight(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        if (latenightMapper.find(space.getId(), day, me) == null) {
            latenightMapper.insert(CoupleCozyLatenight.of(space.getId(), day, me));
            push.pushCoupleEvent("cozy-latenight", me, space.partnerOf(me),
                    CoupleCozyBank.latenightCard(space.getId(), day, me));
        }
        return today(me);
    }

    // ========== F226 周末慢生活 ==========

    /** 本周提一件「什么都不赶」的小事（可改）。 */
    public TodayVO slow(String me, String thing) {
        CoupleSpace space = requireSpace(me);
        String t = trimLimit(thing, 70, "小事 70 字以内哦");
        if (t == null) {
            throw new BusinessException(400, "写一件这周什么都不赶的小事 🐢");
        }
        String week = currentWeek();
        CoupleCozySlow row = slowMapper.find(space.getId(), week, me);
        boolean isNew = row == null;
        if (isNew) {
            slowMapper.insert(CoupleCozySlow.of(space.getId(), week, me, t));
        } else {
            row.setThing(t);
            row.setUpdatedAt(System.currentTimeMillis());
            slowMapper.updateById(row);
        }
        if (isNew && slowsOf(space, me).size() == 2) {
            push.pushCoupleEventBoth("cozy-slow-planned", me, space.getUserA(), space.getUserB(),
                    "🐢 这周的慢生活安排好了：谁都别赶，周日见分晓。");
        }
        return today(me);
    }

    /** 打卡自己的慢生活小事；双方都打完推一条「周末回放」。 */
    public TodayVO slowCheck(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleCozySlow row = slowMapper.find(space.getId(), currentWeek(), me);
        if (row == null) {
            throw new BusinessException(400, "这周还没提小事呢，先提一件");
        }
        if (row.getDoneDay().isEmpty()) {
            row.setDoneDay(LocalDate.now().toString());
            row.setUpdatedAt(System.currentTimeMillis());
            slowMapper.updateById(row);
        }
        List<CoupleCozySlow> both = slowMapper.findByWeek(space.getId(), currentWeek());
        if (both.size() >= 2 && both.stream().allMatch(s -> !s.getDoneDay().isEmpty())) {
            push.pushCoupleEventBoth("cozy-slow-done", me, space.getUserA(), space.getUserB(),
                    "🛋️ 慢生活回放：" + both.stream().map(s -> "「" + s.getThing() + "」").reduce("", (a, b) -> a + b) + " 都做到了，下周继续不赶。");
        }
        return today(me);
    }

    private List<SlowVO> slowsOf(CoupleSpace space, String me) {
        return slowMapper.findByWeek(space.getId(), currentWeek()).stream()
                .map(s -> new SlowVO(s.getFromUser(), me.equals(s.getFromUser()), s.getThing(), s.getDoneDay()))
                .toList();
    }

    // ========== F227 疼痛对策本 ==========

    /** 登记/更新「我难受时的正确做法」（每人一本，随时改）。 */
    public TodayVO saveRemedy(String me, String body) {
        CoupleSpace space = requireSpace(me);
        String b = trimLimit(body, 300, "对策清单 300 字以内哦");
        if (b == null) {
            throw new BusinessException(400, "写下你难受时的正确做法吧，比如：胃疼→热水袋+小米粥，别说话让我躺会儿 🤕");
        }
        CoupleCozyRemedy row = remedyMapper.find(space.getId(), me);
        if (row == null) {
            remedyMapper.insert(CoupleCozyRemedy.of(space.getId(), me, b));
        } else {
            row.setBody(b);
            row.setUpdatedAt(System.currentTimeMillis());
            remedyMapper.updateById(row);
        }
        return today(me);
    }

    /** TA 不适日：一键按 TA 的对策执行并送达关怀推送。 */
    public TodayVO comfort(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleCozyRemedy target = remedyMapper.find(space.getId(), space.partnerOf(me));
        if (target == null) {
            throw new BusinessException(400, "TA 还没有疼痛对策本，先让 TA 写一本");
        }
        push.pushCoupleEvent("cozy-comfort", me, space.partnerOf(me),
                "🤕 你的对策卡已生效：" + target.getBody() + "。照做，我在。");
        return today(me);
    }

    private List<RemedyVO> remediesOf(CoupleSpace space, String me) {
        List<RemedyVO> out = new ArrayList<>();
        for (String u : List.of(me, space.partnerOf(me))) {
            CoupleCozyRemedy r = remedyMapper.find(space.getId(), u);
            if (r != null) {
                out.add(new RemedyVO(r.getForUser(), me.equals(r.getForUser()), r.getBody(), r.getUpdatedAt()));
            }
        }
        return out;
    }

    // ========== F228 抱抱计量器 ==========

    /** 见面抱了？自报几下，跨过里程碑推 both 点亮。 */
    public TodayVO hug(String me, Integer cnt, String note) {
        CoupleSpace space = requireSpace(me);
        int c = cnt == null ? 1 : Math.min(99, Math.max(1, cnt));
        int before = hugTotal(space);
        hugMapper.insert(CoupleCozyHug.of(space.getId(), LocalDate.now().toString(), me, c,
                orEmpty(trimLimit(note, 70, "备注 70 字以内哦"))));
        int after = before + c;
        for (int m : HUG_MILESTONES) {
            if (before < m && after >= m) {
                push.pushCoupleEventBoth("cozy-hug-milestone", me, space.getUserA(), space.getUserB(),
                        "🫂 我们的抱抱累计破 " + m + " 次啦，存档！");
            }
        }
        return today(me);
    }

    private int hugTotal(CoupleSpace space) {
        return hugMapper.findBySpace(space.getId()).stream().mapToInt(h -> h.getCnt() == null ? 0 : h.getCnt()).sum();
    }

    private HugVO hugState(CoupleSpace space, String day) {
        List<CoupleCozyHug> all = hugMapper.findBySpace(space.getId());
        int total = all.stream().mapToInt(h -> h.getCnt() == null ? 0 : h.getCnt()).sum();
        int today = all.stream().filter(h -> day.equals(h.getDay())).mapToInt(h -> h.getCnt() == null ? 0 : h.getCnt()).sum();
        Integer milestone = null;
        for (int m : HUG_MILESTONES) {
            if (total >= m) {
                milestone = m;
            }
        }
        return new HugVO(total, today, milestone);
    }

    // ========== F229 月度安眠小结（聚合无表） ==========

    // ========== 深夜判断（供 F225 前端展示入口用，无表） ==========

    /** 现在是否深夜时段（23:30-05:00），控制「早点睡」卡片按钮的显隐建议。 */
    public boolean inDeepNight() {
        LocalTime t = LocalTime.now();
        return t.isAfter(LocalTime.of(23, 30)) || t.isBefore(LocalTime.of(5, 0));
    }

    // ========== 内部工具 ==========

    static String currentWeek() {
        return LocalDate.now().with(DayOfWeek.MONDAY).toString();
    }

    private static String normalizeDay(String day, String errMsg) {
        if (day == null || day.isBlank()) {
            throw new BusinessException(400, errMsg);
        }
        try {
            return LocalDate.parse(day.trim()).toString();
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, errMsg);
        }
    }

    private static String trimLimit(String v, int max, String errMsg) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > max) {
            throw new BusinessException(400, errMsg);
        }
        return t;
    }

    private static String orEmpty(String v) {
        return v == null ? "" : v;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
