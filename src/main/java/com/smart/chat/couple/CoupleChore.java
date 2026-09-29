package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 家务轮值：定义家务 + 轮值方式，完成打卡自动轮换值日生。 */
@Data
@TableName("couple_chore")
public class CoupleChore {

    public static final String ROTATE_SINGLE = "SINGLE";
    public static final String ROTATE_ALTERNATE = "ALTERNATE";

    public static final int TITLE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    /** 轮值方式：SINGLE 固定一人 / ALTERNATE 每次轮换 */
    private String rotate;
    /** 当前值日生用户名 */
    private String turn;
    private Integer doneCount;
    private String lastDoneDay;
    private String lastDoneBy;
    private Long created;
    private Long updatedAt;

    public static CoupleChore of(String spaceId, String title, String rotate, String turn) {
        CoupleChore row = new CoupleChore();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.title = title;
        row.rotate = rotate;
        row.turn = turn;
        row.doneCount = 0;
        row.created = System.currentTimeMillis();
        return row;
    }
}
