package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F382 安全词使用记录：哪天用的 + 事后一句复盘（uk(space,day,user_name) 一天一人一次）。
 * 复盘口径：登记时空串，事后可补；reflected() 只判「有没有写」，补写不重推由服务层管。
 */
@Data
@TableName("couple_catch_safeword_use")
public class CoupleCatchSafewordUse {

    /** 事后复盘字数上限（列 varchar(240) 已按 4 倍宽度放宽） */
    public static final int REFLECT_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 使用日 yyyy-MM-dd */
    private String day;
    /** 喊了暂停的人（区分大小写） */
    private String userName;
    /** 事后一句复盘；空串=还没补 */
    private String reflect;
    private Long created;
    private Long updatedAt;

    public static CoupleCatchSafewordUse of(String spaceId, String day, String userName) {
        CoupleCatchSafewordUse row = new CoupleCatchSafewordUse();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.userName = userName;
        row.reflect = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 复盘是否已经写了（非空非空白）。 */
    public boolean reflected() {
        return reflect != null && !reflect.isBlank();
    }
}
