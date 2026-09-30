package com.smart.chat.couple;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 懂我与被接住的定时任务（F65/F69）：
 * 1）21:00 情话储蓄罐利息——每人随机取一句未投递的情话送给 TA；
 * 2）23:00 深夜陪伴——谁今天心情低落还没被接住，就提醒对方去陪陪 TA。
 */
@Component
public class CoupleCareTalkJob {

    private final CoupleTalkService talkService;
    private final CoupleComfortService comfortService;

    public CoupleCareTalkJob(CoupleTalkService talkService, CoupleComfortService comfortService) {
        this.talkService = talkService;
        this.comfortService = comfortService;
    }

    /** 每天 21:00（Asia/Shanghai）：情话储蓄罐的利息送达。 */
    @Scheduled(cron = "0 0 21 * * ?", zone = "Asia/Shanghai")
    public void deliverLoveInterest() {
        talkService.deliverInterest();
    }

    /** 每天 23:00（Asia/Shanghai）：深夜陪伴提醒。 */
    @Scheduled(cron = "0 0 23 * * ?", zone = "Asia/Shanghai")
    public void nightCare() {
        comfortService.remindNightCare();
    }
}
