package com.smart.chat.couple.domain.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 经期周期推算的值对象：唯一输入是记录集合与今天，其余全是派生量。
 * <p>
 * 口径刻意保守：只拿相邻两次开始日的间隔求平均，间隔小于 15 天或大于 45 天的样本直接忽略
 * （漏记一次、隔了很久才补记都会造出这种假间隔）；一条有效样本都没有时按 28 天估。
 * 推算只是提醒，页面文案必须写明「按记录推算」，不能让它看起来像医学判断。
 */
public final class PeriodCycles {

    /** 没有足够样本时的默认周期 */
    public static final int DEFAULT_CYCLE_DAYS = 28;

    /** 有效周期样本的上下界：超出这个范围的相邻间隔多半是漏记或补记造成的 */
    static final int MIN_CYCLE_DAYS = 15;
    static final int MAX_CYCLE_DAYS = 45;

    private final int averageCycleDays;
    private final int sampleCount;
    private final LocalDate predictedNextDay;
    private final Integer daysUntilNext;

    private PeriodCycles(int averageCycleDays, int sampleCount, LocalDate predictedNextDay, Integer daysUntilNext) {
        this.averageCycleDays = averageCycleDays;
        this.sampleCount = sampleCount;
        this.predictedNextDay = predictedNextDay;
        this.daysUntilNext = daysUntilNext;
    }

    /** 从全部记录算平均周期与下次开始日；非法日期静默忽略（restore 不校验，历史脏行不该让整卡 500）。 */
    public static PeriodCycles of(Collection<PeriodRecord> records, LocalDate today) {
        List<LocalDate> starts = new ArrayList<>();
        for (PeriodRecord record : records) {
            LocalDate day = parse(record.startDay());
            if (day != null) {
                starts.add(day);
            }
        }
        Collections.sort(starts);

        List<Integer> intervals = new ArrayList<>();
        for (int i = 1; i < starts.size(); i++) {
            long gap = ChronoUnit.DAYS.between(starts.get(i - 1), starts.get(i));
            if (gap >= MIN_CYCLE_DAYS && gap <= MAX_CYCLE_DAYS) {
                intervals.add((int) gap);
            }
        }

        int average = DEFAULT_CYCLE_DAYS;
        if (!intervals.isEmpty()) {
            int sum = 0;
            for (int interval : intervals) {
                sum += interval;
            }
            average = Math.round((float) sum / intervals.size());
        }
        LocalDate predicted = starts.isEmpty() ? null : starts.get(starts.size() - 1).plusDays(average);
        Integer until = predicted == null ? null : (int) ChronoUnit.DAYS.between(today, predicted);
        return new PeriodCycles(average, intervals.size(), predicted, until);
    }

    private static LocalDate parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public int averageCycleDays() {
        return averageCycleDays;
    }

    /** 参与平均的有效间隔数：0 表示还在用默认 28 天估 */
    public int sampleCount() {
        return sampleCount;
    }

    /** 下一次预计开始日；一条记录都没有时为 null */
    public LocalDate predictedNextDay() {
        return predictedNextDay;
    }

    /** 距预计日还有几天；已经过期为负数，无记录为 null */
    public Integer daysUntilNext() {
        return daysUntilNext;
    }
}
