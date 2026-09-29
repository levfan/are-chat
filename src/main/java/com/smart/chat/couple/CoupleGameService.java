package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 恋爱游戏化：今日心动加成、互动热力图、心情曲线、恋爱红绿灯。
 * 全部由现有数据实时推导，无新表。情绪价值设计：让「每天都互动」这件事变成可玩的游戏。
 */
@Service
public class CoupleGameService {

    // ========== VO ==========

    /** 今日加成单项。 */
    public record BoostItem(String key, String label, String emoji, boolean done, int bonus, String hint) {
    }

    /** F31 今日心动加成：今天的互动按项计分，明天清零重来。 */
    public record IntimacyBoostVO(String day, List<BoostItem> items, int totalBonus, String cheer) {
    }

    /** 热力图单格：level 0-4（0=无互动，4=爆表）。 */
    public record HeatCell(String day, int count, int level) {
    }

    /** F33 互动热力图：最近 12 周。 */
    public record HeatmapVO(int weeks, List<HeatCell> cells, int maxCount, int activeDays) {
    }

    /** 心情曲线单日：双方心情分 1-5（没记 = null）。 */
    public record MoodCurveDay(String day, Integer mine, Integer partner) {
    }

    /** F34 心情曲线：最近 30 天双方心情走势。 */
    public record MoodCurveVO(List<MoodCurveDay> days, double myAvg, double partnerAvg) {
    }

    /** F35 恋爱红绿灯。 */
    public record TrafficLightVO(String light, String title, String detail, String advice,
                                 Long hoursSinceLast, String lastDay) {
    }

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleCheckinMapper checkinMapper;
    private final CoupleAnswerMapper answerMapper;
    private final CoupleActionMapper actionMapper;
    private final CoupleTaskMapper taskMapper;
    private final CoupleMoodMapper moodMapper;
    private final CoupleLetterMapper letterMapper;
    private final CouplePraiseMapper praiseMapper;

    @SuppressWarnings("java:S107")
    public CoupleGameService(CoupleSpaceMapper spaceMapper, CoupleCheckinMapper checkinMapper,
                             CoupleAnswerMapper answerMapper, CoupleActionMapper actionMapper,
                             CoupleTaskMapper taskMapper, CoupleMoodMapper moodMapper,
                             CoupleLetterMapper letterMapper, CouplePraiseMapper praiseMapper) {
        this.spaceMapper = spaceMapper;
        this.checkinMapper = checkinMapper;
        this.answerMapper = answerMapper;
        this.actionMapper = actionMapper;
        this.taskMapper = taskMapper;
        this.moodMapper = moodMapper;
        this.letterMapper = letterMapper;
        this.praiseMapper = praiseMapper;
    }

    // ========== F31 今日心动加成 ==========

    /** 今日加成：每天最多可得 20 点，当天 24 点清零。 */
    public IntimacyBoostVO todayBoost(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String today = LocalDate.now().toString();

        Set<String> morning = checkinUsers(space, CoupleCheckin.KIND_MORNING, today);
        Set<String> night = checkinUsers(space, CoupleCheckin.KIND_NIGHT, today);
        Set<String> answered = answerUsers(space, today);
        long bondToday = actionMapper.findBySpace(space.getId()).stream()
                .filter(a -> a.getCreated() != null && sameDay(a.getCreated(), today)).count();
        long taskToday = taskMapper.findBySpace(space.getId()).stream()
                .filter(t -> CoupleTask.STATUS_DONE.equals(t.getStatus()))
                .filter(t -> today.equals(t.getTaskDay())).count();
        boolean moodToday = moodMapper.findBySpace(space.getId()).stream()
                .anyMatch(m -> me.equals(m.getUsername()) && today.equals(m.getMoodDay()));
        long letterToday = letterMapper.findBySpace(space.getId()).stream()
                .filter(l -> me.equals(l.getSender()) && l.getCreated() != null && sameDay(l.getCreated(), today)).count();
        long praiseToday = praiseMapper.findBySpace(space.getId()).stream()
                .filter(p -> me.equals(p.getFromUser()) && p.getCreated() != null && sameDay(p.getCreated(), today)).count();

        List<BoostItem> items = new ArrayList<>();
        boolean bothMorning = morning.contains(me) && morning.contains(partner);
        boolean bothNight = night.contains(me) && night.contains(partner);
        boolean bothAnswer = answered.contains(me) && answered.contains(partner);
        items.add(new BoostItem("morning", "互道早安", "🌅", bothMorning, 4,
                bothMorning ? "已完成" : morning.contains(me) ? "就等 TA 啦" : "说一声早安"));
        items.add(new BoostItem("night", "互道晚安", "🌙", bothNight, 4,
                bothNight ? "已完成" : night.contains(me) ? "就等 TA 啦" : "睡前道声晚安"));
        items.add(new BoostItem("question", "一问同答", "💬", bothAnswer, 4,
                bothAnswer ? "已完成" : answered.contains(me) ? "就等 TA 啦" : "去回答今日一问"));
        int bondBonus = (int) Math.min(bondToday, 5);
        items.add(new BoostItem("bond", "贴贴互动", "🫶", bondToday > 0, bondBonus,
                bondToday > 0 ? "今天贴了 " + bondToday + " 次" : "去贴贴 TA"));
        items.add(new BoostItem("task", "甜蜜任务", "🍬", taskToday > 0, taskToday > 0 ? 3 : 0,
                taskToday > 0 ? "已完成" : "看看今天的任务卡"));
        items.add(new BoostItem("mood", "心情打卡", "📔", moodToday, 1,
                moodToday ? "已完成" : "记录今天的心情"));
        items.add(new BoostItem("letter", "写悄悄话", "💌", letterToday > 0, 2,
                letterToday > 0 ? "已完成" : "给 TA 写一句"));
        items.add(new BoostItem("praise", "夸一夸", "🌟", praiseToday > 0, 2,
                praiseToday > 0 ? "已完成" : "夸夸今天 TA 做的事"));

        int total = items.stream().mapToInt(BoostItem::bonus).sum();
        String cheer = total >= 20 ? "今天的甜度爆表！满加成达成 🎉"
                : total >= 10 ? "今天也很甜，还差一点点就满加成啦 💖"
                : "再加把劲，把今天的甜度攒满 🍬";
        return new IntimacyBoostVO(today, items, total, cheer);
    }

