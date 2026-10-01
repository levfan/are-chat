package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F302 师徒日：一周一卦，侍奉打卡与出师定级。 */
@Data
@TableName("couple_master_day")
public class CoupleMasterDay {

    public static final int SERVE_TARGET = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String masterUser;
    private String apprenticeUser;
    private String serves;
    private String review;
    private String grade;
    private Long created;
    private Long updatedAt;

    public static CoupleMasterDay of(String spaceId, String week, String masterUser, String apprenticeUser) {
        CoupleMasterDay row = new CoupleMasterDay();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.masterUser = masterUser;
        row.apprenticeUser = apprenticeUser;
        row.serves = "";
        row.review = "";
        row.grade = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public int serveCount() {
        if (serves == null || serves.isEmpty()) {
            return 0;
        }
        return (int) java.util.Arrays.stream(serves.split(",")).map(String::trim).filter(s -> !s.isEmpty()).count();
    }

    public boolean hasServed(String day) {
        return serves != null && (("," + serves + ",").contains("," + day + ","));
    }
}
