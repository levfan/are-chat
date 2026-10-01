package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 隔空牵手（F110）：同一天双方都点亮「牵手」即成功，累计牵手日子。 */
@Data
@TableName("couple_handhold")
public class CoupleHandhold {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private Integer holdA;
    private Integer holdB;
    private Long holdAtA;
    private Long holdAtB;
    private Long created;

    public static CoupleHandhold of(String spaceId, String day) {
        CoupleHandhold row = new CoupleHandhold();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.holdA = 0;
        row.holdB = 0;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean bothHold() {
        return Integer.valueOf(1).equals(holdA) && Integer.valueOf(1).equals(holdB);
    }
}
