package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F273 代拿快递：下单-接单-送达，送达插 2 分感谢章。 */
@Data
@TableName("couple_parcel")
public class CoupleParcel {

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_GRABBED = "GRABBED";
    public static final String STATUS_DONE = "DONE";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String note;
    private String fromUser;
    private String status;
    private String grabber;
    private Long doneAt;
    private Long created;
    private Long updatedAt;

    public static CoupleParcel of(String spaceId, String note, String fromUser) {
        CoupleParcel row = new CoupleParcel();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.note = note == null ? "" : note;
        row.fromUser = fromUser;
        row.status = STATUS_SENT;
        row.grabber = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
