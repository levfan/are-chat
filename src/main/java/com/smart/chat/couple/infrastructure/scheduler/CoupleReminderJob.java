package com.smart.chat.couple.infrastructure.scheduler;

import com.smart.chat.couple.infrastructure.persistence.CoupleAnniversaryPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleAnniversaryMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpacePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 情侣空间定时提醒：09:30 纪念日倒数——扫描在一起纪念日与共同日历里的纪念日，
 * 在提前 7 天 / 1 天 / 当天推送给双方（每天只跑一次，天然按天去重）。
 */
@Component
public class CoupleReminderJob {

    private static final Logger log = LoggerFactory.getLogger(CoupleReminderJob.class);

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleAnniversaryMapper anniversaryMapper;
    private final CoupleEventPublisher push;

    public CoupleReminderJob(CoupleSpaceMapper spaceMapper, CoupleAnniversaryMapper anniversaryMapper,
                             CoupleEventPublisher push) {
        this.spaceMapper = spaceMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.push = push;
    }

    /** 每天 09:30（Asia/Shanghai）检查纪念日倒数：提前 7 天 / 1 天 / 当天各提醒一次。 */
    @Scheduled(cron = "0 30 9 * * ?", zone = "Asia/Shanghai")
    public void remindAnniversaryCountdown() {
        String today = LocalDate.now().toString();
        List<CoupleSpacePO> spaces = spaceMapper.findAllActive();
        int reminded = 0;
        for (CoupleSpacePO space : spaces) {
            LocalDate now = LocalDate.now();
            // 在一起纪念日（couple_space.anniversary，可空）
            if (space.getAnniversary() != null) {
                reminded += remindCountdown(space, "我们在一起", space.getAnniversary(), true, now);
            }
            // 共同日历纪念日
            for (CoupleAnniversaryPO row : anniversaryMapper.findBySpace(space.getId())) {
                reminded += remindCountdown(space, row.getTitle(), row.getEventDate(), row.yearlyFlag(), now);
            }
        }
        if (reminded > 0) {
            log.info("情侣纪念日倒数提醒完成：推送 {} 条", reminded);
        }
        log.debug("纪念日倒数扫描结束：today={}", today);
    }

    /** 计算某个纪念日距离今天的倒数天数，命中 7/1/0 时给双方推送，返回是否推送。 */
    private int remindCountdown(CoupleSpacePO space, String title, String dateText, boolean yearly, LocalDate today) {
        LocalDate occurrence = nextOccurrence(dateText, yearly, today);
        if (occurrence == null) {
            return 0;
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(today, occurrence);
        String detail;
        if (days == 0) {
            detail = "今天是「" + title + "」🎊 大日子，好好庆祝呀！";
        } else if (days == 1) {
            detail = "明天就是「" + title + "」啦 🎉 记得准备小惊喜哦";
        } else if (days == 7) {
            detail = "一周后就是「" + title + "」🎉 可以开始悄悄准备小惊喜了";
        } else {
            return 0;
        }
        push.pushCoupleEventBoth("anniversary-reminder", "system", space.getUserA(), space.getUserB(), detail);
        return 1;
    }

    /**
     * 纪念日的下一次落位日期：yearly 按今年（已过则明年）；非 yearly 只认未来/当天的一次性日期；
     * 2/29 在平年落到 2/28；解析失败或已过期返回 null。
     */
    private LocalDate nextOccurrence(String dateText, boolean yearly, LocalDate today) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateText);
        } catch (Exception e) {
            return null;
        }
        if (yearly) {
            date = withYearSafe(date, today.getYear());
            if (date.isBefore(today)) {
                date = withYearSafe(date, today.getYear() + 1);
            }
        } else if (date.isBefore(today)) {
            return null;
        }
        return date;
    }

    /** 落位到指定年份：2/29 遇平年落到 2/28（LocalDate.withYear 对非法日期会抛异常）。 */
    private LocalDate withYearSafe(LocalDate date, int year) {
        if (date.getMonthValue() == 2 && date.getDayOfMonth() == 29 && !java.time.Year.isLeap(year)) {
            return LocalDate.of(year, 2, 28);
        }
        return date.withYear(year);
    }
}
