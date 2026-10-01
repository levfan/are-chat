package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F313 戒烟戒糖互助营：营期天数 + 破戒留痕 + 陪绑方安慰词。 */
@Data
@TableName("couple_body_quit")
public class CoupleBodyQuit {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_DONE = "DONE";
    public static final String STATUS_GONE = "GONE";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String name;
    private String ownerUser;
    private Integer targetDays;
    private String startDay;
    private String brokeDays;
    private String cheer;
    private String cheerBy;
    private String status;
    private Long created;
    private Long updatedAt;

    public static CoupleBodyQuit of(String spaceId, String name, String ownerUser, int targetDays, String startDay) {
        CoupleBodyQuit row = new CoupleBodyQuit();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.name = name;
        row.ownerUser = ownerUser;
        row.targetDays = targetDays;
        row.startDay = startDay;
        row.brokeDays = "";
        row.cheer = "";
        row.cheerBy = "";
        row.status = STATUS_OPEN;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public int brokeCount() {
        if (brokeDays == null || brokeDays.isEmpty()) {
            return 0;
        }
        return (int) java.util.Arrays.stream(brokeDays.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).count();
    }

    public boolean hasBroke(String day) {
        return brokeDays != null && ("," + brokeDays + ",").contains("," + day + ",");
    }
}
