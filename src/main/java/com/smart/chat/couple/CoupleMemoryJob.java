package com.smart.chat.couple;

import com.smart.chat.im.ImPushService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 回忆资产定时任务（F87）：每天 09:05 扫描今日到期的时光胶囊，
 * 给双方推送开胶囊提醒——约定好的那天，一句都不能少。
 */
@Component
public class CoupleMemoryJob {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleCapsuleMapper capsuleMapper;
    private final ImPushService push;

    public CoupleMemoryJob(CoupleSpaceMapper spaceMapper, CoupleCapsuleMapper capsuleMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.capsuleMapper = capsuleMapper;
        this.push = push;
    }

    /** 每天 09:05（Asia/Shanghai）：今日到期胶囊提醒。 */
    @Scheduled(cron = "0 5 9 * * ?", zone = "Asia/Shanghai")
    public void remindDueCapsules() {
        String today = LocalDate.now().toString();
        for (CoupleSpace space : spaceMapper.findAllActive()) {
            List<CoupleCapsule> due = capsuleMapper.findByOpenDay(space.getId(), today);
            for (CoupleCapsule capsule : due) {
                String detail = "⏰ 今天是时光胶囊的开启日（" + capsule.getOpenDay() + "）！"
                        + (capsule.getSender().equals(space.getUserA()) ? space.getUserB() : space.getUserA())
                        + " 快去拆开 TA 封存的信 💌";
                push.pushCoupleEventBoth("capsule-due", "system", space.getUserA(), space.getUserB(), detail);
            }
        }
    }
}
