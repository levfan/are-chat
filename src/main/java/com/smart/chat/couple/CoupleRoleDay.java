package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F300 今日身份签：同日同身份，日终双方各评 1-5。 */
@Data
@TableName("couple_role_day")
public class CoupleRoleDay {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String roleName;
    private String guide;
    private Integer rateA;
    private Integer rateB;
    private Long created;

    public static CoupleRoleDay of(String spaceId, String day, String roleName, String guide) {
        CoupleRoleDay row = new CoupleRoleDay();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.roleName = roleName;
        row.guide = guide;
        row.created = System.currentTimeMillis();
        return row;
    }
}
