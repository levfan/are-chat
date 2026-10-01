package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F276 久坐互拍：一人一天一拍，双方间隔≤1h 记同起。 */
@Data
@TableName("couple_standup")
public class CoupleStandup {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private Long tappedAt;
    private Integer paired;
    private Long created;

    public static CoupleStandup of(String spaceId, String day, String fromUser) {
        CoupleStandup row = new CoupleStandup();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.tappedAt = System.currentTimeMillis();
        row.paired = 0;
        row.created = row.tappedAt;
        return row;
    }

    public boolean isPaired() {
        return Integer.valueOf(1).equals(paired);
    }
}
