package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

/**
 * 情侣心情日记：每天每人一条心情（LOVE/HAPPY/CALM/BUSY/TIRED/SICK/SAD/ANGRY）+ 一句话，
 * 按（空间, 用户, 自然日）唯一，当天重复提交视为修改。双方互相可见，用于绘制双人心情曲线。
 */
@Data
@TableName("couple_mood")
public class CoupleMood {

    /** 心情键 → 展示文案/表情由前端维护，后端只做白名单校验与曲线分值计算。 */
    public static final String MOOD_LOVE = "LOVE";
    public static final String MOOD_HAPPY = "HAPPY";
    public static final String MOOD_CALM = "CALM";
    public static final String MOOD_BUSY = "BUSY";
    public static final String MOOD_TIRED = "TIRED";
    public static final String MOOD_SICK = "SICK";
    public static final String MOOD_SAD = "SAD";
    public static final String MOOD_ANGRY = "ANGRY";

    public static final int NOTE_MAX = 200;

    public static final Set<String> MOOD_KEYS = Set.of(
            MOOD_LOVE, MOOD_HAPPY, MOOD_CALM, MOOD_BUSY, MOOD_TIRED, MOOD_SICK, MOOD_SAD, MOOD_ANGRY);

    /** 心情表情（推送文案用，前端同款映射负责展示）。 */
    public static String emojiOf(String mood) {
        return switch (mood) {
            case MOOD_LOVE -> "😍";
            case MOOD_HAPPY -> "😄";
            case MOOD_CALM -> "😌";
            case MOOD_BUSY -> "🤯";
            case MOOD_TIRED -> "😴";
            case MOOD_SICK -> "🤒";
            case MOOD_SAD -> "😢";
            case MOOD_ANGRY -> "😠";
            default -> "💗";
        };
    }

    /** 心情分值（1-5，供双人心情曲线/心动值参考：越好越高）。 */
    public static int scoreOf(String mood) {
        return switch (mood) {
            case MOOD_LOVE -> 5;
            case MOOD_HAPPY -> 4;
            case MOOD_CALM -> 3;
            case MOOD_BUSY, MOOD_TIRED -> 2;
            case MOOD_SICK, MOOD_SAD -> 1;
            case MOOD_ANGRY -> 0;
            default -> 0;
        };
    }

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String username;
    /** 心情日期 yyyy-MM-dd */
    private String moodDay;
    private String mood;
    private String note;
    private Long created;
    private Long updatedAt;

    public static CoupleMood of(String spaceId, String username, String day, String mood, String note) {
        CoupleMood row = new CoupleMood();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.username = username;
        row.moodDay = day;
        row.mood = mood;
        row.note = note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
