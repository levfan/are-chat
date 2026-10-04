package com.smart.chat.platform.domain.announcement;

/**
 * 公告已读记录的仓储端口（薄实体只需要两件事：判重与追加）。
 */
public interface AnnouncementReadRepository {

    /** 这个人是否已经读过这条公告（一人一公告一行，判重口径见 uq_ann_read）。 */
    boolean hasRead(String username, String announcementId);

    /** 追加一条已读记录；已存在同一行时不重复插入。 */
    void save(AnnouncementRead read);
}
