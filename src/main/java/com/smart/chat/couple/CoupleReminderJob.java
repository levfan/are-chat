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
 * 约定逾期提醒：每天早上扫描「待兑现且过了截止时间」的承诺卡，
 * 给被承诺的一方发可爱提醒「还有 N 件事你没做到哦~」。
 * 每条约定每天最多提醒一次（lastRemindDay 去重），兑现后自动停提。
 */
@Component
public class CoupleReminderJob {

    private static final Logger log = LoggerFactory.getLogger(CoupleReminderJob.class);

    private final CoupleSpaceMapper spaceMapper;
    private final CouplePromiseMapper promiseMapper;
    private final ImPushService push;

    public CoupleReminderJob(CoupleSpaceMapper spaceMapper, CouplePromiseMapper promiseMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.promiseMapper = promiseMapper;
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
}
