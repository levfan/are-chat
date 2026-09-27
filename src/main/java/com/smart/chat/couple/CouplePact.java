package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 恋爱条约：双方共同签署的甜蜜公约（如「吵架不过夜」「每周一次约会日」）。
 * 一方提出（accepted_by 为空 = 待盖章），另一方盖章后生效；双方都能废除（删除）。
 */
@Data
@TableName("couple_pact")
public class CouplePact {

    public static final int CONTENT_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String content;
    private String proposedBy;
    /** 盖章人用户名（空 = 还没生效） */
    private String acceptedBy;
    private Long acceptedAt;
    private Long created;

    public static CouplePact of(String spaceId, String proposer, String content) {
        CouplePact pact = new CouplePact();
        pact.id = UUID.randomUUID().toString();
        pact.spaceId = spaceId;
        pact.content = content;
        pact.proposedBy = proposer;
        pact.created = System.currentTimeMillis();
        return pact;
    }

    /** 是否已生效（对方盖过章）。 */
    public boolean isAccepted() {
        return acceptedBy != null && !acceptedBy.isBlank();
    }
}
