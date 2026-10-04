package com.smart.chat.couple.domain.notify;

import java.util.List;

/** 情侣通知落库副本的仓储端口。 */
public interface NotifyRepository {

    /** 某人最近的通知（新→旧，现役口径是最多 50 条） */
    List<NotifyEntry> findRecent(String username);

    long countUnread(String username);

    /** 全部标记已读，返回影响条数 */
    int markAllRead(String username);

    /** 只追加：通知一旦落库就是发生过的事实，不改内容 */
    void append(NotifyEntry entry);
}
