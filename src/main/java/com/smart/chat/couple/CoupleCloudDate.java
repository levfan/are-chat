package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 云约会清单（F116）：异地也能一起做的事，完成打卡。 */
@Data
@TableName("couple_cloud_date")
public class CoupleCloudDate {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";
    public static final int ITEM_MAX = 100;
    public static final int NOTE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String item;
    private String status;
    private String doneNote;
    private Long doneAt;
    private Long created;

    public static CoupleCloudDate of(String spaceId, String fromUser, String item) {
        CoupleCloudDate row = new CoupleCloudDate();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.item = item;
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        return row;
    }
}
