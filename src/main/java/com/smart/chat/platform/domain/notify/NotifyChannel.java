package com.smart.chat.platform.domain.notify;

/**
 * 管理员站外通知渠道（78）。枚举值的 {@code wireName} 就是 {@code ChannelResult.channel} 里
 * 落日志的字符串，也是运维配置的说法——属于对外契约，不因分层而改名。
 * <p>
 * 声明顺序即发送顺序（企微 → WxPusher → Server酱 → 虾推啥），改造前后一致。
 */
public enum NotifyChannel {

    WECOM("wecom"),
    WXPUSHER("wxpusher"),
    SERVERCHAN("serverchan"),
    XTUIS("xtuis");

    private final String wireName;

    NotifyChannel(String wireName) {
        this.wireName = wireName;
    }

    /** 日志与结果里出现的渠道名 */
    public String wireName() {
        return wireName;
    }
}
