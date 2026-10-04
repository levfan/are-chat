package com.smart.chat.identity.domain;

/**
 * 系统欢迎消息：审批通过后要发一条「登录即可见」的站内消息。
 * 消息落库与在线推送都是 messaging 的事，identity 只交代「发给谁、以谁的名义、说什么」。
 */
public interface WelcomeMessenger {

    void sendSystemWelcome(String reviewer, String toUser, String content);
}
