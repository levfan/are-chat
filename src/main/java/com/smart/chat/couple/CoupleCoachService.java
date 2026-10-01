package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成长系（F150-F159，批次十一）：21天习惯搭子、感恩便签墙、情绪颗粒度日记、
 * 每周高光互评、共读一分钟、拖延互助所、早安能量站、优点存折、成长年度关键词。
 * 情绪价值设计：把「变得更好」变成两个人一起玩的游戏——TA 盯着的日子总不好意思偷懒，
 * 把「谢谢」写回日常，把「情绪」说出名字，把「对方的优点」存进能随时取用的存折。
 */
@Service
public class CoupleCoachService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleHabitStreakMapper streakMapper;
    private final CoupleThanksNoteMapper thanksMapper;
    private final CoupleFeelLogMapper feelMapper;
    private final CoupleWeeklyStarMapper starMapper;
    private final CoupleReadMinuteMapper readMapper;
    private final CoupleDelayTaskMapper delayMapper;
    private final CouplePraiseBankMapper praiseMapper;
    private final ImPushService push;

    public CoupleCoachService(CoupleSpaceMapper spaceMapper, CoupleHabitStreakMapper streakMapper,
                              CoupleThanksNoteMapper thanksMapper, CoupleFeelLogMapper feelMapper,
                              CoupleWeeklyStarMapper starMapper, CoupleReadMinuteMapper readMapper,
                              CoupleDelayTaskMapper delayMapper, CouplePraiseBankMapper praiseMapper,
                              ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.streakMapper = streakMapper;
        this.thanksMapper = thanksMapper;
        this.feelMapper = feelMapper;
        this.starMapper = starMapper;
        this.readMapper = readMapper;
        this.delayMapper = delayMapper;
        this.praiseMapper = praiseMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record HabitVO(String id, String fromUser, boolean mine, String title,
                          Integer targetDays, Integer doneDays, boolean doneToday,
                          String status, Long doneAt, Long created) {
    }

    public record ThanksVO(String id, String fromUser, boolean mine, String content, Long created) {
    }

    public record FeelVO(String day, CoupleFeelLog mine, CoupleFeelLog partner) {
    }

    public record WeekStarVO(String week, CoupleWeeklyStar mine, CoupleWeeklyStar partner) {
    }

    public record ReadMinuteVO(String day, String passage, CoupleReadMinute mine, CoupleReadMinute partner) {
    }

    public record DelayVO(String id, String fromUser, boolean mine, String title, String deadlineDay,
                          Integer nagCount, String status, Long lastNagAt, Long doneAt, Long created) {
    }

    public record PraiseBankVO(String id, String fromUser, boolean mine, String content, String scene, Long created) {
    }

    public record MorningVO(String greeting, String luckyThing, String luckyColor) {
    }

    public record YearKeywordVO(String year, String keyword, Integer habitDays, Integer thanksCount,
                                Integer feelCount, String summary) {
    }

    // ========== F150 21天习惯搭子 ==========

    public List<HabitVO> habits(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        return streakMapper.findBySpace(space.getId()).stream()
                .map(h -> new HabitVO(h.getId(), h.getFromUser(), h.getFromUser().equals(me), h.getTitle(),
                        h.getTargetDays(), h.getDoneDays(), today.equals(h.getLastDoneDay()),
                        h.getStatus(), h.getDoneAt(), h.getCreated()))
                .toList();
    }

    /** 立一个习惯（每人同时只有一个挑战中的）。 */
    public List<HabitVO> createHabit(String me, String title, Integer targetDays) {
        CoupleSpace space = requireSpace(me);
        if (streakMapper.findOpenByUser(space.getId(), me) != null) {
            throw new BusinessException(400, "你还有一个进行中的习惯搭子，先完成它再开新的 💪");
        }
        String t = trimLimit(title, CoupleHabitStreak.TITLE_MAX, "习惯名写 " + CoupleHabitStreak.TITLE_MAX + " 字以内哦");
        if (t == null) {
            throw new BusinessException(400, "先给习惯起个名字吧，比如「每天读书 30 分钟」");
        }
        int target = targetDays == null ? 21 : targetDays;
        if (target < CoupleHabitStreak.TARGET_MIN || target > CoupleHabitStreak.TARGET_MAX) {
            throw new BusinessException(400, "目标天数在 " + CoupleHabitStreak.TARGET_MIN + "-" + CoupleHabitStreak.TARGET_MAX + " 天之间哦");
        }
        streakMapper.insert(CoupleHabitStreak.of(space.getId(), me, t, target));
        push.pushCoupleEvent("streak-started", me, space.partnerOf(me),
                "🌱 TA 立了一个 " + target + " 天习惯：「" + t + "」——做 TA 的监督员吧！");
        return habits(me);
    }

    /** 习惯打卡：每天一次，满目标自动达成。 */
    public List<HabitVO> checkinHabit(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleHabitStreak row = streakMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这个习惯哦");
        }
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己的习惯要自己打卡呀（帮 TA 盯着就好）");
        }
        if (CoupleHabitStreak.STATUS_DONE.equals(row.getStatus())) {
            throw new BusinessException(400, "这个习惯已经达成啦，去立下一个吧 🌳");
        }
        String today = LocalDate.now().toString();
        if (today.equals(row.getLastDoneDay())) {
            throw new BusinessException(400, "今天已经打过卡啦，明天继续 💪");
        }
        row.setDoneDays(row.getDoneDays() + 1);
        row.setLastDoneDay(today);
        if (row.getDoneDays() >= row.getTargetDays()) {
            row.setStatus(CoupleHabitStreak.STATUS_DONE);
            row.setDoneAt(System.currentTimeMillis());
            streakMapper.updateById(row);
            push.pushCoupleEventBoth("streak-done", me, space.getUserA(), space.getUserB(),
                    "🌳 习惯达成！「" + row.getTitle() + "」坚持满 " + row.getTargetDays() + " 天了，你们太棒了！");
        } else {
            streakMapper.updateById(row);
            push.pushCoupleEvent("streak-checkin", me, space.partnerOf(me),
                    "🌱 TA 的「" + row.getTitle() + "」打卡 " + row.getDoneDays() + "/" + row.getTargetDays() + " 天，快去鼓励一下！");
        }
        return habits(me);
    }

    // ========== F151 感恩便签墙 ==========

    public List<ThanksVO> thanks(String me) {
        CoupleSpace space = requireSpace(me);
        return thanksMapper.findBySpace(space.getId()).stream()
                .map(t -> new ThanksVO(t.getId(), t.getFromUser(), t.getFromUser().equals(me), t.getContent(), t.getCreated()))
                .toList();
    }

    /** 写一张感恩便签。 */
    public List<ThanksVO> addThanks(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(content, CoupleThanksNote.CONTENT_MAX, "便签写 " + CoupleThanksNote.CONTENT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "先想想今天要谢谢 TA 什么 💌");
        }
        thanksMapper.insert(CoupleThanksNote.of(space.getId(), me, text));
        push.pushCoupleEvent("thanks-note", me, space.partnerOf(me),
                "💌 TA 谢谢你：" + abbreviate(text, 24) + "——被记着的感觉很好吧。");
        return thanks(me);
    }

    // ========== F152 情绪颗粒度日记 ==========

    /** 情绪词表（5 族 40 词）。 */
    public List<CoupleCoachBank.FeelFamily> feelFamilies() {
        return CoupleCoachBank.feelFamilies();
    }

    /** 今日情绪日记：我 + 对方（TA 写了就能看到，看到才好去关心）。 */
    public FeelVO feelToday(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        return new FeelVO(day, feelMapper.find(space.getId(), me, day),
                feelMapper.find(space.getId(), space.partnerOf(me), day));
    }

    /** 记录今日情绪（可覆盖）。 */
    public FeelVO saveFeel(String me, String word, Integer intensity, String note) {
        CoupleSpace space = requireSpace(me);
        if (!CoupleCoachBank.isKnownFeelWord(word)) {
            throw new BusinessException(400, "从词表里挑一个最贴切的情绪词吧（情绪颗粒度从小词开始练）");
        }
        String day = LocalDate.now().toString();
        CoupleFeelLog row = feelMapper.find(space.getId(), me, day);
        if (row == null) {
            row = CoupleFeelLog.of(space.getId(), me, day, word,
                    intensity == null ? 3 : Math.max(1, Math.min(5, intensity)),
                    trimLimit(note, CoupleFeelLog.NOTE_MAX, null));
            feelMapper.insert(row);
        } else {
            row.setWord(word);
            row.setIntensity(intensity == null ? row.getIntensity() : Math.max(1, Math.min(5, intensity)));
            row.setNote(trimLimit(note, CoupleFeelLog.NOTE_MAX, null));
            row.setUpdatedAt(System.currentTimeMillis());
            feelMapper.updateById(row);
        }
        push.pushCoupleEvent("feel-logged", me, space.partnerOf(me),
                "🌤️ TA 给今天的心情起了个名字：「" + word + "」。想不想问问为什么？");
        return feelToday(me);
    }

    // ========== F153 每周高光互评 ==========

    public WeekStarVO weekStar(String me) {
        CoupleSpace space = requireSpace(me);
        String week = mondayOf().toString();
        return new WeekStarVO(week, starMapper.find(space.getId(), week, me),
                starMapper.find(space.getId(), week, space.partnerOf(me)));
    }

    /** 提名对方本周的高光瞬间（每周一条可改）。 */
    public WeekStarVO saveWeekStar(String me, String highlight) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(highlight, CoupleWeeklyStar.HIGHLIGHT_MAX, "提名写 " + CoupleWeeklyStar.HIGHLIGHT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "先想想 TA 这周最闪光的瞬间 ✨");
        }
        String week = mondayOf().toString();
        CoupleWeeklyStar row = starMapper.find(space.getId(), week, me);
        if (row == null) {
            starMapper.insert(CoupleWeeklyStar.of(space.getId(), week, me, text));
        } else {
            row.setHighlight(text);
            starMapper.updateById(row);
        }
        if (starMapper.find(space.getId(), week, space.partnerOf(me)) != null) {
            push.pushCoupleEventBoth("week-star-both", me, space.getUserA(), space.getUserB(),
                    "⭐ 本周高光互评集齐！你们互相提名了彼此最闪光的瞬间，去看看吧。");
        } else {
            push.pushCoupleEvent("week-star-saved", me, space.partnerOf(me),
                    "⭐ TA 提名了你的本周高光，去领取属于你的掌声！");
        }
        return weekStar(me);
    }

    // ========== F154 共读一分钟 ==========

    public ReadMinuteVO readMinute(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        return new ReadMinuteVO(day, CoupleCoachBank.pickPassage(space.getId(), day),
                readMapper.find(space.getId(), day, me),
                readMapper.find(space.getId(), day, space.partnerOf(me)));
    }

    /** 写下今日共读感想（可改）。 */
    public ReadMinuteVO saveReadMinute(String me, String thought) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(thought, CoupleReadMinute.THOUGHT_MAX, "感想写 " + CoupleReadMinute.THOUGHT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "读一段，再写一句感想吧 📖");
        }
        String day = LocalDate.now().toString();
        CoupleReadMinute row = readMapper.find(space.getId(), day, me);
        if (row == null) {
            readMapper.insert(CoupleReadMinute.of(space.getId(), day, me, text));
        } else {
            row.setThought(text);
            readMapper.updateById(row);
        }
        push.pushCoupleEvent("read-thought", me, space.partnerOf(me),
                "📖 TA 写下了今天的共读感想，等你的版本。");
        return readMinute(me);
    }

    // ========== F155 拖延互助所 ==========

    public List<DelayVO> delayTasks(String me) {
        CoupleSpace space = requireSpace(me);
        return delayMapper.findBySpace(space.getId()).stream()
                .map(d -> new DelayVO(d.getId(), d.getFromUser(), d.getFromUser().equals(me), d.getTitle(),
                        d.getDeadlineDay(), d.getNagCount(), d.getStatus(), d.getLastNagAt(), d.getDoneAt(), d.getCreated()))
                .toList();
    }

    /** 登记一件拖着的事。 */
    public List<DelayVO> addDelay(String me, String title, String deadlineDay) {
        CoupleSpace space = requireSpace(me);
        String t = trimLimit(title, CoupleDelayTask.TITLE_MAX, "标题写 " + CoupleDelayTask.TITLE_MAX + " 字以内哦");
        if (t == null) {
            throw new BusinessException(400, "先写下你拖着的那件事，说出来就成功一半 🙈");
        }
        delayMapper.insert(CoupleDelayTask.of(space.getId(), me, t,
                trimLimit(deadlineDay, 10, null)));
        push.pushCoupleEvent("delay-added", me, space.partnerOf(me),
                "🙈 TA 把拖了很久的「" + abbreviate(t, 16) + "」交给你监督了，该催就催！");
        return delayTasks(me);
    }

    /** 对方催办（1 小时冷却）。 */
    public List<DelayVO> nagDelay(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleDelayTask row = requireDelay(space.getId(), id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己的事不能自己催自己，交给 TA 来 🙋");
        }
        if (CoupleDelayTask.STATUS_DONE.equals(row.getStatus())) {
            throw new BusinessException(400, "这件事已经办完啦 ✅");
        }
        long now = System.currentTimeMillis();
        if (row.getLastNagAt() != null && now - row.getLastNagAt() < CoupleDelayTask.NAG_COOLDOWN_MS) {
            throw new BusinessException(400, "刚催过啦，1 小时后再催，给 TA 一点缓冲 🍅");
        }
        row.setNagCount(row.getNagCount() + 1);
        row.setLastNagAt(now);
        delayMapper.updateById(row);
        push.pushCoupleEvent("delay-nagged", row.getFromUser(), me,
                "⏰ 监督员第 " + row.getNagCount() + " 次催办：「" + abbreviate(row.getTitle(), 16) + "」，今天办了吗？");
        return delayTasks(me);
    }

    /** 完成（立事人自己宣布），双人庆祝。 */
    public List<DelayVO> doneDelay(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleDelayTask row = requireDelay(space.getId(), id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "只有立事人能宣布完成哦");
        }
        if (CoupleDelayTask.STATUS_DONE.equals(row.getStatus())) {
            throw new BusinessException(400, "这件事已经办完啦 ✅");
        }
        row.setStatus(CoupleDelayTask.STATUS_DONE);
        row.setDoneAt(System.currentTimeMillis());
        delayMapper.updateById(row);
        push.pushCoupleEventBoth("delay-done", me, space.getUserA(), space.getUserB(),
                "🎉 拖延互助所战报：「" + abbreviate(row.getTitle(), 16) + "」在被催 " + row.getNagCount() + " 次后终于完成！撒花！");
        return delayTasks(me);
    }

    // ========== F156 早安能量站（无表） ==========

    public MorningVO morning(String me) {
        CoupleSpace space = requireSpace(me);
        CoupleCoachBank.Morning m = CoupleCoachBank.pickMorning(space.getId(), LocalDate.now().toString());
        return new MorningVO(m.greeting(), m.luckyThing(), m.luckyColor());
    }

    // ========== F158 优点存折 ==========

    public List<PraiseBankVO> praiseBank(String me) {
        CoupleSpace space = requireSpace(me);
        return praiseMapper.findBySpace(space.getId()).stream()
                .map(p -> new PraiseBankVO(p.getId(), p.getFromUser(), p.getFromUser().equals(me), p.getContent(), p.getScene(), p.getCreated()))
                .toList();
    }

    /** 存一条对方优点。 */
    public List<PraiseBankVO> addPraiseBank(String me, String content, String scene) {
        CoupleSpace space = requireSpace(me);
        String text = trimLimit(content, CouplePraiseBank.CONTENT_MAX, "优点写 " + CouplePraiseBank.CONTENT_MAX + " 字以内哦");
        if (text == null) {
            throw new BusinessException(400, "先存一个 TA 的优点进存折 🏦");
        }
        praiseMapper.insert(CouplePraiseBank.of(space.getId(), me, text,
                trimLimit(scene, CouplePraiseBank.SCENE_MAX, null)));
        push.pushCoupleEvent("praise-bank-added", me, space.partnerOf(me),
                "🏦 TA 往优点存折里存了一笔你的好，攒着攒着就是一辈子。");
        return praiseBank(me);
    }

    // ========== F159 成长年度关键词（聚合，无新表） ==========

    public YearKeywordVO yearKeyword(String me, Integer year) {
        CoupleSpace space = requireSpace(me);
        int y = year == null ? LocalDate.now().getYear() : year;
        long start = LocalDate.of(y, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long end = LocalDate.of(y + 1, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        int habitDays = streakMapper.findBySpace(space.getId()).stream()
                .filter(h -> h.getCreated() >= start && h.getCreated() < end)
                .mapToInt(CoupleHabitStreak::getDoneDays)
                .sum();
        List<CoupleThanksNote> thanksInYear = thanksMapper.findBySpace(space.getId()).stream()
                .filter(t -> t.getCreated() >= start && t.getCreated() < end)
                .toList();
        List<CoupleFeelLog> feelsInYear = feelMapper.findBySpace(space.getId()).stream()
                .filter(f -> f.getCreated() >= start && f.getCreated() < end)
                .toList();
        Map<String, Integer> wordCount = new HashMap<>();
        feelsInYear.forEach(f -> wordCount.merge(f.getWord(), 1, Integer::sum));
        String topWord = wordCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        String keyword;
        String summary;
        if (thanksInYear.size() >= habitDays && thanksInYear.size() >= feelsInYear.size() && !thanksInYear.isEmpty()) {
            keyword = "感恩力";
            summary = "这一年你们写下了 " + thanksInYear.size() + " 张感谢便签，把「理所当然」都变成了「谢谢」。";
        } else if (habitDays >= feelsInYear.size() && habitDays > 0) {
            keyword = "坚持力";
            summary = "这一年你们为好习惯攒下了 " + habitDays + " 次打卡，一步没停就是了不起。";
        } else if (!feelsInYear.isEmpty()) {
            keyword = "觉察力";
            summary = "这一年你们给 " + feelsInYear.size() + " 天的心情起了名字" + (topWord == null ? "" : "，出现最多的是「" + topWord + "」") + "。说得出情绪，就吵不起大架。";
        } else {
            keyword = "刚开始";
            summary = "今年的成长故事还空着，从立一个习惯或写一张感谢便签开始吧 🌱";
        }
        return new YearKeywordVO(String.valueOf(y), keyword, habitDays, thanksInYear.size(), feelsInYear.size(), summary);
    }

    // ========== 内部工具 ==========

    private LocalDate mondayOf() {
        LocalDate today = LocalDate.now();
        return today.with(DayOfWeek.MONDAY);
    }

    private String abbreviate(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }

    private String trimLimit(String text, int max, String message) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.trim();
        if (message != null && t.length() > max) {
            throw new BusinessException(400, message);
        }
        return t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    private CoupleDelayTask requireDelay(String spaceId, String id) {
        CoupleDelayTask row = delayMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(spaceId)) {
            throw new BusinessException(404, "没有找到这件拖延的事哦");
        }
        return row;
    }
}
