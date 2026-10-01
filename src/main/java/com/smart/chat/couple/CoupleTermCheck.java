package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F250 节气跟风机：节气当日一键「跟上了」+晒一句话，双方互见。 */
@Data
@TableName("couple_term_check")
public class CoupleTermCheck {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String term;
    private String year;
    private String fromUser;
    private String note;
    private Long created;

    public static CoupleTermCheck of(String spaceId, String term, String year, String fromUser, String note) {
        CoupleTermCheck row = new CoupleTermCheck();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.term = term;
        row.year = year;
        row.fromUser = fromUser;
        row.note = note == null ? "" : note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
