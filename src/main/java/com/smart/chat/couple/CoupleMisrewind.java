package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F262 误会倒带卡：同日同主题双方各写「我当时以为/我猜你其实想」。 */
@Data
@TableName("couple_misrewind")
public class CoupleMisrewind {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String topic;
    private String day;
    private String fromUser;
    private String mine;
    private String theirs;
    private Long created;

    public static CoupleMisrewind of(String spaceId, String day, String topic, String fromUser,
                                     String mine, String theirs) {
        CoupleMisrewind row = new CoupleMisrewind();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.topic = topic;
        row.fromUser = fromUser;
        row.mine = mine;
        row.theirs = theirs;
        row.created = System.currentTimeMillis();
        return row;
    }
}
