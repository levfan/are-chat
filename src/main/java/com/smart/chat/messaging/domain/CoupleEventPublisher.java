package com.smart.chat.messaging.domain;

/**
 * 情侣事件推送：messaging 对外的发布语言。
 * 谁都要推 WS，但别人不该 import 传输实现——「帧长什么样、离线了怎么补存」是 messaging 的私事。
 * 方法名沿用 ImPushService 现有签名，改动只是把依赖从实现类换成接口，行为零变化。
 */
public interface CoupleEventPublisher {

    boolean isOnline(String username);

    void pushCoupleEvent(String event, String actor, String toUser, String detail);

    void pushCoupleEventBoth(String event, String actor, String userA, String userB, String detail);
}
