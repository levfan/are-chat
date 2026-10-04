package com.smart.chat.identity.domain;

/**
 * 管理员外呼通知渠道（审批相关的站外提醒）。
 * 渠道本身（WxPusher / Server酱 / 企微 webhook）归 platform，identity 只交代「提醒谁、说什么」。
 */
public interface AdminNotifyChannel {

    /** 通用文本推送（异步尽力而为，失败不影响主流程） */
    void pushTextAsync(String title, String body);

    /** 注册申请审批结果通知 */
    void applicationReviewed(String username, boolean approved, String reviewer);
}
