package com.smart.chat.platform.domain.notify;

/**
 * 通知渠道配置的取数端口：AdminNotifyService 要知道「渠道配了什么」，但不该认识
 * {@code bootstrap.properties.NotifyProperties} 这类装配类型——配置从哪儿来（yml / 环境变量 / 将来换库）
 * 是 infrastructure 的事，由 {@code NotifyChannelConfigAdapter} 实现本端口。
 * <p>
 * 返回值允许为 null 或空白：「配了没配」的裁决在 {@link EnabledNotifyChannels}，不在这里。
 */
public interface NotifyChannelConfig {

    /** 企业微信群机器人 Webhook 地址 */
    String wecomWebhook();

    /** WxPusher 应用 token */
    String wxpusherAppToken();

    /** WxPusher 接收人 uid 列表（逗号/分号分隔） */
    String wxpusherUids();

    /** Server酱 Turbo SendKey */
    String serverchanSendkey();

    /** 虾推啥 SendKey */
    String xtuisSendkey();
}
