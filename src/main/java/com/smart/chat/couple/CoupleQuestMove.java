package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F375 搬家互助：一个空间固定 8 个打包区块（uk(space_id,slot) 一格一行），
 * 各自认领分工并累计纸箱数，格子自己标打包完。
 */
@Data
@TableName("couple_quest_move")
public class CoupleQuestMove {

    public static final int SLOT_MIN = 1;
    public static final int SLOT_MAX = 8;
    /** 区块名字数上限（列 varchar(60) 已按 3 倍宽度放宽） */
    public static final int NAME_MAX = 20;
    /** 纸箱数上限（服务层钳制） */
    public static final int BOX_MAX = 99;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 区块位 1-8（固定八格，服务层校验） */
    private Integer slot;
    /** 区块名（厨房/书房…）；空串=还没起名 */
    private String name;
    /** 认领的人；null=没人认领 */
    private String owner;
    /** 打包纸箱数 0-99（服务层钳制） */
    private Integer boxes;
    /** 该区块是否打包完（1=完） */
    private Integer done;
    private Long created;
    private Long updatedAt;

    public static CoupleQuestMove of(String spaceId, int slot) {
        CoupleQuestMove row = new CoupleQuestMove();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.slot = slot;
        row.name = "";
        row.owner = null;
        row.boxes = 0;
        row.done = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 该区块是否已被认领（owner 非空非空白）。 */
    public boolean claimed() {
        return owner != null && !owner.isBlank();
    }

    /** 该区块是否打包完（done=1）。 */
    public boolean finished() {
        return done != null && done == 1;
    }
}
