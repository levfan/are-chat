package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F338 群聊记者：每日一条「今天群里最好笑的是我们…」互递素材。 */
@Data
@TableName("couple_world_group_report")
public class CoupleWorldGroupReport {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String lineA;
    private String lineB;
    private Integer laughA;
    private Integer laughB;
    private Long created;
    private Long updatedAt;

    public static CoupleWorldGroupReport of(String spaceId, String day) {
        CoupleWorldGroupReport row = new CoupleWorldGroupReport();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.lineA = "";
        row.lineB = "";
        row.laughA = 0;
        row.laughB = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean isLaughA() {
        return laughA != null && laughA == 1;
    }

    public boolean isLaughB() {
        return laughB != null && laughB == 1;
    }
}
