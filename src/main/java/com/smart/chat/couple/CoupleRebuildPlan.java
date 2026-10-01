package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F323 信任重建 30 天：每日任务卡 + 双方签 + 周复盘，断签可续。 */
@Data
@TableName("couple_rebuild_plan")
public class CoupleRebuildPlan {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_GIVENUP = "GIVENUP";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String name;
    private String fromUser;
    private String cause;
    private String startDay;
    private Integer targetDays;
    private String tasks;
    private String signedDays;
    private String review;
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleRebuildPlan of(String spaceId, String name, String fromUser, String cause,
                                       String startDay, int targetDays, String tasksCsv) {
        CoupleRebuildPlan row = new CoupleRebuildPlan();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.name = name;
        row.fromUser = fromUser;
        row.cause = cause == null ? "" : cause;
        row.startDay = startDay;
        row.targetDays = targetDays;
        row.tasks = tasksCsv == null ? "" : tasksCsv;
        row.signedDays = "";
        row.review = "";
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public int taskCount() {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        return (int) java.util.Arrays.stream(tasks.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).count();
    }


}
