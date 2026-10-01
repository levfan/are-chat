package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F305 每日奥斯卡：一人一天一次提名，附一句话演技证据。 */
@Data
@TableName("couple_act_award")
public class CoupleActAward {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String aboutUser;
    private String evidence;
    private Long created;

    public static CoupleActAward of(String spaceId, String day, String fromUser, String aboutUser, String evidence) {
        CoupleActAward row = new CoupleActAward();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.aboutUser = aboutUser;
        row.evidence = evidence;
        row.created = System.currentTimeMillis();
        return row;
    }
}
