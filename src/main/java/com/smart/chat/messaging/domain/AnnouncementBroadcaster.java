package com.smart.chat.messaging.domain;

/** 公告广播：公告归 platform，但「往所有在线连接刷一帧」这件事只有 messaging 会做。 */
public interface AnnouncementBroadcaster {

    void publishAnnouncement(String announcementId, String content);
}
