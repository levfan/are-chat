package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F292 总得有一天拍卖：上拍-7 天认领-排期-完成，逾期下架。 */
@Data
@TableName("couple_someday")
public class CoupleSomeday {

    public static final int SHELF_DAYS = 7;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String thing;
    private String ownerUser;
    private String status;
    private String takenBy;
    private String scheduledDay;
    private Long doneAt;
    private Long created;
    private Long updatedAt;

    public static CoupleSomeday of(String spaceId, String thing, String ownerUser) {
        CoupleSomeday row = new CoupleSomeday();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.thing = thing;
        row.ownerUser = ownerUser;
        row.status = "SHELF";
        row.takenBy = "";
        row.scheduledDay = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
