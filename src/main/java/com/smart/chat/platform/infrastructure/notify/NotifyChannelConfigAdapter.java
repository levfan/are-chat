package com.smart.chat.platform.infrastructure.notify;

import com.smart.chat.bootstrap.properties.NotifyProperties;
import com.smart.chat.platform.domain.notify.NotifyChannelConfig;
import org.springframework.stereotype.Component;

/**
 * 通知渠道配置的出站适配器：把 {@code arechat.notify.*} 这装配层的值绑定成领域端口
 * {@link NotifyChannelConfig}，这样 AdminNotifyService 只依赖端口，不认识 {@code @ConfigurationProperties}。
 * 这里<b>不做任何裁决</b>（「配没配齐」在 {@code EnabledNotifyChannels}），只负责搬运原始值。
 */
@Component
public class NotifyChannelConfigAdapter implements NotifyChannelConfig {

    private final NotifyProperties properties;

    public NotifyChannelConfigAdapter(NotifyProperties properties) {
        this.properties = properties;
    }

    @Override
    public String wecomWebhook() {
        return properties.wecomWebhook();
    }

    @Override
    public String wxpusherAppToken() {
        return properties.wxpusherAppToken();
    }

    @Override
    public String wxpusherUids() {
        return properties.wxpusherUids();
    }

    @Override
    public String serverchanSendkey() {
        return properties.serverchanSendkey();
    }

    @Override
    public String xtuisSendkey() {
        return properties.xtuisSendkey();
    }
}
