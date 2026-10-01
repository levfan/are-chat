package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F298 周年愿望台账：一年一愿，之后盖「圆上了/鸽了」章。 */
@Data
@TableName("couple_anniv_wish")
public class CoupleAnnivWish {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String year;
    private String fromUser;
    private String wish;
    private String verdict;
    private Long verdictAt;
    private Long created;

    public static CoupleAnnivWish of(String spaceId, String year, String fromUser, String wish) {
        CoupleAnnivWish row = new CoupleAnnivWish();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.year = year;
        row.fromUser = fromUser;
        row.wish = wish;
        row.verdict = "";
        row.created = System.currentTimeMillis();
        return row;
    }
}
