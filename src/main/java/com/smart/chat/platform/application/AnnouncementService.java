package com.smart.chat.platform.application;

import com.smart.chat.platform.domain.announcement.Announcement;
import com.smart.chat.platform.domain.announcement.AnnouncementRead;
import com.smart.chat.platform.domain.announcement.AnnouncementReadRepository;
import com.smart.chat.platform.domain.announcement.AnnouncementRepository;
import com.smart.chat.platform.domain.RuleViolation;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.AnnouncementBroadcaster;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 88 全站公告：管理员发布/关闭；用户端拉取当前生效公告，已读后不再展示。
 * 新公告通过 WebSocket 实时推给在线用户。
 * <p>
 * 用例编排器：取数一律经 {@link AnnouncementRepository} / {@link AnnouncementReadRepository} 端口，
 * 内容闸门与「发布即顶掉旧公告」的状态迁移在 {@code platform.domain.announcement.Announcement} 里，
 * 领域违规经 {@link DomainRules} 翻译成对外 {@link BusinessException}（文案与 code 原样）。
 */
@Service
public class AnnouncementService {

    public record AnnouncementVO(String id, String content, String createdBy, Long created, boolean read) {
    }

    /**
     * 管理端公告列表的投影：字段名与改造前直接序列化持久化模型时完全一致
     * （fastjson2 按字段名字典序输出，故顺序也一致）。
     */
    public record AnnouncementAdminVO(String id, String content, String createdBy, boolean enabled, long created) {
    }

    private final AnnouncementRepository announcements;
    private final AnnouncementReadRepository reads;
    private final AnnouncementBroadcaster broadcaster;

    public AnnouncementService(AnnouncementRepository announcements, AnnouncementReadRepository reads,
                               AnnouncementBroadcaster broadcaster) {
        this.announcements = announcements;
        this.reads = reads;
        this.broadcaster = broadcaster;
    }

    /** 发布新公告：同一时刻只有最新一条生效（发布即关闭旧公告） */
    public AnnouncementVO publish(String actor, String content) {
        Announcement fresh = DomainRules.rule(() -> Announcement.post(content, actor));
        List<Announcement> window = announcements.listRecent();
        List<Announcement> superseded = DomainRules.rule(() -> fresh.supersede(window));
        for (Announcement retired : superseded) {
            announcements.save(retired);
        }
        announcements.save(fresh);
        // 在线用户实时收到（unread 未读标记由客户端按已读记录判断）
        broadcaster.publishAnnouncement(fresh.id(), fresh.content());
        return toVO(fresh, false);
    }

    /**
     * 关闭一条公告。谁能关闭（管理员角色）在 api 层经 identity 的 AccountDirectory 判，
     * 这里只负责取聚合、调领域方法、存回。
     */
    public void close(String actor, String id) {
        Announcement announcement = requireAnnouncement(id);
        announcement.close();
        announcements.save(announcement);
    }

    public List<AnnouncementAdminVO> all() {
        return announcements.listRecent().stream().map(AnnouncementService::toAdminVO).toList();
    }

    /** 当前生效公告（含我是否已读），没有返回 null 语义由 Optional 表达 */
    public AnnouncementVO current(String username) {
        return announcements.findActive()
                .map(a -> toVO(a, reads.hasRead(username, a.id())))
                .orElse(null);
    }

    public void markRead(String username, String announcementId) {
        requireAnnouncement(announcementId);
        if (!reads.hasRead(username, announcementId)) {
            reads.save(DomainRules.rule(() -> AnnouncementRead.confirm(username, announcementId)));
        }
    }

    private Announcement requireAnnouncement(String id) {
        return DomainRules.rule(() -> announcements.findById(id)
                .orElseThrow(() -> new RuleViolation("公告不存在", true)));
    }

    private AnnouncementVO toVO(Announcement announcement, boolean read) {
        return new AnnouncementVO(announcement.id(), announcement.content(),
                announcement.createdBy(), announcement.created(), read);
    }

    private static AnnouncementAdminVO toAdminVO(Announcement announcement) {
        return new AnnouncementAdminVO(announcement.id(), announcement.content(), announcement.createdBy(),
                announcement.enabled(), announcement.created());
    }
}
