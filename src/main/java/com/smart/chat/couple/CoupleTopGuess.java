package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F282 喜好互猜：猜对方榜单，双榜齐后差异进「重新认识清单」。 */
@Data
@TableName("couple_top_guess")
public class CoupleTopGuess {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String category;
    private String ownerUser;
    private String guesserUser;
    private String items;
    private Long created;
    private Long updatedAt;

    public static CoupleTopGuess of(String spaceId, String category, String ownerUser,
                                    String guesserUser, String items) {
        CoupleTopGuess row = new CoupleTopGuess();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.category = category;
        row.ownerUser = ownerUser;
        row.guesserUser = guesserUser;
        row.items = items;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }
}
