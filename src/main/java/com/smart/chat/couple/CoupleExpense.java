package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/** 甜蜜记账本：日常开销谁付的记一笔，月度汇总 + AA 差额提示。 */
@Data
@TableName("couple_expense")
public class CoupleExpense {

    public static final String CATEGORY_FOOD = "FOOD";
    public static final String CATEGORY_TRANSPORT = "TRANSPORT";
    public static final String CATEGORY_FUN = "FUN";
    public static final String CATEGORY_HOME = "HOME";
    public static final String CATEGORY_GIFT = "GIFT";
    public static final String CATEGORY_OTHER = "OTHER";

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 付款人用户名 */
    private String username;
    /** 金额（分） */
    private Long amount;
    private String category;
    private String note;
    /** 花销日期（yyyy-MM-dd） */
    private String spentDay;
    private Long created;

    public static CoupleExpense of(String spaceId, String username, long amount, String category,
                                   String note, String spentDay) {
        CoupleExpense row = new CoupleExpense();
        row.id = UUID.randomUUID().toString();
        row.spaceId = spaceId;
        row.username = username;
        row.amount = amount;
        row.category = category;
        row.note = note;
        row.spentDay = spentDay;
        row.created = System.currentTimeMillis();
        return row;
    }

    public static boolean isValidCategory(String category) {
        return CATEGORY_FOOD.equals(category) || CATEGORY_TRANSPORT.equals(category)
                || CATEGORY_FUN.equals(category) || CATEGORY_HOME.equals(category)
                || CATEGORY_GIFT.equals(category) || CATEGORY_OTHER.equals(category);
    }
}
