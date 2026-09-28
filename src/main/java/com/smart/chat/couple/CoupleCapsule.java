package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 时光胶囊：写给未来的 TA——封存 30~365 天，到点才能开启。
 * 区别于 7 天内的慢递悄悄话，胶囊是更长的约定。
 */
@Data
@TableName("couple_capsule")
public class CoupleCapsule {

    public static final String STATUS_SEALED = "SEALED";
    public static final String STATUS_OPENED = "OPENED";

    public static final int CONTENT_MAX = 500;
    /** 封存期限下限/上限（天） */
    public static final int OPEN_MIN_DAYS = 30;
    public static final int OPEN_MAX_DAYS = 365;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String sender;
    private String recipient;
    private String content;
    /** 可开启日期（yyyy-MM-dd） */
    private String openDay;
    private String status;
    private Long openedAt;
    private Long created;

    public static CoupleCapsule of(String spaceId, String sender, String recipient, String content, String openDay) {
        CoupleCapsule capsule = new CoupleCapsule();
        capsule.id = UUID.randomUUID().toString();
        capsule.spaceId = spaceId;
        capsule.sender = sender;
        capsule.recipient = recipient;
        capsule.content = content;
        capsule.openDay = openDay;
        capsule.status = STATUS_SEALED;
        capsule.created = System.currentTimeMillis();
        return capsule;
    }

    /** 现在是否可以开启（到达 openDay 当天 00:00 之后）。 */
    public boolean openable(LocalDate today) {
        return STATUS_SEALED.equals(status) && !today.isBefore(LocalDate.parse(openDay));
    }

    /** 是否处于「封存未到期」状态。 */
    public boolean locked(LocalDate today) {
        return STATUS_SEALED.equals(status) && !openable(today);
    }
}
