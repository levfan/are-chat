package com.smart.chat.couple.domain.streak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 连续互动打卡的值对象：唯一输入是「已确认的打卡日集合」（含补签），其余全是派生量。
 * <p>
 * 刻意<b>不物化</b> current/longest 到表里——和心动值一样读时算，避免出现一列与真源漂移的缓存。
 * <p>
 * 两条产品口径写在这里而不是 Service 里：
 * <ul>
 *   <li>今天还没打卡时<b>不算断</b>（今天还有机会），只有昨天缺行才叫断了；</li>
 *   <li>解锁档位按<b>历史最长连续</b>判定，因此永不回收——外观被没收比没拿到更伤人。</li>
 * </ul>
 */
public final class StreakDays {

    private final Set<LocalDate> days;
    private final LocalDate today;

    private StreakDays(Set<LocalDate> days, LocalDate today) {
        this.days = Collections.unmodifiableSet(days);
        this.today = today;
    }

    /** 由 yyyy-MM-dd 的打卡日集合建值对象；非法或重复的日子直接忽略。 */
    public static StreakDays of(Collection<String> checkinDays, LocalDate today) {
        Set<LocalDate> parsed = new HashSet<>();
        for (String day : checkinDays) {
            LocalDate date = parseDay(day);
            if (date != null) {
                parsed.add(date);
            }
        }
        return new StreakDays(parsed, today);
    }

    /** 今天是否已经打上卡。 */
    public boolean checkedToday() {
        return days.contains(today);
    }

    /** 昨天缺卡——断签判定，也是补签入口该不该亮出来的条件。 */
    public boolean missedYesterday() {
        return !days.contains(today.minusDays(1));
    }

    /**
     * 当前连续天数：今天已打卡就从今天往回数，否则从昨天往回数（今天还来得及，不算断）。
     */
    public int currentStreak() {
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int count = 0;
        while (days.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }

    /** 历史最长连续天数：单调峰值，解锁档位只看它。 */
    public int longestStreak() {
        int longest = 0;
        for (List<LocalDate> run : runs()) {
            longest = Math.max(longest, run.size());
        }
        return longest;
    }

    /** 累计打卡过多少天（不是连续，是总共）。 */
    public int confirmedDays() {
        return days.size();
    }

    /** 最近一次打卡日，一天都没有返回 null。 */
    public LocalDate lastCheckinDay() {
        return days.stream().max(LocalDate::compareTo).orElse(null);
    }

    /** 已解锁的档位 key（按天数从小到大）。 */
    public List<String> unlockedTierKeys() {
        int longest = longestStreak();
        List<String> keys = new ArrayList<>();
        for (StreakTier tier : StreakTier.values()) {
            if (longest >= tier.days()) {
                keys.add(tier.key());
            }
        }
        return keys;
    }

    public boolean unlocked(String tierKey) {
        StreakTier tier = StreakTier.ofKey(tierKey);
        return tier != null && longestStreak() >= tier.days();
    }

    /** 下一个还没解锁的档位，全解锁了返回 null。 */
    public StreakTier nextTier() {
        int longest = longestStreak();
        for (StreakTier tier : StreakTier.values()) {
            if (longest < tier.days()) {
                return tier;
            }
        }
        return null;
    }

    /** 再连续多少天能碰到下一档（以当前连续数为基准，已满则 0）。 */
    public int daysToNextTier() {
        StreakTier next = nextTier();
        if (next == null) {
            return 0;
        }
        return Math.max(next.days() - currentStreak(), 0);
    }

    /**
     * 某个档位是在哪一天被达成的：取所有够长的连续段里最早的那次「凑满第 N 天」的日子。
     * 从没达到过返回 null。
     */
    public LocalDate unlockedAt(StreakTier tier) {
        LocalDate best = null;
        for (List<LocalDate> run : runs()) {
            if (run.size() < tier.days()) {
                continue;
            }
            LocalDate moment = run.get(0).plusDays(tier.days() - 1L);
            if (best == null || moment.isBefore(best)) {
                best = moment;
            }
        }
        return best;
    }

    /**
     * 打卡数从 prevLongest 涨到 newLongest 时跨过了哪些档位——推送用它去重，
     * 免得「断签后重新爬到 3 天」把气泡再庆祝一遍。
     */
    public static List<StreakTier> tiersCrossed(int prevLongest, int newLongest) {
        List<StreakTier> crossed = new ArrayList<>();
        for (StreakTier tier : StreakTier.values()) {
            if (prevLongest < tier.days() && newLongest >= tier.days()) {
                crossed.add(tier);
            }
        }
        return crossed;
    }

    /** yyyy-MM-dd 形式的打卡日集合（测试与调试用）。 */
    public Set<String> dayStrings() {
        Set<String> texts = new HashSet<>();
        for (LocalDate day : days) {
            texts.add(day.toString());
        }
        return texts;
    }

    /** 所有连续段，按起始日升序。 */
    private List<List<LocalDate>> runs() {
        List<LocalDate> sorted = new ArrayList<>(days);
        Collections.sort(sorted);
        List<List<LocalDate>> result = new ArrayList<>();
        List<LocalDate> run = new ArrayList<>();
        for (LocalDate day : sorted) {
            if (!run.isEmpty() && !run.get(run.size() - 1).plusDays(1).equals(day)) {
                result.add(run);
                run = new ArrayList<>();
            }
            run.add(day);
        }
        if (!run.isEmpty()) {
            result.add(run);
        }
        return result;
    }

    private static LocalDate parseDay(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
