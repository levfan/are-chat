package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F285 去过的地方：足迹档案（地名同空间唯一）。 */
@Data
@TableName("couple_place")
public class CouplePlace {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String name;
    private String year;
    private String happened;
    private Integer rating;
    private String fromUser;
    private Long created;
    private Long updatedAt;

    public static CouplePlace of(String spaceId, String name, String year, String happened,
                                 int rating, String fromUser) {
        CouplePlace row = new CouplePlace();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.name = name;
        row.year = year == null ? "" : year;
        row.happened = happened == null ? "" : happened;
        row.rating = Math.max(1, Math.min(5, rating));
        row.fromUser = fromUser;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
