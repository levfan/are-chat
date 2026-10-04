package com.smart.chat.messaging.infrastructure.identity;

import com.smart.chat.identity.domain.AdminAlerter;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import org.springframework.stereotype.Component;

/** 管理员待办角标：转给 messaging 的推送门面（帧格式本来就归它定义）。 */
@Component
public class AdminAlerterAdapter implements AdminAlerter {

    private final ImPushService push;

    public AdminAlerterAdapter(ImPushService push) {
        this.push = push;
    }

    @Override
    public void publishPendingCount(Iterable<String> adminUsernames, long pendingCount) {
        push.pushAdminEvent(adminUsernames, pendingCount);
    }
}
