package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F240 头衔任命：各报两个在家的职位，对方点「任命」生效。 */
@Data
@TableName("couple_board_role")
public class CoupleBoardRole {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String toUser;
    private String title;
    private Integer appointed;
    private Long created;
    private Long updatedAt;

    public static CoupleBoardRole of(String spaceId, String fromUser, String toUser, String title) {
        CoupleBoardRole row = new CoupleBoardRole();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.toUser = toUser;
        row.title = title;
        row.appointed = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean appointedFlag() {
        return appointed != null && appointed == 1;
    }
}
