package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** 恋爱盲盒（F51）：把一句话/小任务装进盒子，对方到指定日子才能拆——制造一天的期待。 */
@Data
@TableName("couple_mystery_box")
public class CoupleMysteryBox {

    public static final String KIND_WHISPER = "whisper";
    public static final String KIND_TASK = "task";
    public static final int CONTENT_MAX = 300;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    /** whisper 悄悄话 / task 小任务 */
    private String kind;
    private String content;
    /** 可拆日期（yyyy-MM-dd，最早明天） */
    private String openDay;
    private boolean opened;
    private Long openedAt;
    private Long created;

    public static CoupleMysteryBox of(String spaceId, String fromUser, String kind, String content,
                                      LocalDate openDay) {
        CoupleMysteryBox row = new CoupleMysteryBox();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.kind = kind;
        row.content = content;
        row.openDay = openDay.toString();
        row.opened = false;
        row.created = System.currentTimeMillis();
        return row;
    }

    public static boolean isValidKind(String kind) {
        return KIND_WHISPER.equals(kind) || KIND_TASK.equals(kind);
    }
}
