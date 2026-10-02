package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F388 今日一句话：每天给对方留一句想说的话（≤40 字）。
 * 一人一天一行（uk(space,day,user_name)），当天可改写；今天没说时回看最近一句。
 */
@Data
@TableName("couple_catch_daily")
public class CoupleCatchDaily {

    /** 一句话字数上限（列 varchar(160) 已按 4 倍宽度放宽） */
    public static final int CONTENT_MAX = 40;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 哪一天 yyyy-MM-dd */
    private String day;
    /** 说话的人（区分大小写） */
    private String userName;
    /** 今日一句话 */
    private String content;
    private Long created;
    private Long updatedAt;

    public static CoupleCatchDaily of(String spaceId, String day, String userName, String content) {
        CoupleCatchDaily row = new CoupleCatchDaily();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.userName = userName;
        row.content = content == null ? "" : content;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
