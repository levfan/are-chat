package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Arrays;
import java.util.UUID;

/**
 * F376 低谷通行证：TA 宣布最近状态不好（7-30 天窗口），对方每天递一张「不说话也行」卡。
 * 递卡口径：care_days 存 MMdd 逗号分隔 CSV，按逗号精确匹配判重；张数上限由服务层挡。
 * 回升口径：status 由宣布进低谷的人自己定（LOW 在谷底 / UP 已回升），revive_day 记实际回升日。
 */
@Data
@TableName("couple_quest_valley")
public class CoupleQuestValley {

    public static final String STATUS_LOW = "LOW";
    public static final String STATUS_UP = "UP";
    /** 低谷窗口最短天数（服务层校验） */
    public static final int SPAN_MIN = 7;
    /** 低谷窗口最长天数（服务层校验） */
    public static final int SPAN_MAX = 30;
    /** 递卡 CSV 条数上限（服务层校验，超出不收） */
    public static final int CARE_CSV_MAX = 40;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 宣布进低谷的人（区分大小写，在途每人 ≤1） */
    private String fromUser;
    /** 宣布日 yyyy-MM-dd */
    private String openDay;
    /** 预计回升日 yyyy-MM-dd（7-30 天内） */
    private String untilDay;
    /** LOW 在谷底 / UP 已回升 */
    private String status;
    /** 已递卡日 CSV，MMdd 逗号分隔；空串=一张没递 */
    private String careDays;
    /** 实际宣布回升的日子 yyyy-MM-dd；空串=还没回升 */
    private String reviveDay;
    private Long created;
    private Long updatedAt;

    public static CoupleQuestValley of(String spaceId, String fromUser, String openDay, String untilDay) {
        CoupleQuestValley row = new CoupleQuestValley();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.openDay = openDay;
        row.untilDay = untilDay;
        row.status = STATUS_LOW;
        row.careDays = "";
        row.reviveDay = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 是否还在谷底。 */
    public boolean low() {
        return STATUS_LOW.equals(status);
    }

    /** 某日（MMdd）是否已递过卡：CSV 按逗号精确匹配，不做子串误判。 */
    public boolean cared(String mmdd) {
        return careDays != null && ("," + careDays + ",").contains("," + mmdd + ",");
    }

    /** 追加一张「不说话也行」卡（MMdd）：已递过那天就不再重复追加；条数上限由服务层挡（CARE_CSV_MAX）。 */
    public void care(String mmdd) {
        if (cared(mmdd)) {
            return;
        }
        careDays = careDays == null || careDays.isEmpty() ? mmdd : careDays + "," + mmdd;
        updatedAt = System.currentTimeMillis();
    }

    /** 已递卡张数（CSV 非空token 计数）。 */
    public int careCount() {
        if (careDays == null || careDays.isEmpty()) {
            return 0;
        }
        return (int) Arrays.stream(careDays.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).count();
    }

    /** 本人宣布回升：置 UP 并写实际回升日（同时刷新更新时间）。 */
    public void rise(String day) {
        status = STATUS_UP;
        reviveDay = day;
        updatedAt = System.currentTimeMillis();
    }
}
