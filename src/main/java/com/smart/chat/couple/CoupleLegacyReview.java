package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F346 我们的一年：一键年度盘点，写的是真数字不是套话。 */
@Data
@TableName("couple_legacy_review")
public class CoupleLegacyReview {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String year;
    private String content;
    private String fromUser;
    private Long created;
    private Long updatedAt;

    public static CoupleLegacyReview of(String spaceId, String year, String content, String fromUser) {
        CoupleLegacyReview row = new CoupleLegacyReview();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.year = year;
        row.content = content;
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
