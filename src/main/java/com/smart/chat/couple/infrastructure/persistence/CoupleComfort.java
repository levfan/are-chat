package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/** 求抱抱（F60）：一键告诉 TA「我需要安慰」，对方送出抱抱和一句话回应。 */
@Data
@TableName("couple_comfort")
public class CoupleComfort {

    public static final String FEELING_SAD = "SAD";
    public static final String FEELING_WRONGED = "WRONGED";
    public static final String FEELING_TIRED = "TIRED";
    public static final String FEELING_ANXIOUS = "ANXIOUS";
    public static final String FEELING_EMO = "EMO";

    public static final Set<String> FEELINGS = Set.of(FEELING_SAD, FEELING_WRONGED, FEELING_TIRED, FEELING_ANXIOUS, FEELING_EMO);

    public static final int NOTE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private String day;
    private String feeling;
    private boolean handled;
    private String handledNote;
    private Long handledAt;
    private Long created;

    public static CoupleComfort of(String spaceId, String fromUser, String feeling) {
        CoupleComfort row = new CoupleComfort();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.day = LocalDate.now().toString();
        row.feeling = feeling;
        row.handled = false;
        row.created = System.currentTimeMillis();
        return row;
    }

    public static String feelingLabel(String feeling) {
        return switch (feeling) {
            case FEELING_SAD -> "难过";
            case FEELING_WRONGED -> "委屈";
            case FEELING_TIRED -> "好累";
            case FEELING_ANXIOUS -> "焦虑";
            case FEELING_EMO -> "emo";
            default -> "不太好";
        };
    }

    public static String feelingEmoji(String feeling) {
        return switch (feeling) {
            case FEELING_SAD -> "😢";
            case FEELING_WRONGED -> "🥺";
            case FEELING_TIRED -> "😮‍💨";
            case FEELING_ANXIOUS -> "😖";
            case FEELING_EMO -> "🌧️";
            default -> "🫂";
        };
    }
}
