package com.smart.chat.couple.domain.period;

import com.smart.chat.couple.domain.RuleViolation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * 一次经期记录：开始日必填，结束日可以当天记、也可以后补。
 * <p>
 * 这里只守「一条记录自身」的规则：日期格式、不能记未来、结束不能早于开始。
 * 平均周期与下次预测不落在这条记录上，也不物化进表里——它们由 {@link PeriodCycles} 从整段历史读时算。
 * 记录人只做留痕，不做权限闸门：这是两个人共享的记录，双方都能记、都能删。
 */
public final class PeriodRecord {

    private final String id;
    private final String spaceId;
    private final String startDay;
    private String endDay;
    private final String recorderUser;
    private final Long created;

    private PeriodRecord(String id, String spaceId, String startDay, String endDay, String recorderUser, Long created) {
        this.id = id;
        this.spaceId = spaceId;
        this.startDay = startDay;
        this.endDay = endDay;
        this.recorderUser = recorderUser;
        this.created = created;
    }

    /** 新记一次经期：开始日不能是将来，记录人必须有。 */
    public static PeriodRecord start(String spaceId, String startDay, String recorderUser, LocalDate today) {
        LocalDate start = requireDay(startDay, "开始日");
        if (start.isAfter(today)) {
            throw new RuleViolation("还没到的日子记不了呀，今天之后的事留给明天 🩸");
        }
        String recorder = recorderUser == null ? "" : recorderUser.trim();
        if (recorder.isEmpty()) {
            throw new RuleViolation("得知道是谁记的呀");
        }
        return new PeriodRecord(UUID.randomUUID().toString(), spaceId, start.toString(), null, recorder,
                System.currentTimeMillis());
    }

    public static PeriodRecord restore(String id, String spaceId, String startDay, String endDay, String recorderUser,
                                       Long created) {
        return new PeriodRecord(id, spaceId, startDay, endDay, recorderUser, created);
    }

    /** 记结束日：只有还没结束的记录能记，结束日不能早于开始日。 */
    public void markEnded(LocalDate today) {
        if (endDay != null) {
            throw new RuleViolation("这次已经记过结束日啦");
        }
        LocalDate start = LocalDate.parse(startDay);
        if (today.isBefore(start)) {
            throw new RuleViolation("结束日不能早于开始日呀");
        }
        this.endDay = today.toString();
    }

    private static LocalDate requireDay(String text, String label) {
        if (text == null || text.isBlank()) {
            throw new RuleViolation(label + "得选一个日子呀");
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            throw new RuleViolation(label + "得是 yyyy-MM-dd 这样的日子呀");
        }
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String startDay() {
        return startDay;
    }

    public String endDay() {
        return endDay;
    }

    public String recorderUser() {
        return recorderUser;
    }

    public Long created() {
        return created;
    }
}