    // ========== F33 互动热力图 ==========

    /** 最近 12 周（84 天）互动热力图：0=无互动，1=1 次，2=2-3 次，3=4-6 次，4=7+ 次。 */
    public HeatmapVO heatmap(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(83);

        Map<String, Integer> counts = new HashMap<>();
        for (String kind : new String[]{CoupleCheckin.KIND_MORNING, CoupleCheckin.KIND_NIGHT}) {
            for (CoupleCheckin row : checkinMapper.findBySpaceAndKind(space.getId(), kind)) {
                bump(counts, row.getCheckinDay(), start, today);
            }
        }
        for (CoupleAnswer row : answerMapper.findBySpace(space.getId())) {
            bump(counts, row.getAnswerDay(), start, today);
        }
        for (CoupleAction row : actionMapper.findBySpace(space.getId())) {
            if (row.getCreated() != null) {
                bump(counts, Instant.ofEpochMilli(row.getCreated()).atZone(ZoneId.systemDefault())
                        .toLocalDate().toString(), start, today);
            }
        }
        for (CoupleTask row : taskMapper.findBySpace(space.getId())) {
            if (CoupleTask.STATUS_DONE.equals(row.getStatus())) {
                bump(counts, row.getTaskDay(), start, today);
            }
        }
        for (CoupleMood row : moodMapper.findBySpace(space.getId())) {
            bump(counts, row.getMoodDay(), start, today);
        }
        for (CoupleLetter row : letterMapper.findBySpace(space.getId())) {
            if (row.getCreated() != null) {
                bump(counts, Instant.ofEpochMilli(row.getCreated()).atZone(ZoneId.systemDefault())
                        .toLocalDate().toString(), start, today);
            }
        }

        List<HeatCell> cells = new ArrayList<>();
        int maxCount = 0;
        int activeDays = 0;
        for (int i = 0; i < 84; i++) {
            String day = start.plusDays(i).toString();
            int count = counts.getOrDefault(day, 0);
            maxCount = Math.max(maxCount, count);
            if (count > 0) {
                activeDays++;
            }
            cells.add(new HeatCell(day, count, heatLevel(count)));
        }
        return new HeatmapVO(12, cells, maxCount, activeDays);
    }

    // ========== F34 心情曲线 ==========

    /** 最近 30 天双方心情走势（1-5 分，没记为 null，前端画双线图）。 */
    public MoodCurveVO moodCurve(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        LocalDate today = LocalDate.now();

        Map<String, Integer> mine = new HashMap<>();
        Map<String, Integer> theirs = new HashMap<>();
        for (CoupleMood row : moodMapper.findBySpace(space.getId())) {
            Integer score = moodScore(row.getMood());
            if (score == null) {
                continue;
            }
            if (me.equals(row.getUsername())) {
                mine.putIfAbsent(row.getMoodDay(), score);
            } else {
                theirs.putIfAbsent(row.getMoodDay(), score);
            }
        }

        List<MoodCurveDay> days = new ArrayList<>();
        double mySum = 0;
        long myCount = 0;
        double theirSum = 0;
        long theirCount = 0;
        for (int i = 29; i >= 0; i--) {
            String day = today.minusDays(i).toString();
            Integer m = mine.get(day);
            Integer p = theirs.get(day);
            if (m != null) {
                mySum += m;
                myCount++;
            }
            if (p != null) {
                theirSum += p;
                theirCount++;
            }
            days.add(new MoodCurveDay(day, m, p));
        }
        double myAvg = myCount == 0 ? 0 : Math.round(mySum / myCount * 10) / 10.0;
        double partnerAvg = theirCount == 0 ? 0 : Math.round(theirSum / theirCount * 10) / 10.0;
        return new MoodCurveVO(days, myAvg, partnerAvg);
    }

