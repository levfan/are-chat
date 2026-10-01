package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 爱语测评（F170）：五爱语 12 题静态卷，重测覆盖。 */
@Data
@TableName("couple_love_lang")
public class CoupleLoveLang {

    public static final String LANG_WORDS = "WORDS";
    public static final String LANG_TIME = "TIME";
    public static final String LANG_GIFTS = "GIFTS";
    public static final String LANG_SERVICE = "SERVICE";
    public static final String LANG_TOUCH = "TOUCH";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    private Integer wordsScore;
    private Integer timeScore;
    private Integer giftsScore;
    private Integer serviceScore;
    private Integer touchScore;
    private String primaryLang;
    private Long created;
    private Long updatedAt;

    public static CoupleLoveLang of(String spaceId, String fromUser, String primaryLang) {
        long now = System.currentTimeMillis();
        CoupleLoveLang row = new CoupleLoveLang();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.wordsScore = 0;
        row.timeScore = 0;
        row.giftsScore = 0;
        row.serviceScore = 0;
        row.touchScore = 0;
        row.primaryLang = primaryLang;
        row.created = now;
        row.updatedAt = now;
        return row;
    }

    /** 按爱语取分数。 */
    public int scoreOf(String lang) {
        return switch (lang) {
            case LANG_WORDS -> wordsScore;
            case LANG_TIME -> timeScore;
            case LANG_GIFTS -> giftsScore;
            case LANG_SERVICE -> serviceScore;
            default -> touchScore;
        };
    }

    /** 按爱语写入分数。 */
    public void addScore(String lang, int delta) {
        switch (lang) {
            case LANG_WORDS -> wordsScore += delta;
            case LANG_TIME -> timeScore += delta;
            case LANG_GIFTS -> giftsScore += delta;
            case LANG_SERVICE -> serviceScore += delta;
            default -> touchScore += delta;
        }
    }
}
