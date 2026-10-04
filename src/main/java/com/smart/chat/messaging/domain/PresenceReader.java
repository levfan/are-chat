package com.smart.chat.messaging.domain;

import java.util.Set;

/** 在线名册的只读视图（健康检查、运营看板要数在线人数，不该摸注册表实现）。 */
public interface PresenceReader {

    Set<String> onlineUsers();
}
