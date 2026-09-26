package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 每日小仪式打卡：MORNING 早安 / NIGHT 晚安，按自然日（yyyy-MM-dd）去重。
 * 双方都完成当日同类打卡 → 解锁当日专属背景/贴纸；晚安连续天数用于计算 streak。
 */
@Data
@TableName("couple_checkin")
public class CoupleCheckin {

    public static final String KIND_MORNING = "MORNING";
    public static final String KIND_NIGHT = "NIGHT";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String username;
    private String kind;
    /** 打卡日期 yyyy-MM-dd */
    private String checkinDay;
    private Long created;

    public static CoupleCheckin of(String spaceId, String username, String kind, String day) {
        CoupleCheckin checkin = new CoupleCheckin();
        checkin.id = UUID.randomUUID().toString();
        checkin.spaceId = spaceId;
        checkin.username = username;
        checkin.kind = kind;
        checkin.checkinDay = day;
        checkin.created = System.currentTimeMillis();
        return checkin;
    }
}
