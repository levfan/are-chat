package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 道歉券（F62）：把「对不起」做成一张可以递出去的券，对方收下即台阶已搭好。 */
@Data
@TableName("couple_sorry_ticket")
public class CoupleSorryTicket {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_USED = "USED";
    public static final int NOTE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String note;
    private String status;
    private String usedNote;
    private Long usedAt;
    private Long created;

    public static CoupleSorryTicket of(String spaceId, String fromUser, String note) {
        CoupleSorryTicket row = new CoupleSorryTicket();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.note = note;
        row.status = STATUS_ACTIVE;
        row.created = System.currentTimeMillis();
        return row;
    }
}
