package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 悄悄话信箱：写给 TA 的小纸条（1-300 字），支持慢递——deliver_at 之前收件人不能拆，
 * 到点后收件人拆封，双方都可见内容；发件人在拆封前可以撤回。
 */
@Data
@TableName("couple_letter")
public class CoupleLetter {

    public static final String STATUS_SEALED = "SEALED";
    public static final String STATUS_OPENED = "OPENED";

    public static final int CONTENT_MAX = 300;

    /** 慢递最长期限：7 天（毫秒）。 */
    public static final long DELIVER_MAX_MILLIS = 7L * 24 * 60 * 60 * 1000;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String sender;
    private String recipient;
    private String content;
    /** 可拆封时间（毫秒时间戳，null = 立即可拆） */
    private Long deliverAt;
    private String status;
    private Long openedAt;
    private Long created;

    public static CoupleLetter of(String spaceId, String sender, String recipient, String content, Long deliverAt) {
        CoupleLetter letter = new CoupleLetter();
        letter.id = UUID.randomUUID().toString();
        letter.spaceId = spaceId;
        letter.sender = sender;
        letter.recipient = recipient;
        letter.content = content;
        letter.deliverAt = deliverAt;
        letter.status = STATUS_SEALED;
        letter.created = System.currentTimeMillis();
        return letter;
    }

    /** 收件人此刻是否可以拆封。 */
    public boolean deliverable(long now) {
        return deliverAt == null || deliverAt <= now;
    }

    /** 是否处于「未到期慢递」状态。 */
    public boolean locked(long now) {
        return STATUS_SEALED.equals(status) && !deliverable(now);
    }
}
