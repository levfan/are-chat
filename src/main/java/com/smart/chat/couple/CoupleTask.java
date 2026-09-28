package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 甜蜜任务卡：每天为双方各随机生成一个小任务（夸夸对方/分享小事…），完成打卡积累心动值。
 * 每人每天一张（空间+人+自然日唯一），重复拉取返回同一张。
 */
@Data
@TableName("couple_task")
public class CoupleTask {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DONE = "DONE";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 任务日期（yyyy-MM-dd） */
    private String taskDay;
    /** 任务归属人用户名 */
    private String username;
    private String content;
    private String status;
    private Long doneAt;
    private Long created;

    public static CoupleTask of(String spaceId, String taskDay, String username, String content) {
        CoupleTask task = new CoupleTask();
        task.id = UUID.randomUUID().toString();
        task.spaceId = spaceId;
        task.taskDay = taskDay;
        task.username = username;
        task.content = content;
        task.status = STATUS_PENDING;
        task.created = System.currentTimeMillis();
        return task;
    }
}
