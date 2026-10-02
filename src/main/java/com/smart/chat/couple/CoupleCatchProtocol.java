package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * F386 聆听方式协议：各写「我难过时要的是」五选一 + 补充说明。
 * 每人一行可改写（uk(space,from_user)），mode 白名单在服务层校验。
 */
@Data
@TableName("couple_catch_protocol")
public class CoupleCatchProtocol {

    public static final String MODE_REASON = "REASON";
    public static final String MODE_RANT = "RANT";
    public static final String MODE_HUG = "HUG";
    public static final String MODE_FOOD = "FOOD";
    public static final String MODE_SPACE = "SPACE";
    public static final List<String> MODES =
            List.of(MODE_REASON, MODE_RANT, MODE_HUG, MODE_FOOD, MODE_SPACE);
    /** 补充说明字数上限（列 varchar(240) 已按 4 倍宽度放宽） */
    public static final int NOTE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 写协议的人（区分大小写，每人一行可改写） */
    private String fromUser;
    /** REASON 讲道理/RANT 陪骂/HUG 抱抱不说话/FOOD 递吃的/SPACE 别理我（服务层白名单） */
    private String mode;
    /** 补充说明；空串=没说 */
    private String note;
    private Long created;
    private Long updatedAt;

    public static CoupleCatchProtocol of(String spaceId, String fromUser, String mode, String note) {
        CoupleCatchProtocol row = new CoupleCatchProtocol();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.mode = mode;
        row.note = note == null ? "" : note;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
