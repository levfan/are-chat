package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 藏宝图任务（F58）：给 TA 布置一个现实里的小任务，完成后才揭晓藏起来的「宝藏」。 */
@Data
@TableName("couple_treasure")
public class CoupleTreasure {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DONE = "DONE";
    public static final int TASK_MAX = 100;
    public static final int PRIZE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 埋宝人 */
    private String fromUser;
    private String taskText;
    /** 宝藏内容（完成后才揭晓） */
    private String prizeText;
    private String status;
    private Long doneAt;
    private Long created;

    public static CoupleTreasure of(String spaceId, String fromUser, String taskText, String prizeText) {
        CoupleTreasure row = new CoupleTreasure();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.taskText = taskText;
        row.prizeText = prizeText;
        row.status = STATUS_PENDING;
        row.created = System.currentTimeMillis();
        return row;
    }
}