    // ========== F35 恋爱红绿灯 ==========

    /** 恋爱红绿灯：多久没互动了——绿=今天有互动，黄=2-3 天，红=4 天以上。 */
    public TrafficLightVO trafficLight(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate today = LocalDate.now();

        Long lastAt = lastInteractionAt(space);
        if (lastAt == null) {
            return new TrafficLightVO("YELLOW", "还没开张", "空间里还没有任何互动记录",
                    "去打个招呼吧：贴一贴 TA，或者说声早安 🌅", null, null);
        }
        LocalDate lastDay = Instant.ofEpochMilli(lastAt).atZone(ZoneId.systemDefault()).toLocalDate();
        long gapDays = ChronoUnit.DAYS.between(lastDay, today);
        long hours = (System.currentTimeMillis() - lastAt) / 3_600_000L;
        switch ((int) Math.min(gapDays, 4)) {
            case 0:
                return new TrafficLightVO("GREEN", "畅通无阻", "今天已经互动过啦",
                        "保持这个频率，甜度持续上升 🍬", hours, lastDay.toString());
            case 1:
            case 2:
            case 3:
                return new TrafficLightVO("YELLOW", "有点想念", "你们已经 " + (gapDays + 1) + " 天没有互动了",
                        "先主动一步：发一句悄悄话，或者补一张甜蜜任务卡 💬", hours, lastDay.toString());
            default:
                return new TrafficLightVO("RED", "该行动了", "超过 4 天没有互动，感情需要浇灌 🚨",
                        "别让沉默变习惯：现在就给 TA 写一封信，或者直接约一场约会！", hours, lastDay.toString());
        }
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    private Set<String> checkinUsers(CoupleSpace space, String kind, String day) {
        Set<String> users = new HashSet<>();
        for (CoupleCheckin row : checkinMapper.findBySpaceAndKind(space.getId(), kind)) {
            if (day.equals(row.getCheckinDay())) {
                users.add(row.getUsername());
            }
        }
        return users;
    }

    private Set<String> answerUsers(CoupleSpace space, String day) {
        Set<String> users = new HashSet<>();
        for (CoupleAnswer row : answerMapper.findBySpace(space.getId())) {
            if (day.equals(row.getAnswerDay())) {
                users.add(row.getUsername());
            }
        }
        return users;
    }

    private boolean sameDay(long at, String day) {
        return Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate().toString().equals(day);
    }

    private void bump(Map<String, Integer> counts, String day, LocalDate start, LocalDate today) {
        if (day == null) {
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(day);
        } catch (Exception e) {
            return;
        }
        if (date.isBefore(start) || date.isAfter(today)) {
            return;
        }
        counts.merge(day, 1, Integer::sum);
    }

    private int heatLevel(int count) {
        if (count >= 7) {
            return 4;
        }
        if (count >= 4) {
            return 3;
        }
        if (count >= 2) {
            return 2;
        }
        if (count >= 1) {
            return 1;
        }
        return 0;
    }

    /** 心情 → 1-5 分（负面低、正面高），未知心情记 3 分中性。 */
    private Integer moodScore(String mood) {
        if (mood == null) {
            return null;
        }
        return switch (mood) {
            case "SAD", "ANGRY" -> 1;
            case "SICK", "TIRED" -> 2;
            case "CALM", "NORMAL", "NEUTRAL" -> 3;
            case "HAPPY", "LUCKY" -> 4;
            case "LOVE", "EXCITED", "SWEET" -> 5;
            default -> 3;
        };
    }

    /** 最近一次互动时间：扫描常用互动表取最大 created。 */
    private Long lastInteractionAt(CoupleSpace space) {
        String spaceId = space.getId();
        Long last = null;
        for (String kind : new String[]{CoupleCheckin.KIND_MORNING, CoupleCheckin.KIND_NIGHT}) {
            for (CoupleCheckin row : checkinMapper.findBySpaceAndKind(spaceId, kind)) {
                last = maxAt(last, row.getCreated());
            }
        }
        for (CoupleAnswer row : answerMapper.findBySpace(spaceId)) {
            last = maxAt(last, row.getCreated());
        }
        for (CoupleAction row : actionMapper.findBySpace(spaceId)) {
            last = maxAt(last, row.getCreated());
        }
        for (CoupleTask row : taskMapper.findBySpace(spaceId)) {
            if (CoupleTask.STATUS_DONE.equals(row.getStatus())) {
                last = maxAt(last, row.getDoneAt());
            }
        }
        for (CoupleMood row : moodMapper.findBySpace(spaceId)) {
            last = maxAt(last, row.getCreated());
        }
        for (CoupleLetter row : letterMapper.findBySpace(spaceId)) {
            last = maxAt(last, row.getCreated());
        }
        return last;
    }

    private Long maxAt(Long current, Long candidate) {
        if (candidate == null) {
            return current;
        }
        return current == null ? candidate : Math.max(current, candidate);
    }
}
