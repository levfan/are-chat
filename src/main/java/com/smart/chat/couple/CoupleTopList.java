package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F282 喜好 TOP10：各类目本人榜。 */
@Data
@TableName("couple_top_list")
public class CoupleTopList {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String category;
    private String ownerUser;
    private String items;
    private Long created;
    private Long updatedAt;

    public static CoupleTopList of(String spaceId, String category, String ownerUser, String items) {
        CoupleTopList row = new CoupleTopList();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.category = category;
        row.ownerUser = ownerUser;
        row.items = items;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
