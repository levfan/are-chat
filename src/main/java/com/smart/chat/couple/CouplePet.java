package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 守护兽（F129）：双人共同养的守护兽，心情按最近照料时间惰性衰减。 */
@Data
@TableName("couple_pet")
public class CouplePet {

    public static final String KIND_FOX = "FOX";
    public static final String KIND_CAT = "CAT";
    public static final String KIND_BEAR = "BEAR";
    public static final String KIND_BUNNY = "BUNNY";
    public static final int NAME_MAX = 20;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String name;
    private String kind;
    private Integer careCount;
    private Long lastCareAt;
    private Long created;

    public static CouplePet of(String spaceId, String name, String kind) {
        CouplePet row = new CouplePet();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.name = name;
        row.kind = kind;
        row.careCount = 0;
        row.created = System.currentTimeMillis();
        return row;
    }
}
