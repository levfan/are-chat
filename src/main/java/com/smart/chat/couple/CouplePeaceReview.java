package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/** 矛盾复盘（F61）：和好之后各写一份复盘，双方都写完自动合成「和好锦囊」。 */
@Data
@TableName("couple_peace_review")
public class CouplePeaceReview {

    public static final int PART_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String byUser;
    /** 我当时为什么在意 / 我的那部分 */
    private String myPart;
    /** 下次我们可以怎么做 */
    private String nextTime;
    private Long created;

    public static CouplePeaceReview of(String spaceId, String byUser, String myPart, String nextTime) {
        CouplePeaceReview row = new CouplePeaceReview();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = LocalDate.now().toString();
        row.byUser = byUser;
        row.myPart = myPart;
        row.nextTime = nextTime;
        row.created = System.currentTimeMillis();
        return row;
    }
}
