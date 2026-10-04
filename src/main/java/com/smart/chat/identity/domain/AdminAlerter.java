package com.smart.chat.identity.domain;

/**
 * 管理员待办角标：有新的注册申请时，在线管理员要立刻看到。
 * 推送通道归 messaging，identity 只声明这个需要。
 */
public interface AdminAlerter {

    void publishPendingCount(Iterable<String> adminUsernames, long pendingCount);
}
