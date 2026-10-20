package com.smart.chat.couple.domain.intimatelog;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;

/**
 * 亲密记录的读时统计：最近一次、本月分类型次数、累计次数。
 * <p>
 * 只统计能解析的日期；历史脏行静默跳过，不该让整张卡 500。最近一次按「日期 + 记录时刻」取最大，
 * 同一天记了多条也能稳定指到最后写下的那条。
 */
public final class IntimateLogs {

    private final IntimateLog last;
    private final int monthCount;
    private final int monthEdgeCount;
    private final int monthSexCount;
    private final int totalCount;

    private IntimateLogs(IntimateLog last, int monthCount, int monthEdgeCount, int monthSexCount, int totalCount) {
        this.last = last;
        this.monthCount = monthCount;
        this.monthEdgeCount = monthEdgeCount;
        this.monthSexCount = monthSexCount;
        this.totalCount = totalCount;
    }

    public static IntimateLogs of(Collection<IntimateLog> logs, LocalDate today) {
        String month = today.toString().substring(0, 7);
        IntimateLog last = null;
        LocalDate lastDay = null;
        int monthCount = 0;
        int monthEdge = 0;
        int monthSex = 0;
        int total = 0;
        for (IntimateLog log : logs) {
            LocalDate day = parse(log.day());
            if (day == null) {
                continue;
            }
            total++;
            if (log.day().startsWith(month)) {
                monthCount++;
                if (IntimateLog.KIND_EDGE.equals(log.kind())) {
                    monthEdge++;
                } else if (IntimateLog.KIND_SEX.equals(log.kind())) {
                    monthSex++;
                }
            }
            if (lastDay == null || day.isAfter(lastDay)
                    || (day.isEqual(lastDay) && createdOf(log) >= createdOf(last))) {
                last = log;
                lastDay = day;
            }
        }
        return new IntimateLogs(last, monthCount, monthEdge, monthSex, total);
    }

    private static long createdOf(IntimateLog log) {
        return log == null || log.created() == null ? Long.MIN_VALUE : log.created();
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

    /** 最近一次；一条有效记录都没有时为 null */
    public IntimateLog last() {
        return last;
    }

    public int monthCount() {
        return monthCount;
    }

    public int monthEdgeCount() {
        return monthEdgeCount;
    }

    public int monthSexCount() {
        return monthSexCount;
    }

    public int totalCount() {
        return totalCount;
    }
}
