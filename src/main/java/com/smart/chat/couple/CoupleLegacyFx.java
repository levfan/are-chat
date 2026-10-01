package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F344 恋爱汇率：三种心意的兑换比，年末趣味结算。 */
@Data
@TableName("couple_legacy_fx")
public class CoupleLegacyFx {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private Integer kissToHug;
    private Integer hugToWord;
    private String settledYear;
    private Long created;
    private Long updatedAt;

    public static CoupleLegacyFx of(String spaceId, String fromUser, int kissToHug, int hugToWord) {
        CoupleLegacyFx row = new CoupleLegacyFx();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.kissToHug = kissToHug;
        row.hugToWord = hugToWord;
        row.settledYear = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
