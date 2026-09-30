package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 道歉三部曲（F107）：引导式道歉卡（我错了→错在哪→以后我会），对方收下即结。 */
@Data
@TableName("couple_apology_card")
public class CoupleApologyCard {

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final int SECTION_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String whatWrong;
    private String whyWrong;
    private String willDo;
    private String status;
    private Long acceptedAt;
    private Long created;

    public static CoupleApologyCard of(String spaceId, String fromUser,
                                       String whatWrong, String whyWrong, String willDo) {
        CoupleApologyCard row = new CoupleApologyCard();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.whatWrong = whatWrong;
        row.whyWrong = whyWrong;
        row.willDo = willDo;
        row.status = STATUS_SENT;
        row.created = System.currentTimeMillis();
        return row;
    }
}
