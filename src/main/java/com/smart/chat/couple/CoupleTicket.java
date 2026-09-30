package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 恋爱电影票根（F88）：散场不散，票根为证。 */
@Data
@TableName("couple_ticket")
public class CoupleTicket {

    public static final int TITLE_MAX = 100;
    public static final int COMMENT_MAX = 200;
    public static final int RATING_MAX = 5;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String title;
    private String watchDay;
    private int rating;
    private String comment;
    private Long created;

    public static CoupleTicket of(String spaceId, String fromUser, String title, String watchDay, int rating, String comment) {
        CoupleTicket row = new CoupleTicket();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.title = title;
        row.watchDay = watchDay;
        row.rating = rating;
        row.comment = comment;
        row.created = System.currentTimeMillis();
        return row;
    }
}
