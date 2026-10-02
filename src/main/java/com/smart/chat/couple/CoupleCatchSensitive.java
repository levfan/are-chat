package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * F383 敏感日历：给 TA 的敏感日提前标注 + 当天想被怎样对待，前 1 天提醒我。
 * 归属口径：ownerUser 是「这个日子属于谁」，行由对方代为标注；kind 白名单在服务层校验。
 */
@Data
@TableName("couple_catch_sensitive")
public class CoupleCatchSensitive {

    public static final String KIND_PERIOD = "PERIOD";
    public static final String KIND_CHECK = "CHECK";
    public static final String KIND_MEMORY = "MEMORY";
    public static final String KIND_OTHER = "OTHER";
    public static final List<String> KINDS = List.of(KIND_PERIOD, KIND_CHECK, KIND_MEMORY, KIND_OTHER);
    /** 当天想被怎样对待字数上限（列 varchar(240) 已按 4 倍宽度放宽） */
    public static final int CARE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 这个日子属于谁（区分大小写，由对方代为标注） */
    private String ownerUser;
    /** 敏感日 yyyy-MM-dd */
    private String day;
    /** PERIOD/CHECK/MEMORY/OTHER（服务层白名单） */
    private String kind;
    /** 当天想被怎样对待；空串=没说 */
    private String care;
    private Long created;
    private Long updatedAt;

    public static CoupleCatchSensitive of(String spaceId, String ownerUser, String day,
                                          String kind, String care) {
        CoupleCatchSensitive row = new CoupleCatchSensitive();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.ownerUser = ownerUser;
        row.day = day;
        row.kind = kind;
        row.care = care == null ? "" : care;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
