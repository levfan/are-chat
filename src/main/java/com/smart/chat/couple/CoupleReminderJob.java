package com.smart.chat.couple;

import com.smart.chat.im.ImPushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 情侣空间定时提醒：
 * 1）09:00 约定逾期提醒——扫描「待兑现且过了截止时间」的承诺卡，
 *    给被承诺的一方发可爱提醒「还有 N 件事你没做到哦~」（lastRemindDay 去重）；
 * 2）09:30 纪念日倒数提醒——扫描在在一起纪念日与共同日历里的纪念日，
 *    在提前 7 天 / 1 天 / 当天推送给双方（每天只跑一次，天然按天去重）。
 */
@Component
public class CoupleReminderJob {

    private static final Logger log = LoggerFactory.getLogger(CoupleReminderJob.class);

    private final CoupleSpaceMapper spaceMapper;
    private final CouplePromiseMapper promiseMapper;
    private final CoupleAnniversaryMapper anniversaryMapper;
    private final CoupleCareService careService;
    private final CoupleMemoryService memoryService;
    private final ImPushService push;

    public CoupleReminderJob(CoupleSpaceMapper spaceMapper, CouplePromiseMapper promiseMapper,
                             CoupleAnniversaryMapper anniversaryMapper, CoupleCareService careService,
                             CoupleMemoryService memoryService, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.promiseMapper = promiseMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.careService = careService;
        this.memoryService = memoryService;
        this.push = push;
    }

    /** 每天 09:00（Asia/Shanghai）提醒一次逾期未兑现的约定。 */
    @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Shanghai")
    public void remindOverdue() {
        long now = System.currentTimeMillis();
        String today = LocalDate.now().toString();
        // 只提醒生效中的情侣空间：已解除的关系不再打扰
        Set<String> activeSpaceIds = spaceMapper.findAllActive().stream()
                .map(CoupleSpace::getId)
                .collect(Collectors.toSet());
        if (activeSpaceIds.isEmpty()) {
            return;
        }
        List<CouplePromise> overdue = promiseMapper.findPendingWithDueBefore(now).stream()
                .filter(p -> activeSpaceIds.contains(p.getSpaceId()))
                .toList();
        // 按承诺人聚合：一次性给出「还有 N 件事」的汇总提醒（没做到的人自己收提醒）
        Map<String, List<String>> byPromiser = new LinkedHashMap<>();
        int reminded = 0;
        for (CouplePromise promise : overdue) {
            if (today.equals(promise.getLastRemindDay())) {
                continue;
            }
            promise.setLastRemindDay(today);
            promiseMapper.updateById(promise);
            byPromiser.computeIfAbsent(promise.getPromiser(), k -> new ArrayList<>())
                    .add("「" + promise.getContent() + "」");
            reminded++;
        }
        for (Map.Entry<String, List<String>> entry : byPromiser.entrySet()) {
            List<String> items = entry.getValue();
            String joined = String.join("、", items);
            String detail = items.size() == 1
                    ? "还有 1 件事你没做到哦~：" + joined + " 😉"
                    : "还有 " + items.size() + " 件事你没做到哦~：" + joined;
            push.pushCoupleEvent("promise-overdue", "system", entry.getKey(), detail);
        }
        if (reminded > 0) {
            log.info("情侣约定逾期提醒完成：提醒 {} 条约定", reminded);
        }
    }

    /** 每天 10:00（Asia/Shanghai）情绪急救箱：TA 连续 2 天低落时提醒对方哄一哄。 */
    @Scheduled(cron = "0 0 10 * * ?", zone = "Asia/Shanghai")
    public void remindLowMoods() {
        careService.remindLowMoods();
    }

    /** 每天 09:45（Asia/Shanghai）倒数日提醒：期待的事倒数 7/3/1/0 天时提醒双方。 */
    @Scheduled(cron = "0 45 9 * * ?", zone = "Asia/Shanghai")
    public void remindCountdowns() {
        memoryService.remindCountdowns();
    }

    /** 每天 09:30（Asia/Shanghai）检查纪念日倒数：提前 7 天 / 1 天 / 当天各提醒一次。 */
    @Scheduled(cron = "0 30 9 * * ?", zone = "Asia/Shanghai")
    public void remindAnniversaryCountdown() {
        String today = LocalDate.now().toString();
        List<CoupleSpace> spaces = spaceMapper.findAllActive();
        int reminded = 0;
        for (CoupleSpace space : spaces) {
            LocalDate now = LocalDate.now();
            // 在一起纪念日（couple_space.anniversary，可空）
            if (space.getAnniversary() != null) {
                reminded += remindCountdown(space, "我们在一起", space.getAnniversary(), true, now);
            }
            // 共同日历纪念日
            for (CoupleAnniversary row : anniversaryMapper.findBySpace(space.getId())) {
                reminded += remindCountdown(space, row.getTitle(), row.getEventDate(), row.isYearly(), now);
            }
        }
        if (reminded > 0) {
            log.info("情侣纪念日倒数提醒完成：推送 {} 条", reminded);
        }
        log.debug("纪念日倒数扫描结束：today={}", today);
    }

    /** 计算某个纪念日距离今天的倒数天数，命中 7/1/0 时给双方推送，返回是否推送。 */
    private int remindCountdown(CoupleSpace space, String title, String dateText, boolean yearly, LocalDate today) {
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
