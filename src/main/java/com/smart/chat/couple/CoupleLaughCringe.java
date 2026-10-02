package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * F393 尴尬回收站：社死时刻提交，对方盖「抱抱你」章，365 天后读时转成好笑的事。
 * 判定口径（V48 头注）：一天一人一格，盖章的人只可能是对方，所以只记 healed_by 用户名，不拆 A/B 双列。
 * 「什么时候不好笑了」不落库、不写标记列——turnedFunny(today) 在读取时按 day 距今的天数现算，
 * 满 HEAL_AFTER_DAYS 天才换一句话术讲，所以本表没有任何状态需要结算任务去翻。
 */
@Data
@TableName("couple_laugh_cringe")
public class CoupleLaughCringe {

    /** 社死现场字数上限（列 varchar(400) 已按 4 倍宽度放宽） */
    public static final int CONTENT_MAX = 100;
    /** 社死日距今满 365 天，读时才转成「好笑的事」 */
    public static final int HEAL_AFTER_DAYS = 365;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 社死日 yyyy-MM-dd */
    private String day;
    /** 经历社死的人（区分大小写） */
    private String fromUser;
    /** 社死现场 */
    private String content;
    /** 盖「抱抱你」章的人=对方（null=还没盖） */
    private String healedBy;
    /** 盖章时间毫秒；null=还没盖 */
    private Long healedAt;
    private Long created;
    private Long updatedAt;

    public static CoupleLaughCringe of(String spaceId, String day, String fromUser, String content) {
        CoupleLaughCringe row = new CoupleLaughCringe();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.content = content;
        row.healedBy = null;
        row.healedAt = null;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 章盖了没有。 */
    public boolean healed() {
        return healedBy != null && !healedBy.isBlank();
    }

    /** 对方盖「抱抱你」章：一条只盖一次，已盖过返回 false 不改写（服务层不重推）。 */
    public boolean heal(String by) {
        if (healed()) {
            return false;
        }
        healedBy = by;
        healedAt = System.currentTimeMillis();
        updatedAt = healedAt;
        return true;
    }

    /**
     * 365 天后读时转成好笑的事：today 与社死日相差满 HEAL_AFTER_DAYS 天才成立。
     * 只在展示时判定，不改任何列；日期解析不了（脏数据/空串）一律当「还没到」返回 false。
     */
    public boolean turnedFunny(String today) {
        if (day == null || day.isBlank() || today == null || today.isBlank()) {
            return false;
        }
        try {
            return ChronoUnit.DAYS.between(LocalDate.parse(day), LocalDate.parse(today)) >= HEAL_AFTER_DAYS;
        } catch (Exception e) {
            return false;
        }
    }
}
