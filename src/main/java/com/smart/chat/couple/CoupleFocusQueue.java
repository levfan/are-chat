package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * F362 攒一句话：对方专注/勿扰时先把话攒进队列（在途每人 ≤5），
 * TA 结束后一键收全部，读时批量置 read_at。
 */
@Data
@TableName("couple_focus_queue")
public class CoupleFocusQueue {

    public static final int CONTENT_MAX = 80;
    /** 每人在途上限 5 条。 */
    public static final int IN_FLIGHT_MAX = 5;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fromUser;
    /** 收留言的人=对方 */
    private String toUser;
    private String content;
    /** 已读时间毫秒；null=还在排队 */
    private Long readAt;
    private Long created;
    private Long updatedAt;

    public static CoupleFocusQueue of(String spaceId, String fromUser, String toUser, String content) {
        CoupleFocusQueue row = new CoupleFocusQueue();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fromUser = fromUser;
        row.toUser = toUser;
        row.content = content;
        row.created = System.currentTimeMillis();
        row.updatedAt = row.created;
        return row;
    }

    public boolean read() {
        return readAt != null;
    }

    /** 置已读（幂等：已读的不再改写时间）。 */
    public void markRead() {
        if (readAt == null) {
            readAt = System.currentTimeMillis();
            updatedAt = readAt;
        }
    }
}
