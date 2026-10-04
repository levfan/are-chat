package com.smart.chat.messaging.domain;

/** 好友关系判定：邀请要「只能是好友」，但 FriendMapper 不该被情侣空间直接摸。 */
public interface FriendshipChecker {

    boolean areFriends(String owner, String friend);
}
