package com.smart.chat.room;

import com.alibaba.fastjson2.JSON;

/**
 * WS 消息协议（fastjson2 序列化），作为 IM 的实时推送通道：
 * type=system    系统提示
 * type=heart     心跳请求（兼容老客户端直接发 "heartBeat" 字符串）
 * type=heart-ack 心跳应答
 * type=typing    IM「对方正在输入」转发 {name:发送者, subject:接收者, msg:"1"/"0"}
 * type=presence  IM 上下线广播 {name:用户, msg:"1"/"0"}
 */
public record ChatMessage(String type, String name, String subject, String msg, Long ts) {

    private static long now() {
        return System.currentTimeMillis();
    }

    public static ChatMessage system(String subject, String msg) {
        return new ChatMessage("system", "系统提示", subject, msg, now());
    }

    public static ChatMessage heartAck() {
        return new ChatMessage("heart-ack", null, null, null, now());
    }

    public static ChatMessage typing(String name, String subject, boolean typing) {
        return new ChatMessage("typing", name, subject, typing ? "1" : "0", now());
    }

    public static ChatMessage presence(String name, boolean online) {
        return new ChatMessage("presence", name, null, online ? "1" : "0", now());
    }

    public String toJson() {
        return JSON.toJSONString(this);
    }
}
