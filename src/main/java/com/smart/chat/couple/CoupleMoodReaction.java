package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 心情回应：对 TA 某天记录的心情贴一个回应（抱抱/亲亲/加油/摸摸头）。
 * 每人每天对对方只有一条回应，重复提交视为修改。
 */
@Data
@TableName("couple_mood_reaction")
public class CoupleMoodReaction {

    public static final String REACTION_HUG = "HUG";
    public static final String REACTION_KISS = "KISS";
    public static final String REACTION_CHEER = "CHEER";
    public static final String REACTION_PAT = "PAT";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 被回应的心情日期（yyyy-MM-dd） */
    private String moodDay;
    /** 回应人用户名 */
    private String fromUser;
    private String reaction;
    private Long created;
    private Long updatedAt;

    public static CoupleMoodReaction of(String spaceId, String moodDay, String fromUser, String reaction) {
        CoupleMoodReaction row = new CoupleMoodReaction();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.moodDay = moodDay;
        row.fromUser = fromUser;
        row.reaction = reaction;
        row.created = System.currentTimeMillis();
        return row;
    }

    public static String emojiOf(String reaction) {
        return switch (reaction == null ? "" : reaction) {
            case REACTION_HUG -> "🤗";
            case REACTION_KISS -> "💋";
            case REACTION_CHEER -> "💪";
            case REACTION_PAT -> "🫶";
            default -> "💕";
        };
    }

    public static String labelOf(String reaction) {
        return switch (reaction == null ? "" : reaction) {
            case REACTION_HUG -> "抱抱";
            case REACTION_KISS -> "亲亲";
            case REACTION_CHEER -> "加油";
            case REACTION_PAT -> "摸摸头";
            default -> "回应";
        };
    }

    public static boolean isValidReaction(String reaction) {
        return REACTION_HUG.equals(reaction) || REACTION_KISS.equals(reaction)
                || REACTION_CHEER.equals(reaction) || REACTION_PAT.equals(reaction);
    }
}
