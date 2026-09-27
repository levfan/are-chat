package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 心愿基金：共同的存钱目标（如「一起去北海道旅行」）。双方都能往里存钱（couple_fund_deposit），
 * saved_amount 攒到 target_amount 自动标记达成并推送庆祝。
 */
@Data
@TableName("couple_fund")
public class CoupleFund {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REACHED = "REACHED";

    public static final int TITLE_MAX = 60;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String title;
    /** 目标金额（分） */
    private Long targetAmount;
    /** 已存金额（分） */
    private Long savedAmount;
    private String status;
    private Long doneAt;
    private String createdBy;
    private Long created;

    public static CoupleFund of(String spaceId, String creator, String title, long targetAmount) {
        CoupleFund fund = new CoupleFund();
        fund.id = UUID.randomUUID().toString();
        fund.spaceId = spaceId;
        fund.title = title;
        fund.targetAmount = targetAmount;
        fund.savedAmount = 0L;
        fund.status = STATUS_ACTIVE;
        fund.createdBy = creator;
        fund.created = System.currentTimeMillis();
        return fund;
    }

    public boolean isReached() {
        return CoupleFund.STATUS_REACHED.equals(status);
    }
}
