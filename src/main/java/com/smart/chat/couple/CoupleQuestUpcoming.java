package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F379 下次关卡预约：把未来 60 天已知的关口挂上双人时间轴，对方点「我会到场」。
 * 到场口径：attend_by 非空即已应援（空=还没应援），只能由对方应援自己应的不算。
 */
@Data
@TableName("couple_quest_upcoming")
public class CoupleQuestUpcoming {

    /** 关口名字数上限（列 varchar(90) 已按 3 倍宽度放宽） */
    public static final int TITLE_MAX = 30;
    /** 可预约的未来窗口天数（服务层校验） */
    public static final int WINDOW_DAYS = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 关口日子 yyyy-MM-dd（今天起 60 天内） */
    private String day;
    /** 挂上来的人（区分大小写） */
    private String fromUser;
    /** 关口名 */
    private String title;
    /** 说「我会到场」的人=对方；空串=还没应援 */
    private String attendBy;
    private Long created;
    private Long updatedAt;

    public static CoupleQuestUpcoming of(String spaceId, String day, String fromUser, String title) {
        CoupleQuestUpcoming row = new CoupleQuestUpcoming();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.day = day;
        row.fromUser = fromUser;
        row.title = title;
        row.attendBy = "";
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    /** 对方是否已说「我会到场」（attend_by 非空非空白）。 */
    public boolean attended() {
        return attendBy != null && !attendBy.isBlank();
    }

    /** 应援到场：写到场的人（同时刷新更新时间）。 */
    public void attend(String by) {
        attendBy = by;
        updatedAt = System.currentTimeMillis();
    }
}
