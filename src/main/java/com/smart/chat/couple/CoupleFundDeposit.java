package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 心愿基金存入记录：谁在什么时候往哪个目标存了多少（流水双方可见）。 */
@Data
@TableName("couple_fund_deposit")
public class CoupleFundDeposit {

    public static final int NOTE_MAX = 100;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    private String fundId;
    private String username;
    /** 存入金额（分） */
    private Long amount;
    private String note;
    private Long created;

    public static CoupleFundDeposit of(String spaceId, String fundId, String username, long amount, String note) {
        CoupleFundDeposit row = new CoupleFundDeposit();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.fundId = fundId;
        row.username = username;
        row.amount = amount;
        row.note = note;
        row.created = System.currentTimeMillis();
        return row;
    }
}
