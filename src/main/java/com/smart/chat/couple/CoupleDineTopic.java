package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F218 饭桌话题卡：吃完标记「聊过了」。 */
@Data
@TableName("couple_dine_topic")
public class CoupleDineTopic {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String markedBy;
    private Long created;

    public static CoupleDineTopic of(String spaceId, String day, String markedBy) {
        CoupleDineTopic row = new CoupleDineTopic();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.markedBy = markedBy;
        row.created = System.currentTimeMillis();

        return row;
    }
}
