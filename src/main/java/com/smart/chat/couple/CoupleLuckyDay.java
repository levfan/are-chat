package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F252 择吉日：给大事挑一个我们的吉日，发起+对方双盖章。 */
@Data
@TableName("couple_lucky_day")
public class CoupleLuckyDay {

    public static final int MATTER_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String matter;
    private String fromUser;
    private Integer confirmed;
    private String confirmedBy;
    private String comment;
    private Long created;

    public static CoupleLuckyDay of(String spaceId, String day, String matter, String fromUser, String comment) {
        CoupleLuckyDay row = new CoupleLuckyDay();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.matter = matter;
        row.fromUser = fromUser;
        row.confirmed = 0;
        row.confirmedBy = "";
        row.comment = comment == null ? "" : comment;
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean confirmedFlag() {
        return Integer.valueOf(1).equals(confirmed);
    }
}
