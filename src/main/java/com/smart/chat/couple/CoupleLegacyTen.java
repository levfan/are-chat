package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F340 年度十问：每年固定十问双答，跨年看变化。 */
@Data
@TableName("couple_legacy_ten")
public class CoupleLegacyTen {

    public static final int QUESTION_COUNT = 10;
    public static final int ANSWER_MAX = 140;
    public static final String SEP = "\n";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String year;
    private String fromUser;
    private String answers;
    private Long created;
    private Long updatedAt;

    public static CoupleLegacyTen of(String spaceId, String year, String fromUser) {
        CoupleLegacyTen row = new CoupleLegacyTen();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.year = year;
        row.fromUser = fromUser;
        row.answers = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public int answeredCount() {
        if (answers == null || answers.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String line : answers.split(SEP)) {
            if (!line.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
