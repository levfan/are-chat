package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F319 早睡军令状：本周熄灯线各报一条，双签才生效，违约率读 F220 熄灯数据。 */
@Data
@TableName("couple_body_oath")
public class CoupleBodyOath {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String week;
    private String lineA;
    private String lineB;
    private Integer signedA;
    private Integer signedB;
    private Long created;
    private Long updatedAt;

    public static CoupleBodyOath of(String spaceId, String week) {
        CoupleBodyOath row = new CoupleBodyOath();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.week = week;
        row.lineA = "";
        row.lineB = "";
        row.signedA = 0;
        row.signedB = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 双人才算立状成立。 */
    public boolean isBothSigned() {
        return signedA != null && signedA == 1 && signedB != null && signedB == 1;
    }

    /** userA 是否已签。 */
    public boolean signedAFlag() {
        return signedA != null && signedA == 1;
    }

    /** userB 是否已签。 */
    public boolean signedBFlag() {
        return signedB != null && signedB == 1;
    }
}
