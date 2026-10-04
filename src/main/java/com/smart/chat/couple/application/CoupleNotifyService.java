package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.notify.NotifyEntry;
import com.smart.chat.couple.domain.notify.NotifyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知中心（F41）：情侣事件的落库副本，供离线补看。
 * <p>
 * 「推不推、推给谁」不在这里——那是 {@code messaging.domain.CoupleEventPublisher} 那一侧的决策；
 * 本用例只负责把已经发生过的事件读出来、以及把未读清掉。
 */
@Service
public class CoupleNotifyService {

    public record NotifyVO(String id, String event, String actor, String detail, boolean read, Long created) {
    }

    public record NotifyListVO(List<NotifyVO> items, long unread) {
    }

    private final NotifyRepository notifyRepository;

    public CoupleNotifyService(NotifyRepository notifyRepository) {
        this.notifyRepository = notifyRepository;
    }

    /** 我的最近通知 + 未读数。 */
    public NotifyListVO mine(String me) {
        List<NotifyVO> items = notifyRepository.findRecent(me).stream()
                .map(n -> new NotifyVO(n.id(), n.event(), n.actor(), n.detail(), !n.unread(), n.created()))
                .toList();
        return new NotifyListVO(items, notifyRepository.countUnread(me));
    }

    /** 全部标记已读。 */
    public void markAllRead(String me) {
        notifyRepository.markAllRead(me);
    }

    /** 供基础设施写一条副本（CoupleNotifyRecorder 挂在推送门面上）。 */
    public void record(NotifyEntry entry) {
        notifyRepository.append(entry);
    }
}
