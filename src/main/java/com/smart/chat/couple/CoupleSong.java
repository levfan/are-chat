package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 我们的歌单（F89）：每首歌都藏着一段我们的故事。 */
@Data
@TableName("couple_song")
public class CoupleSong {

    public static final int TITLE_MAX = 100;
    public static final int ARTIST_MAX = 50;
    public static final int REASON_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String title;
    private String artist;
    private String reason;
    private Long created;

    public static CoupleSong of(String spaceId, String fromUser, String title, String artist, String reason) {
        CoupleSong row = new CoupleSong();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.title = title;
        row.artist = artist;
        row.reason = reason;
        row.created = System.currentTimeMillis();
        return row;
    }
}
