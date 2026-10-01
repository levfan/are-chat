package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** F321 道歉质检：六要素自评 + 对方验货，退回重写或进陈列室。 */
@Data
@TableName("couple_sorry_review")
public class CoupleSorryReview {

    public static final String STATUS_VERIFY = "VERIFY";
    public static final String STATUS_PASSED = "PASSED";
    public static final String STATUS_BACK = "BACK";

    public static final List<String> POINTS = List.of("FACT", "FEEL", "BLAME", "SORRY", "FIX", "ASK");

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String letter;
    private String points;
    private String status;
    private String verdict;
    private String verifiedBy;
    private Long created;
    private Long updatedAt;

    public static CoupleSorryReview of(String spaceId, String fromUser, String letter, String points) {
        CoupleSorryReview row = new CoupleSorryReview();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.letter = letter;
        row.points = points == null ? "" : points;
        row.status = STATUS_VERIFY;
        row.verdict = "";
        row.verifiedBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public int pointCount() {
        if (points == null || points.isEmpty()) {
            return 0;
        }
        return (int) java.util.Arrays.stream(points.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).count();
    }

    public boolean hasPoint(String p) {
        return points != null && ("," + points + ",").contains("," + p + ",");
    }
}
