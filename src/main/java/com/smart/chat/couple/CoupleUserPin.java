package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 常用收藏（F207）：每人 pin ≤6 个功能卡键，页签顶部「我的常用」用。 */
@Data
@TableName("couple_user_pin")
public class CoupleUserPin {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    /** 功能卡键，逗号分隔 */
    private String pins;
    private Long updatedAt;
    private Long created;

    public static CoupleUserPin of(String spaceId, String fromUser, String pins) {
        CoupleUserPin row = new CoupleUserPin();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.pins = pins;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
