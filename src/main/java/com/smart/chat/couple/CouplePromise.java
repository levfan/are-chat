package com.smart.chat.couple;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.UUID;

/**
 * 约定/承诺卡：promiser 答应 creditor 做某件事。
 * 如 promiser 说「明天给你带奶茶」→ creditor 的「TA 答应我的事」出现一张承诺卡，
 * promiser 完成打卡后 creditor 收到推送；逾期未兑现给 creditor 可爱提醒。
 */
@Data
@TableName("couple_promise")
public class CouplePromise {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_DONE = "DONE";
    /** 承诺内容最长长度 */
    public static final int CONTENT_MAX = 200;

    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String spaceId;
    /** 承诺人（答应做事的一方） */
    private String promiser;
    /** 受益人（被承诺的一方） */
    private String creditor;
    private String content;
    /** 截止时间（毫秒时间戳，空表示不设期限） */
    private Long dueAt;
    private String status;
    private Long doneAt;
    /** 最近一次逾期提醒日期（yyyy-MM-dd） */
    private String lastRemindDay;
    private Long created;

    public static CouplePromise of(String spaceId, String promiser, String creditor, String content, Long dueAt) {
        CouplePromise promise = new CouplePromise();
        promise.id = UUID.randomUUID().toString();
        promise.spaceId = spaceId;
        promise.promiser = promiser;
        promise.creditor = creditor;
        promise.content = content;
        promise.dueAt = dueAt;
        promise.status = STATUS_PENDING;
        promise.created = System.currentTimeMillis();
        return promise;
    }

    /** 是否已逾期：待兑现且过了截止时间。 */
    public boolean isOverdue(long now) {
        return STATUS_PENDING.equals(status) && dueAt != null && dueAt < now;
    }
}
