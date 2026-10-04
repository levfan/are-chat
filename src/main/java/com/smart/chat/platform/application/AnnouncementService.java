package com.smart.chat.platform.application;

import com.smart.chat.platform.infrastructure.persistence.Announcement;
import com.smart.chat.platform.infrastructure.persistence.AnnouncementMapper;
import com.smart.chat.platform.infrastructure.persistence.AnnouncementRead;
import com.smart.chat.platform.infrastructure.persistence.AnnouncementReadMapper;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.AnnouncementBroadcaster;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 88 全站公告：管理员发布/关闭；用户端拉取当前生效公告，已读后不再展示。
 * 新公告通过 WebSocket 实时推给在线用户。
 */
@Service
public class AnnouncementService {

    public record AnnouncementVO(String id, String content, String createdBy, Long created, boolean read) {
    }

    private final AnnouncementMapper announcementMapper;
    private final AnnouncementReadMapper readMapper;
    private final AnnouncementBroadcaster broadcaster;

    public AnnouncementService(AnnouncementMapper announcementMapper, AnnouncementReadMapper readMapper,
                               AnnouncementBroadcaster broadcaster) {
        this.announcementMapper = announcementMapper;
        this.readMapper = readMapper;
        this.broadcaster = broadcaster;
    }

    /** 发布新公告：同一时刻只有最新一条生效（发布即关闭旧公告） */
    public AnnouncementVO publish(String actor, String content) {
        String clean = content == null ? "" : content.trim();
        if (clean.isEmpty()) {
            throw new BusinessException(400, "公告内容不能为空");
        }
        if (clean.length() > 500) {
            throw new BusinessException(400, "公告最长 500 个字");
        }
        for (Announcement old : announcementMapper.findAllOrdered()) {
            if (Boolean.TRUE.equals(old.getEnabled())) {
                old.setEnabled(false);
                announcementMapper.updateById(old);
            }
        }
        Announcement announcement = Announcement.of(clean, actor);
        announcementMapper.insert(announcement);
        // 在线用户实时收到（unread 未读标记由客户端按已读记录判断）
        broadcaster.publishAnnouncement(announcement.getId(), clean);
        return toVO(announcement, false);
    }

    public void close(String actor, String id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException(404, "公告不存在");
        }
        announcement.setEnabled(false);
        announcementMapper.updateById(announcement);
    }

    public List<Announcement> all() {
        return announcementMapper.findAllOrdered();
    }

    /** 当前生效公告（含我是否已读），没有返回 null 语义由 Optional 表达 */
    public AnnouncementVO current(String username) {
        return announcementMapper.findLatestEnabled()
                .map(a -> toVO(a, readMapper.exists(username, a.getId())))
                .orElse(null);
    }

    public void markRead(String username, String announcementId) {
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BusinessException(404, "公告不存在");
        }
        if (!readMapper.exists(username, announcementId)) {
            readMapper.insert(AnnouncementRead.of(username, announcementId));
        }
    }

    private AnnouncementVO toVO(Announcement announcement, boolean read) {
        return new AnnouncementVO(announcement.getId(), announcement.getContent(),
                announcement.getCreatedBy(), announcement.getCreated(), read);
    }
}
