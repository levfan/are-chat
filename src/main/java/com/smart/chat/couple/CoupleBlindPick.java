package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 周末盲选（F135）：双方各写 3 个周末愿望，都提交后配对结算。 */
@Data
@TableName("couple_blind_pick")
public class CoupleBlindPick {

    public static final int PICKS_MAX = 300;
    public static final int PICK_COUNT = 3;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    /** 所属周（周一日期 yyyy-MM-dd） */
    private String week;
    private String picks;
    private Long created;

    public static CoupleBlindPick of(String spaceId, String fromUser, String week, String picks) {
        CoupleBlindPick row = new CoupleBlindPick();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.week = week;
        row.picks = picks;
        row.created = System.currentTimeMillis();
        return row;
    }
}
