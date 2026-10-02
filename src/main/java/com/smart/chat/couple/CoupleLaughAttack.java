package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * F394 快乐突袭：突发一串夸奖/一个梗/一段回忆杀，对方「中弹」盖章。
 * 限流口径：每人每天一次（uk(space_id,from_user,day)），所以一天一人只有一行，
 * 中弹判定只记 hit_by 用户名（V48 头注：判定人只可能是对方，不拆 A/B 双列）。
 * kind 白名单在服务层校验（PRAISE/MEME/MEMORY），本类只存常量与顺序。
 */
@Data
@TableName("couple_laugh_attack")
public class CoupleLaughAttack {

    /** 一串夸奖 */
    public static final String KIND_PRAISE = "PRAISE";
    /** 一个梗 */
    public static final String KIND_MEME = "MEME";
    /** 一段回忆杀 */
    public static final String KIND_MEMORY = "MEMORY";
    /** 突袭类型白名单（服务层校验，顺序即口径顺序） */
    public static final List<String> KINDS = List.of(KIND_PRAISE, KIND_MEME, KIND_MEMORY);
    /** 突袭内容字数上限（列 varchar(400) 已按 4 倍宽度放宽） */
    public static final int CONTENT_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 突袭日 yyyy-MM-dd（每人每天一次） */
    private String day;
    /** 发动突袭的人（区分大小写） */
    private String fromUser;
    /** PRAISE 一串夸奖/MEME 一个梗/MEMORY 一段回忆杀 */
    private String kind;
    /** 突袭内容；空串=没写 */
    private String content;
    /** 中弹盖章的人=对方（null=还没中弹） */
    private String hitBy;
    /** 中弹时间毫秒；null=还没中弹 */
    private Long hitAt;
    private Long created;
    private Long updatedAt;

    public static CoupleLaughAttack of(String spaceId, String day, String fromUser, String kind, String content) {
        CoupleLaughAttack row = new CoupleLaughAttack();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.kind = kind;
        row.content = content == null ? "" : content;
        row.hitBy = null;
        row.hitAt = null;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 对方中弹了没有。 */
    public boolean hit() {
        return hitBy != null && !hitBy.isBlank();
    }

    /** 对方盖「中弹」章：一发只盖一次，已盖过返回 false 不改写（服务层不重推）。 */
    public boolean markHit(String by) {
        if (hit()) {
            return false;
        }
        hitBy = by;
        hitAt = System.currentTimeMillis();
        updatedAt = hitAt;
        return true;
    }
}
