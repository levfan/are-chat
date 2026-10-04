package com.smart.chat.messaging.domain;

/**
 * 出站帧的旁路存档钩子（F41 通知中心落库就是它）。
 * 钩子接口由 messaging 拥有，实现方在别的上下文——这样存档规则不外泄，也不让 messaging 反向认识 couple。
 */
public interface OutboundNotifySink {

    void record(String event, String actor, String toUser, String detail);
}

