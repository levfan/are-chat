package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F336 亲戚称呼册：称谓关系测验，错题进考前强化。 */
@Data
@TableName("couple_world_relatives_q")
public class CoupleWorldRelativesQ {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String term;
    private String question;
    private String answer;
    private Integer wrongCount;
    private String lastWrongDay;
    private String fromUser;
    private Long created;
    private Long updatedAt;

    public static CoupleWorldRelativesQ of(String spaceId, String term, String question, String answer, String fromUser) {
        CoupleWorldRelativesQ row = new CoupleWorldRelativesQ();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.term = term;
        row.question = question == null ? "" : question;
        row.answer = answer == null ? "" : answer;
        row.wrongCount = 0;
        row.lastWrongDay = "";
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
