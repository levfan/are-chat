package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 月度互评（F183）：每月给这段关系打星+建议，双评互见。 */
@Data
@TableName("couple_month_review")
public class CoupleMonthReview {

    public static final int STARS_MIN = 1;
    public static final int STARS_MAX = 5;
    public static final int ADVICE_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 所属月份（yyyy-MM） */
    private String month;
    private String fromUser;
    private Integer stars;
    private String advice;
    private Long updatedAt;
    private Long created;

    public static CoupleMonthReview of(String spaceId, String month, String fromUser, int stars, String advice) {
        CoupleMonthReview row = new CoupleMonthReview();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.month = month;
        row.fromUser = fromUser;
        row.stars = stars;
        row.advice = advice == null ? "" : advice;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
