package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** F322 重来卡：每季一张，重放那段对话并记满意度。 */
@Data
@TableName("couple_repair_redo")
public class CoupleRepairRedo {

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String quarter;
    private String scene;
    private String fromUser;
    private Integer used;
    private String replayNote;
    private Integer satisfaction;
    private String ratedBy;
    private Long created;
    private Long updatedAt;

    public static CoupleRepairRedo of(String spaceId, String quarter, String scene, String fromUser) {
        CoupleRepairRedo row = new CoupleRepairRedo();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.quarter = quarter;
        row.scene = scene == null ? "" : scene;
        row.fromUser = fromUser;
        row.used = 0;
        row.replayNote = "";
        row.ratedBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean usedFlag() {
        return used != null && used == 1;
    }
}
