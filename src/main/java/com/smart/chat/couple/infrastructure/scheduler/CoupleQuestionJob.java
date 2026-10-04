package com.smart.chat.couple.infrastructure.scheduler;

import com.smart.chat.couple.application.CoupleQuestionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日一问的推送（v8）：每天 09:00 把当天的题目送到双方眼前。
 * 09:00 是现存三条定时任务（09:20 生日贺卡、09:30 纪念日倒数、23:00 深夜陪伴）之外的空档。
 */
@Component
public class CoupleQuestionJob {

    private final CoupleQuestionService questionService;

    public CoupleQuestionJob(CoupleQuestionService questionService) {
        this.questionService = questionService;
    }

    /** 每天 09:00（Asia/Shanghai）：推今日一问，一天只跑一次天然按天去重。 */
    @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Shanghai")
    public void dailyQuestion() {
        questionService.remindDailyQuestion();
    }
}
