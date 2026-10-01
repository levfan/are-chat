package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 双人契约（F128）：一起养成的契约（如「每天说晚安」），双方各自计数。 */
@Data
@TableName("couple_self_contract")
public class CoupleSelfContract {

    public static final int TITLE_MAX = 50;
    public static final int CONTENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    private String content;
    private Integer countA;
    private Integer countB;
    private Long created;

    public static CoupleSelfContract of(String spaceId, String title, String content) {
        CoupleSelfContract row = new CoupleSelfContract();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.title = title;
        row.content = content;
        row.countA = 0;
        row.countB = 0;
        row.created = System.currentTimeMillis();
        return row;
    }
}
