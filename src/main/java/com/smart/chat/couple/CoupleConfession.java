package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 告白重现（F57）：把当年的告白词存下来，每年的今天由小助手重播一遍。 */
@Data
@TableName("couple_confession")
public class CoupleConfession {

    public static final int CONTENT_MAX = 500;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String content;
    /** 告白发生的日期 yyyy-MM-dd（每年这天重现） */
    private String confessDay;
    private String createdBy;
    /** 已重现过的年份（逗号分隔，防重复推送） */
    private String replayYears;
    private Long created;

    public static CoupleConfession of(String spaceId, String content, String confessDay, String createdBy) {
        CoupleConfession row = new CoupleConfession();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.content = content;
        row.confessDay = confessDay;
        row.createdBy = createdBy;
        row.created = System.currentTimeMillis();
        return row;
    }

    /** 某年是否已经重播过。 */
    public boolean replayed(int year) {
        return replayYears != null && (replayYears.equals(String.valueOf(year))
                || replayYears.contains("," + year) || replayYears.contains(year + ","));
    }

    /** 标记某年已重播。 */
    public void markReplayed(int year) {
        this.replayYears = this.replayYears == null || this.replayYears.isBlank()
                ? String.valueOf(year)
                : this.replayYears + "," + year;
    }
}
