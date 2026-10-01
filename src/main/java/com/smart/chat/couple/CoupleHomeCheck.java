package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F279 家安月检：每月各交一份六项勾选，双人才算检完。 */
@Data
@TableName("couple_home_check")
public class CoupleHomeCheck {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String month;
    private String fromUser;
    private String items;
    private Long created;
    private Long updatedAt;

    public static CoupleHomeCheck of(String spaceId, String month, String fromUser, String items) {
        CoupleHomeCheck row = new CoupleHomeCheck();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.month = month;
        row.fromUser = fromUser;
        row.items = items;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
