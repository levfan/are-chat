package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 「如果」问答（F173）：每天一道脑洞题，双答互见，先答者今日默契之星。 */
@Data
@TableName("couple_what_if")
public class CoupleWhatIf {

    public static final int ANSWER_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String answer;
    private Long updatedAt;
    private Long created;

    public static CoupleWhatIf of(String spaceId, String day, String fromUser, String answer) {
        long now = System.currentTimeMillis();
        CoupleWhatIf row = new CoupleWhatIf();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.answer = answer;
        row.updatedAt = now;
        row.created = now;
        return row;
    }
}
