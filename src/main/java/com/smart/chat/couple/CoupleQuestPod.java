package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Arrays;
import java.util.UUID;

/**
 * F374 考试周静音舱：TA 入舱到某日为止，对方期间只发加油卡（每日一张），
 * 出舱后提醒补一封长信。
 * 加油卡口径：cheers 存 MMdd 逗号分隔 CSV，按逗号精确匹配判重；张数上限由服务层挡。
 */
@Data
@TableName("couple_quest_pod")
public class CoupleQuestPod {

    public static final String STATUS_IN = "IN";
    public static final String STATUS_OUT = "OUT";
    /** 加油卡 CSV 条数上限（服务层校验，超出不收） */
    public static final int CHEER_CSV_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 入舱的人（区分大小写） */
    private String fromUser;
    /** 入舱日 yyyy-MM-dd */
    private String startDay;
    /** 出舱日 yyyy-MM-dd（晚于入舱日） */
    private String untilDay;
    /** IN 在舱 / OUT 已出舱 */
    private String status;
    /** 已发加油卡日 CSV，MMdd 逗号分隔；空串=一张没发 */
    private String cheers;
    /** 出舱后的长信是否已补（1=已补） */
    private Integer letterDone;
    private Long created;
    private Long updatedAt;

    public static CoupleQuestPod of(String spaceId, String fromUser, String startDay, String untilDay) {
        CoupleQuestPod row = new CoupleQuestPod();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.startDay = startDay;
        row.untilDay = untilDay;
        row.status = STATUS_IN;
        row.cheers = "";
        row.letterDone = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 是否还在舱里。 */
    public boolean in() {
        return STATUS_IN.equals(status);
    }

    /** 某日（MMdd）是否已发过加油卡：CSV 按逗号精确匹配，不做子串误判。 */
    public boolean cheered(String mmdd) {
        return cheers != null && ("," + cheers + ",").contains("," + mmdd + ",");
    }

    /** 追加一张加油卡（MMdd）：已发过那天就不再重复追加；条数上限由服务层挡（CHEER_CSV_MAX）。 */
    public void cheer(String mmdd) {
        if (cheered(mmdd)) {
            return;
        }
        cheers = cheers == null || cheers.isEmpty() ? mmdd : cheers + "," + mmdd;
        updatedAt = System.currentTimeMillis();
    }

    /** 已发加油卡张数（CSV 非空token 计数）。 */
    public int cheerCount() {
        if (cheers == null || cheers.isEmpty()) {
            return 0;
        }
        return (int) Arrays.stream(cheers.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).count();
    }

    /** 出舱：置 OUT（同时刷新更新时间）。 */
    public void out() {
        status = STATUS_OUT;
        updatedAt = System.currentTimeMillis();
    }
}
