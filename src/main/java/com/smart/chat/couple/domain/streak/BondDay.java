package com.smart.chat.couple.domain.streak;

import java.util.UUID;

/**
 * 一个贴贴打卡日：这一行代表「这天双方都贴贴过」（或事后补签成功）。
 * <p>
 * 薄实体——连续天数与七档解锁都由 {@link BondStreak} 从日子集合读时算出，
 * 这一层不持有任何推导结果，也不重复实现补签规则（那是 {@link MakeupPolicy} 的事）。
 */
public final class BondDay {

    /** 双方当天都发过贴贴，系统自动确认 */
    public static final String SOURCE_AUTO = "AUTO";
    /** 花积分补签 */
    public static final String SOURCE_MAKEUP = "MAKEUP";

    private final String id;
    private final String spaceId;
    private final String day;
    private final String source;
    private final String operatorUser;
    private final long created;

    private BondDay(String id, String spaceId, String day, String source, String operatorUser, long created) {
        this.id = id;
        this.spaceId = spaceId;
        this.day = day;
        this.source = source;
        this.operatorUser = operatorUser;
        this.created = created;
    }

    /** 确认一天打卡（自动或补签由 source 说清楚，operatorUser 只有补签才有） */
    public static BondDay confirm(String spaceId, String day, String source, String operatorUser) {
        return new BondDay(UUID.randomUUID().toString(), spaceId, day, source, operatorUser,
                System.currentTimeMillis());
    }

    public static BondDay restore(String id, String spaceId, String day, String source, String operatorUser,
                                  Long created) {
        return new BondDay(id, spaceId, day, source, operatorUser, created == null ? 0L : created);
    }

    /** 这一天是花钱补回来的吗（时间轴要区分「真贴的」和「补的」） */
    public boolean makeup() {
        return SOURCE_MAKEUP.equals(source);
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String day() {
        return day;
    }

    public String source() {
        return source;
    }

    public String operatorUser() {
        return operatorUser;
    }

    public long created() {
        return created;
    }
}
