package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F333 官宣日：每月一张纯文字官宣卡，成官宣编年。 */
@Data
@TableName("couple_world_declare")
public class CoupleWorldDeclare {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String month;
    private String text;
    private String fromUser;
    private Long created;

    public static CoupleWorldDeclare of(String spaceId, String month, String text, String fromUser) {
        CoupleWorldDeclare row = new CoupleWorldDeclare();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.month = month;
        row.text = text;
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();
        return row;
    }
}
