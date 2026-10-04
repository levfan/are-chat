package com.smart.chat.platform.infrastructure.identity;

import com.smart.chat.identity.domain.AdminNotifyChannel;
import com.smart.chat.platform.application.AdminNotifyService;
import org.springframework.stereotype.Component;

/** identity 声明的外呼端口由 platform 实现：渠道配置与发送细节都不外泄。 */
@Component
public class AdminNotifyChannelAdapter implements AdminNotifyChannel {

    private final AdminNotifyService notifyService;

    public AdminNotifyChannelAdapter(AdminNotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @Override
    public void pushTextAsync(String title, String body) {
        notifyService.pushTextAsync(title, body);
    }

    @Override
    public void applicationReviewed(String username, boolean approved, String reviewer) {
        notifyService.notifyApplicationReviewed(username, approved, reviewer);
    }
}
