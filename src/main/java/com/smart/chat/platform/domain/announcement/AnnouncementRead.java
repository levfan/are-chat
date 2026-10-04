package com.smart.chat.platform.domain.announcement;

import com.smart.chat.platform.domain.RuleViolation;

import java.util.UUID;

/**
 * 公告已读记录：某人对某条公告点过「我知道了」。
 * <p>
 * 这是 append-only 流水表，按 {@code docs/ddd/05-tactical-playbook.md} 2.2 条做成<b>薄实体</b>——
 * 只有校验过的工厂与访问器，不编造行为（没有「撤销已读」「重读」这类用例）。
 * 判重口径（一人一公告一行，uq_ann_read）由仓储端口的 {@code hasRead} 守，见
 * {@code AnnouncementService.markRead} 的编排。
 */
public final class AnnouncementRead {

    private final String id;
    private final String username;
    private final String announcementId;
    private final long readAt;

    private AnnouncementRead(String id, String username, String announcementId, long readAt) {
        this.id = id;
        this.username = username;
        this.announcementId = announcementId;
        this.readAt = readAt;
    }

    /** 确认已读：现场发 id 与已读时刻（口径与改造前一致）。公告必须先存在，否则对外 404。 */
    public static AnnouncementRead confirm(String username, String announcementId) {
        if (announcementId == null || announcementId.isBlank()) {
            throw new RuleViolation("公告不存在", true);
        }
        return new AnnouncementRead(UUID.randomUUID().toString(), username, announcementId,
                System.currentTimeMillis());
    }

    /** 从存储重建：不校验。 */
    public static AnnouncementRead restore(String id, String username, String announcementId, Long readAt) {
        return new AnnouncementRead(id, username, announcementId, readAt == null ? 0L : readAt);
    }

    public String id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String announcementId() {
        return announcementId;
    }

    public long readAt() {
        return readAt;
    }
}
