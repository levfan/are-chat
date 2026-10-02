package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F245 金点子箱：一句话经营提案，被采纳即转董事会决议。 */
@Data
@TableName("couple_board_idea")
public class CoupleBoardIdea {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String content;
    private Integer adopted;
    private String voteId;
    private Long created;

    public static CoupleBoardIdea of(String spaceId, String fromUser, String content) {
        CoupleBoardIdea row = new CoupleBoardIdea();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.adopted = 0;
        row.voteId = "";
        row.created = System.currentTimeMillis();
        return row;
    }

    public boolean adoptedFlag() {
        return adopted != null && adopted == 1;
    }
}
