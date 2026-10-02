package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F384 「说到哪了」：被打断的话题先存档，续完了销档。
 * 在途口径：status=OPEN 的行按 fromUser 计数，每人同时 ≤5；
 * 建表脚本按 idx_catch_thread 建普通索引（同一 (space,from_user,OPEN) 允许多行），
 * 重复内容由服务层按 (space,from_user,topic) 查重挡。
 */
@Data
@TableName("couple_catch_thread")
public class CoupleCatchThread {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";
    /** 话题一句话字数上限（列 varchar(160) 已按 4 倍宽度放宽） */
    public static final int TOPIC_MAX = 40;
    /** 说到哪了字数上限（列 varchar(240) 已按 4 倍宽度放宽） */
    public static final int PROGRESS_MAX = 60;
    /** 在途（OPEN）每人同时最多 5 条 */
    public static final int IN_FLIGHT_MAX = 5;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 存档的人（区分大小写） */
    private String fromUser;
    /** 话题一句话 */
    private String topic;
    /** 说到哪了；空串=没记 */
    private String progress;
    /** OPEN 在途 / DONE 已续完 */
    private String status;
    /** 销档时间毫秒；null=还在途 */
    private Long doneAt;
    private Long created;
    private Long updatedAt;

    public static CoupleCatchThread of(String spaceId, String fromUser, String topic, String progress) {
        CoupleCatchThread row = new CoupleCatchThread();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.topic = topic;
        row.progress = progress == null ? "" : progress;
        row.status = STATUS_OPEN;
        row.doneAt = null;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 是否还在途（没续完）。 */
    public boolean open() {
        return STATUS_OPEN.equals(status);
    }

    /** 续完了：翻 DONE 并记销档时间（同时刷新更新时间）。 */
    public void finish() {
        status = STATUS_DONE;
        doneAt = System.currentTimeMillis();
        updatedAt = doneAt;
    }
}
