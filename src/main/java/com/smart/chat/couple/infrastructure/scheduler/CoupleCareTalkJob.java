package com.smart.chat.couple.infrastructure.scheduler;

import com.smart.chat.couple.application.CoupleComfortService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 求抱抱的定时兜底（F65）：23:00 谁今天心情低落还没被接住，就提醒对方去陪陪 TA。
 */
@Component
public class CoupleCareTalkJob {

    private final CoupleComfortService comfortService;

    public CoupleCareTalkJob(CoupleComfortService comfortService) {
        this.comfortService = comfortService;
    }

    /** 每天 23:00（Asia/Shanghai）：深夜陪伴提醒。 */
    @Scheduled(cron = "0 0 23 * * ?", zone = "Asia/Shanghai")
    public void nightCare() {
        comfortService.remindNightCare();
    }
}
