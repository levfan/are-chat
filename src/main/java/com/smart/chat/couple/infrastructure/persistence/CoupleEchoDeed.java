package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F350 好事簿：记录「TA 为我做的事」——TA 爱我的证据，由记录人收藏。
 * 单记录人模型：只有 from_user=记录人，双方记录对称可见（看 TA 的记录 =「我为 TA 做过的事」清单）。
 */
@Data
@TableName("couple_echo_deed")
public class CoupleEchoDeed {

    public static final int CONTENT_MAX = 80;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 记录人：写下 TA 为自己做的一件事 */
    private String fromUser;
    private String content;
    /** 发生日 yyyy-MM-dd */
    private String day;
    /** 1=记录人点过「这条救过我」 */
    private Integer starred;
    private Long created;
    private Long updatedAt;

    public static CoupleEchoDeed of(String spaceId, String fromUser, String content, String day) {
        CoupleEchoDeed row = new CoupleEchoDeed();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.content = content;
        row.day = day;
        row.starred = 0;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean starredFlag() {
        return starred != null && starred == 1;
    }
}
