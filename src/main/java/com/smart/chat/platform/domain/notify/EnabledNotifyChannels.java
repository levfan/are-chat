package com.smart.chat.platform.domain.notify;

import java.util.ArrayList;
import java.util.List;

/**
 * 「哪些渠道可用」的裁决（无状态策略对象）：
 * <ul>
 *   <li>企微：有 webhook 地址即可；</li>
 *   <li>WxPusher：appToken 与接收人 uid <b>两个都得有</b>，只配一半等于没配；</li>
 *   <li>Server酱 / 虾推啥：各有 SendKey 即可。</li>
 * </ul>
 * 改造前这四条判断长在 AdminNotifyService 的 {@code notBlank(...)} if 里，按战术手法（05 第 2.2/第 4 条）
 * 下沉到这里；发送顺序同样由本类型给出（枚举声明顺序），Service 只按顺序逐个试。
 * 一个渠道都没配不是错误：审批待办始终在管理员控制台里可见，推送只是「更快知道」。
 */
public final class EnabledNotifyChannels {

    private EnabledNotifyChannels() {
    }

    /** 已配置、可用发送的渠道（顺序即发送顺序）。 */
    public static List<NotifyChannel> of(NotifyChannelConfig config) {
        List<NotifyChannel> enabled = new ArrayList<>();
        for (NotifyChannel channel : NotifyChannel.values()) {
            if (isConfigured(channel, config)) {
                enabled.add(channel);
            }
        }
        return enabled;
    }

    /** 一个渠道都没配（调用方可据此走纯站内提醒）。 */
    public static boolean noneConfigured(NotifyChannelConfig config) {
        return of(config).isEmpty();
    }

    /** 单个渠道的配置是否齐备。 */
    public static boolean isConfigured(NotifyChannel channel, NotifyChannelConfig config) {
        return switch (channel) {
            case WECOM -> notBlank(config.wecomWebhook());
            case WXPUSHER -> notBlank(config.wxpusherAppToken()) && notBlank(config.wxpusherUids());
            case SERVERCHAN -> notBlank(config.serverchanSendkey());
            case XTUIS -> notBlank(config.xtuisSendkey());
        };
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
