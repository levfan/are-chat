package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F301 一日互换日记：各写一页「作为 TA」，次日双齐互见。 */
@Data
@TableName("couple_swap_diary")
public class CoupleSwapDiary {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String text;
    private Long created;
    private Long updatedAt;

    public static CoupleSwapDiary of(String spaceId, String day, String fromUser, String text) {
        CoupleSwapDiary row = new CoupleSwapDiary();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.text = text;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
