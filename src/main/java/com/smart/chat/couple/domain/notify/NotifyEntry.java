package com.smart.chat.couple.domain.notify;

import java.util.UUID;

/**
 * 一条情侣通知的落库副本：谁（username）因为哪个事件（event）、由谁（actor）触发、详情是什么。
 * <p>
 * 薄实体——它是 WS 帧在存储里的镜像，供离线补看；「要不要推、推给谁」的决策在
 * {@code messaging.domain.CoupleEventPublisher} 那一侧，这里只记录已经发生过的事。
 */
public final class NotifyEntry {

    private final String id;
    private final String username;
    private final String event;
    private final String actor;
    private final String detail;
    private final int readFlag;
    private final long created;

    private NotifyEntry(String id, String username, String event, String actor, String detail, int readFlag,
                        long created) {
        this.id = id;
        this.username = username;
        this.event = event;
        this.actor = actor;
        this.detail = detail;
        this.readFlag = readFlag;
        this.created = created;
    }

    /** 新落一条通知（未读） */
    public static NotifyEntry of(String username, String event, String actor, String detail) {
        return new NotifyEntry(UUID.randomUUID().toString(), username, event, actor, detail, 0,
                System.currentTimeMillis());
    }

    public static NotifyEntry restore(String id, String username, String event, String actor, String detail,
                                      Integer readFlag, Long created) {
        return new NotifyEntry(id, username, event, actor, detail, readFlag == null ? 0 : readFlag,
                created == null ? 0L : created);
    }

    /** 还没看过 */
    public boolean unread() {
        return readFlag == 0;
    }

    public String id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String event() {
        return event;
    }

    public String actor() {
        return actor;
    }

    public String detail() {
        return detail;
    }

    public int readFlag() {
        return readFlag;
    }

    public long created() {
        return created;
    }
}
