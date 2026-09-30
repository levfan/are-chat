package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 追剧清单（F76）：遥控器是两个人的，进度也是。 */
@Data
@TableName("couple_watchlist")
public class CoupleWatchlist {

    public static final String STATUS_WATCHING = "WATCHING";
    public static final String STATUS_DONE = "DONE";
    public static final int TITLE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    private int currentUnit;
    private Integer totalUnit;
    private String updatedBy;
    private String status;
    private Long finishedAt;
    private Long created;

    public static CoupleWatchlist of(String spaceId, String title, Integer totalUnit) {
        CoupleWatchlist row = new CoupleWatchlist();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.title = title;
        row.totalUnit = totalUnit;
        row.status = STATUS_WATCHING;
        row.created = System.currentTimeMillis();
        return row;
    }
}
