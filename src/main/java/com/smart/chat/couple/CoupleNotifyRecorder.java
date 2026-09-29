package com.smart.chat.couple;

import com.smart.chat.im.ImPushService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * F41 通知中心存档器：把每一条情侣空间事件推送落库到 couple_notify，
 * 收件人下次登录也能在通知中心补看。通过 ImPushService 的注册钩子挂接，
 * 保持 im 包不反向依赖 couple 包。
 */
@Component
public class CoupleNotifyRecorder {

    private final CoupleNotifyMapper notifyMapper;
    private final ImPushService push;

    public CoupleNotifyRecorder(CoupleNotifyMapper notifyMapper, ImPushService push) {
        this.notifyMapper = notifyMapper;
        this.push = push;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        push.setNotifySink(this::record);
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
