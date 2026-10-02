package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F381 雷区探测器：提前挂出「易吵话题 + 我的雷点 + 安全的说法」，对方盖「已知晓」。
 * 盖章口径同 F375 双人 tick：ack(by) 已盖过就不改写并返回 false，
 * 服务层只在「本次真的新盖」时推一次事件（重复点静默）。
 */
@Data
@TableName("couple_catch_mine")
public class CoupleCatchMine {

    /** 易吵话题字数上限（列 varchar(120) 已按 4 倍宽度放宽） */
    public static final int TOPIC_MAX = 30;
    /** 雷点字数上限（列 varchar(240) 已按 4 倍宽度放宽） */
    public static final int TRIP_MAX = 60;
    /** 安全说法字数上限（列 varchar(240) 已按 4 倍宽度放宽） */
    public static final int SAFE_MAX = 60;
    /** 每人挂出的雷区条数上限（服务层用） */
    public static final int PER_USER_MAX = 6;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 挂雷的人（区分大小写） */
    private String fromUser;
    /** 易吵话题 */
    private String topic;
    /** 我的雷点在哪；空串=没说 */
    private String trip;
    /** 安全的说法/做法；空串=没说 */
    private String safeWay;
    /** 知晓盖章的人=对方（区分大小写）；null=还没盖 */
    private String ackBy;
    /** 盖章时间毫秒；null=还没盖 */
    private Long ackAt;
    /** 成功避雷次数（对方主动记，年报用） */
    private Integer avoided;
    private Long created;
    private Long updatedAt;

    public static CoupleCatchMine of(String spaceId, String fromUser, String topic,
                                     String trip, String safeWay) {
        CoupleCatchMine row = new CoupleCatchMine();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.topic = topic;
        row.trip = trip == null ? "" : trip;
        row.safeWay = safeWay == null ? "" : safeWay;
        row.ackBy = null;
        row.ackAt = null;
        row.avoided = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 对方是否已经盖过「已知晓」。 */
    public boolean acked() {
        return ackAt != null;
    }

    /** 盖「已知晓」；返回本次是否真的新盖（已盖过不改写，用于只推一次）。 */
    public boolean ack(String by) {
        if (acked()) {
            return false;
        }
        ackBy = by;
        ackAt = System.currentTimeMillis();
        updatedAt = ackAt;
        return true;
    }

    /** 记一次成功避雷（自增 1，封顶 999 防刷）。 */
    public void avoid() {
        int now = avoided == null ? 0 : avoided;
        avoided = Math.min(now + 1, 999);
        updatedAt = System.currentTimeMillis();
    }
}
