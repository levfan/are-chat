package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F315 身体不适 SOS：在途一条，TA 从话术卡里选「我能做」。 */
@Data
@TableName("couple_body_sos")
public class CoupleBodySos {

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_HELD = "HELD";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String symptom;
    private String since;
    private String status;
    private String comfort;
    private String holdBy;
    private Long created;
    private Long updatedAt;

    public static CoupleBodySos of(String spaceId, String fromUser, String symptom, String since) {
        CoupleBodySos row = new CoupleBodySos();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.symptom = symptom;
        row.since = since == null ? "" : since;
        row.status = STATUS_SENT;
        row.comfort = "";
        row.holdBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
