package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F348 周年抽奖箱：奖池来自当年迷你愿望，周年当天双方各抽一次。 */
@Data
@TableName("couple_legacy_draw")
public class CoupleLegacyDraw {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String year;
    private String prizeA;
    private String prizeB;
    private Integer drawnA;
    private Integer drawnB;
    private Integer notified;
    private Long created;
    private Long updatedAt;

    public static CoupleLegacyDraw of(String spaceId, String year) {
        CoupleLegacyDraw row = new CoupleLegacyDraw();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.year = year;
        row.prizeA = "";
        row.prizeB = "";
        row.drawnA = 0;
        row.drawnB = 0;
        row.notified = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean drawnAFlag() {
        return drawnA != null && drawnA == 1;
    }

    public boolean drawnBFlag() {
        return drawnB != null && drawnB == 1;
    }

    public boolean notifiedFlag() {
        return notified != null && notified == 1;
    }
}
