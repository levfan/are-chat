package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F288 口味变迁：「以前不爱现在爱」，攒成口味演化时间线。 */
@Data
@TableName("couple_taste_shift")
public class CoupleTasteShift {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String thing;
    private String beforeText;
    private String nowText;
    private String shiftedDay;
    private String fromUser;
    private Long created;
    private Long updatedAt;

    public static CoupleTasteShift of(String spaceId, String thing, String beforeText,
                                      String nowText, String shiftedDay, String fromUser) {
        CoupleTasteShift row = new CoupleTasteShift();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.thing = thing;
        row.beforeText = beforeText == null ? "" : beforeText;
        row.nowText = nowText == null ? "" : nowText;
        row.shiftedDay = shiftedDay == null ? "" : shiftedDay;
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
