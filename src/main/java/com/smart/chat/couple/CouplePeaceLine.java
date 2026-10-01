package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F329 和平纪念碑：每次大和好存一句「这段吵架最代表性的话」，周年回看。 */
@Data
@TableName("couple_peace_line")
public class CouplePeaceLine {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private String line;
    private String note;
    private Long created;

    public static CouplePeaceLine of(String spaceId, String day, String fromUser, String line, String note) {
        CouplePeaceLine row = new CouplePeaceLine();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.line = line;
        row.note = note == null ? "" : note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
