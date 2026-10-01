package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F326 底线声明卡：各写 3 条底线 + 生效日，被踩要补红线记录。 */
@Data
@TableName("couple_bottom_line")
public class CoupleBottomLine {

    public static final int SLOT_MAX = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private Integer slot;
    private String text;
    private String sinceDay;
    private Integer breachCount;
    private String breachNote;
    private Long created;
    private Long updatedAt;

    public static CoupleBottomLine of(String spaceId, String fromUser, int slot, String text, String sinceDay) {
        CoupleBottomLine row = new CoupleBottomLine();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.slot = slot;
        row.text = text;
        row.sinceDay = sinceDay == null ? "" : sinceDay;
        row.breachCount = 0;
        row.breachNote = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
