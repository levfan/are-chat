package com.smart.chat.couple.infrastructure.notify;

import com.smart.chat.couple.infrastructure.persistence.CoupleNotify;
import com.smart.chat.couple.infrastructure.persistence.CoupleNotifyMapper;
import com.smart.chat.messaging.domain.NotifySinkRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * F41 通知中心存档器：把每一条情侣空间事件推送落库到 couple_notify，
 * 收件人下次登录也能在通知中心补看。通过 messaging 声明的钩子接口挂上去，
 * 传输实现不认识 couple，couple 也不认识传输实现。
 */
@Component
public class CoupleNotifyRecorder {

    private final CoupleNotifyMapper notifyMapper;
    private final NotifySinkRegistry sinks;

    public CoupleNotifyRecorder(CoupleNotifyMapper notifyMapper, NotifySinkRegistry sinks) {
        this.notifyMapper = notifyMapper;
        this.sinks = sinks;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        sinks.register(this::record);
    }

    /** 事件落库：只存档，不影响推送链路（失败静默，通知缺失可容忍）。 */
    public void record(String event, String actor, String toUser, String detail) {
        try {
            notifyMapper.insert(CoupleNotify.of(toUser, event, actor, detail));
        } catch (Exception ignored) {
            // 通知存档失败不影响业务
        }
    }
}
