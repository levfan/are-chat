package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F251 节气过法：给任一节气定一条固定过法（每节气≤2），当年打卡推 TA。 */
@Data
@TableName("couple_term_ritual")
public class CoupleTermRitual {

    public static final int RITUAL_MAX = 2;
    public static final int CONTENT_MAX = 80;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String term;
    private String content;
    private String fromUser;
    private String lastDoneYear;
    private Long created;

    public static CoupleTermRitual of(String spaceId, String term, String content, String fromUser) {
        CoupleTermRitual row = new CoupleTermRitual();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.term = term;
        row.content = content;
        row.fromUser = fromUser;
        row.lastDoneYear = "";
        row.created = System.currentTimeMillis();
        return row;
    }

    /** 今年是否已打过卡。 */
    public boolean doneThisYear(String year) {
        return year != null && year.equals(lastDoneYear);
    }
}
