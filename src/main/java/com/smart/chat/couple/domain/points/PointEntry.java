package com.smart.chat.couple.domain.points;

import java.util.UUID;

/**
 * 积分台账的一行流水：谁（fromUser）在哪个空间因为这个名目（item）赚或花了多少分。
 * <p>
 * 它是<b>只追加的流水</b>，没有「改一笔」这种操作——所以这里没有 setter，只有两个带语义的工厂。
 * 心动值里的 `pointEarned` 与愿望券的余额都从这条台账算出来（口径见 {@code docs/ddd/03-phase-plan.md}），
 * 所以「EARN 加在谁头上」是产品口径，必须在这一层写死，不能由调用方传字符串决定。
 */
public final class PointEntry {

    public static final String TYPE_EARN = "EARN";
    public static final String TYPE_SPEND = "SPEND";

    private final String id;
    private final String spaceId;
    private final String fromUser;
    private final String type;
    private final String item;
    private final int points;
    private final long created;

    private PointEntry(String id, String spaceId, String fromUser, String type, String item, int points, long created) {
        this.id = id;
        this.spaceId = spaceId;
        this.fromUser = fromUser;
        this.type = type;
        this.item = item;
        this.points = points;
        this.created = created;
    }

    /** 记账时刻的流水（id 与 created 在这里生成，全项目只有这一处给台账分配主键） */
    public static PointEntry earn(String spaceId, String user, String item, int points) {
        return new PointEntry(UUID.randomUUID().toString(), spaceId, user, TYPE_EARN, item, points,
                System.currentTimeMillis());
    }

    /** 花分（愿望券、补签这类出水口） */
    public static PointEntry spend(String spaceId, String user, String item, int points) {
        return new PointEntry(UUID.randomUUID().toString(), spaceId, user, TYPE_SPEND, item, points,
                System.currentTimeMillis());
    }

    /** 从存储还原（历史行不参与新建规则） */
    public static PointEntry restore(String id, String spaceId, String fromUser, String type, String item,
                                     Integer points, Long created) {
        return new PointEntry(id, spaceId, fromUser, type, item,
                points == null ? 0 : points, created == null ? 0L : created);
    }

    /** 这一笔是不是「赚」——心动值与余额都只加 EARN 行 */
    public boolean earned() {
        return TYPE_EARN.equals(type);
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String fromUser() {
        return fromUser;
    }

    public String type() {
        return type;
    }

    public String item() {
        return item;
    }

    public int points() {
        return points;
    }

    public long created() {
        return created;
    }
}
