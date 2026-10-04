package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 贴贴动作流水：一键发送给 TA 的亲密小动作（戳一戳/抱抱/亲亲/捏捏脸/蹭蹭/挠痒痒/在想你）。
 * 每次发送都记一条流水，用于今日动作流与贴贴里程碑统计。
 */
@Data
@TableName("couple_action")
public class CoupleAction {

    public static final String KIND_POKE = "POKE";
    public static final String KIND_HUG = "HUG";
    public static final String KIND_KISS = "KISS";
    public static final String KIND_PAT = "PAT";
    public static final String KIND_NUZZLE = "NUZZLE";
    public static final String KIND_TICKLE = "TICKLE";
    public static final String KIND_MISS = "MISS";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 发送方用户名 */
    private String username;
    private String kind;
    private Long created;

    public static CoupleAction of(String spaceId, String username, String kind) {
        CoupleAction action = new CoupleAction();
        action.id = UUID.randomUUID().toString();
        action.spaceId = spaceId;
        action.username = username;
        action.kind = kind;
        action.created = System.currentTimeMillis();
        return action;
    }

    /** 动作的 emoji 图标。 */
    public static String emojiOf(String kind) {
        return switch (kind == null ? "" : kind) {
            case KIND_POKE -> "👉";
            case KIND_HUG -> "🤗";
            case KIND_KISS -> "💋";
            case KIND_PAT -> "🫳";
            case KIND_NUZZLE -> "😚";
            case KIND_TICKLE -> "🤭";
            case KIND_MISS -> "💌";
            default -> "💕";
        };
    }

    /** 动作的中文名。 */
    public static String labelOf(String kind) {
        return switch (kind == null ? "" : kind) {
            case KIND_POKE -> "戳一戳";
            case KIND_HUG -> "抱抱";
            case KIND_KISS -> "亲亲";
            case KIND_PAT -> "捏捏脸";
            case KIND_NUZZLE -> "蹭蹭";
            case KIND_TICKLE -> "挠痒痒";
            case KIND_MISS -> "在想你";
            default -> "贴贴";
        };
    }

    public static boolean isValidKind(String kind) {
        return KIND_POKE.equals(kind) || KIND_HUG.equals(kind) || KIND_KISS.equals(kind)
                || KIND_PAT.equals(kind) || KIND_NUZZLE.equals(kind) || KIND_TICKLE.equals(kind)
                || KIND_MISS.equals(kind);
    }
}
