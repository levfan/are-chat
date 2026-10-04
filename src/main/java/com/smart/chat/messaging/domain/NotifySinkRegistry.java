package com.smart.chat.messaging.domain;

/** 注册出站存档钩子（原 ImPushService.setNotifySink 的接口化版本）。 */
public interface NotifySinkRegistry {

    void register(OutboundNotifySink sink);
}
