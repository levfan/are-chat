package com.smart.chat.platform.domain.announcement;

import java.util.List;
import java.util.Optional;

/**
 * 公告聚合的仓储端口：领域只说「当前生效的那条」「最近的列表」「把它存回去」，
 * 走哪个 Mapper、LIMIT 多少、哪一列归谁写，都是 infrastructure 的事。
 */
public interface AnnouncementRepository {

    /** 当前生效的公告（最新一条 enabled）；没有则空。 */
    Optional<Announcement> findActive();

    /** 最近的公告列表（创建时间倒序，范围沿用现役读法）。 */
    List<Announcement> listRecent();

    /** 按 id 取一条。 */
    Optional<Announcement> findById(String id);

    /**
     * 存回聚合：id 不存在则整行插入，存在则<b>只回写聚合纳管的那一列</b>（enabled）——
     * 正文、发布人、发布时间发布即冻结，聚合没有改写它们的方法，适配器也不许顺手重建整行。
     */
    void save(Announcement announcement);
}
