package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F247 会议签到：10s 窗口内双方各按一次才算开了会（复用数羊窗口模式）。 */
@Data
@TableName("couple_board_attend")
public class CoupleBoardAttend {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String day;
    private String fromUser;
    private Integer attended;
    private Integer convened;
    private Long created;
    private Long updatedAt;

    public static CoupleBoardAttend of(String spaceId, String day, String fromUser) {
        CoupleBoardAttend row = new CoupleBoardAttend();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.attended = 1;
        row.convened = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean convenedFlag() {
        return convened != null && convened == 1;
    }
}
