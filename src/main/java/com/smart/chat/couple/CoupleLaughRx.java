package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * F396 大笑处方：对方低落时开一张处方，指定翻某条笑点/尴尬/突袭，对方回一句「已服用」。
 * 限流口径：每人每天一张（uk(space_id,from_user,day)）；target_id 必须是本空间的行，
 * 归属由服务层查目标表校验，本层只存 target_kind + target_id 两个字符串。
 * 回执判定只记 taken_by 用户名（V48 头注：收方只可能是对方，不拆 A/B 双列）。
 */
@Data
@TableName("couple_laugh_rx")
public class CoupleLaughRx {

    /** 处方指向笑点存档 */
    public static final String TARGET_MOMENT = "MOMENT";
    /** 处方指向尴尬回收站 */
    public static final String TARGET_CRINGE = "CRINGE";
    /** 处方指向快乐突袭 */
    public static final String TARGET_ATTACK = "ATTACK";
    /** 指向类型白名单（服务层校验，顺序即口径顺序） */
    public static final List<String> TARGET_KINDS = List.of(TARGET_MOMENT, TARGET_CRINGE, TARGET_ATTACK);
    /** 医嘱一句话字数上限（列 varchar(240) 已按 4 倍宽度放宽） */
    public static final int NOTE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 开处方日 yyyy-MM-dd（每人每天一张） */
    private String day;
    /** 开处方的人（区分大小写） */
    private String fromUser;
    /** MOMENT 笑点/CRINGE 尴尬/ATTACK 突袭（服务层白名单） */
    private String targetKind;
    /** 指向的条目 id（必须是本空间的行） */
    private String targetId;
    /** 医嘱一句话；空串=没写 */
    private String note;
    /** 已服用回执的人=收方（null=还没服） */
    private String takenBy;
    /** 服用时间毫秒；null=还没服 */
    private Long takenAt;
    private Long created;
    private Long updatedAt;

    public static CoupleLaughRx of(String spaceId, String day, String fromUser,
                                   String targetKind, String targetId, String note) {
        CoupleLaughRx row = new CoupleLaughRx();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.targetKind = targetKind;
        row.targetId = targetId;
        row.note = note == null ? "" : note;
        row.takenBy = null;
        row.takenAt = null;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 对方服了没有。 */
    public boolean taken() {
        return takenBy != null && !takenBy.isBlank();
    }

    /** 收方回「已服用」：一张只服一次，已回过返回 false 不改写（服务层不重推）。 */
    public boolean markTaken(String by) {
        if (taken()) {
            return false;
        }
        takenBy = by;
        takenAt = System.currentTimeMillis();
        updatedAt = takenAt;
        return true;
    }
}
