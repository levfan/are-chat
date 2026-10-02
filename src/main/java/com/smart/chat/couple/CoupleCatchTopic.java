package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F387 话题许愿池：希望我们多聊 XX，对方接单，一周内聊完并留一句感想。
 * 时限口径：一周期限从 takenAt 起算（WEEK_MILLIS），聊完时 takenAt + WEEK_MILLIS < 聊完时刻
 * 则 talkDone 把 overdue 记 1（年报用）；状态只进不退 PENDING→TAKEN→TALKED。
 */
@Data
@TableName("couple_catch_topic")
public class CoupleCatchTopic {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_TAKEN = "TAKEN";
    public static final String STATUS_TALKED = "TALKED";
    /** 话题标题字数上限（列 varchar(120) 已按 4 倍宽度放宽） */
    public static final int TITLE_MAX = 30;
    /** 一句感想字数上限（列 varchar(240) 已按 4 倍宽度放宽） */
    public static final int REFLECT_MAX = 60;
    /** 接单后聊完的期限：7 天毫秒数（自 takenAt 起算） */
    public static final long WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 许愿的人（区分大小写） */
    private String fromUser;
    /** 希望多聊的话题 */
    private String title;
    /** PENDING 待接 / TAKEN 已接单 / TALKED 聊完了 */
    private String status;
    /** 接单的人=对方（区分大小写）；null=没人接 */
    private String takenBy;
    /** 接单时间毫秒；一周期限从此起算 */
    private Long takenAt;
    /** 聊完的日期 yyyy-MM-dd；空串=还没聊 */
    private String talkDay;
    /** 一句感想；空串=没写 */
    private String reflect;
    /** 接单后超一周才聊完记一笔（1=超时） */
    private Integer overdue;
    private Long created;
    private Long updatedAt;

    public static CoupleCatchTopic of(String spaceId, String fromUser, String title) {
        CoupleCatchTopic row = new CoupleCatchTopic();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.title = title;
        row.status = STATUS_PENDING;
        row.takenBy = null;
        row.takenAt = null;
        row.talkDay = "";
        row.reflect = "";
        row.overdue = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 还没人接单。 */
    public boolean pending() {
        return STATUS_PENDING.equals(status);
    }

    /** 已接单、还没聊完（在途）。 */
    public boolean taken() {
        return STATUS_TAKEN.equals(status);
    }

    /** 已经聊完。 */
    public boolean talked() {
        return STATUS_TALKED.equals(status);
    }

    /** 对方接单：写接单人与时间，翻 TAKEN（一周期限从此刻起算）。 */
    public void take(String by) {
        takenBy = by;
        takenAt = System.currentTimeMillis();
        status = STATUS_TAKEN;
        updatedAt = takenAt;
    }

    /** 聊完销单：翻 TALKED，回填聊完日与感想；overdueFlag=true 记一笔超时。 */
    public void talkDone(String day, String reflectText, boolean overdueFlag) {
        status = STATUS_TALKED;
        talkDay = day == null ? "" : day;
        reflect = reflectText == null ? "" : reflectText;
        overdue = overdueFlag ? 1 : 0;
        updatedAt = System.currentTimeMillis();
    }
}
