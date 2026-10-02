package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F373 生病陪护单：TA 生病开单，陪护人代记喝水/吃药（见 F373 陪护代记表），
 * 痊愈日关单并留一句病中留言。
 * 在途口径：status=OPEN 的行按 patientUser 计数，每人同时 ≤1。
 */
@Data
@TableName("couple_quest_nurse")
public class CoupleQuestNurse {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_CLOSED = "CLOSED";
    /** 症状字数上限（列 varchar(180) 已按 3 倍宽度放宽） */
    public static final int SYMPTOM_MAX = 60;
    /** 病中留言字数上限（列 varchar(240) 已按 3 倍宽度放宽） */
    public static final int MESSAGE_MAX = 80;
    /** 在途（OPEN）每人同时最多 1 单 */
    public static final int IN_FLIGHT_MAX = 1;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 生病的人（区分大小写） */
    private String patientUser;
    /** 陪护的人=对方 */
    private String carerUser;
    /** 开单日 yyyy-MM-dd */
    private String openDay;
    /** 痊愈关单日 yyyy-MM-dd；空串=还在陪护 */
    private String closeDay;
    /** 症状一句话；空串=没写 */
    private String symptom;
    /** 陪护人的病中留言；空串=没写 */
    private String message;
    /** OPEN 陪护中 / CLOSED 已关单 */
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleQuestNurse of(String spaceId, String patientUser, String carerUser,
                                      String openDay, String symptom) {
        CoupleQuestNurse row = new CoupleQuestNurse();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.patientUser = patientUser;
        row.carerUser = carerUser;
        row.openDay = openDay;
        row.closeDay = "";
        row.symptom = symptom;
        row.message = "";
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 是否还在陪护（未关单）。 */
    public boolean open() {
        return STATUS_OPEN.equals(status);
    }

    /** 关单：置 CLOSED 并写关单日；message 传 null 表示保持原有病中留言不动，非 null 才覆盖。 */
    public void close(String day, String message) {
        status = STATUS_CLOSED;
        closeDay = day;
        if (message != null) {
            this.message = message;
        }
        updatedAt = System.currentTimeMillis();
    }
}
