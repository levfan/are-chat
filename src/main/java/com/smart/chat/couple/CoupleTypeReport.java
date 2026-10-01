package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F289 人格双报：8 题四维速测，一年一报，双报出差异卡。 */
@Data
@TableName("couple_type_report")
public class CoupleTypeReport {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String year;
    private String fromUser;
    private String answers;
    private String typeKey;
    private Long created;
    private Long updatedAt;

    public static CoupleTypeReport of(String spaceId, String year, String fromUser,
                                      String answers, String typeKey) {
        CoupleTypeReport row = new CoupleTypeReport();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.year = year;
        row.fromUser = fromUser;
        row.answers = answers;
        row.typeKey = typeKey;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
