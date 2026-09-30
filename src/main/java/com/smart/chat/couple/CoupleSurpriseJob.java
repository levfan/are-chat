package com.smart.chat.couple;

import com.smart.chat.im.ImPushService;
import com.smart.chat.im.UserProfile;
import com.smart.chat.im.UserProfileMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 惊喜与期待的定时任务（F50-F59）：
 * 1）每分钟扫描：到点的心动闹钟（F52）与思念速递（F53），准时替你说那句话；
 * 2）09:15 告白重现（F57）：每年今天的告白词重播一遍；
 * 3）09:20 生日彩蛋（F59）：TA 生日当天送上一张自动贺卡；
 * 4）10:15 爱情花园巡检（F54）：连续 3 天没人浇水时提醒双方。
 */
@Component
public class CoupleSurpriseJob {

    private static final Logger log = LoggerFactory.getLogger(CoupleSurpriseJob.class);

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleSweetAlarmMapper alarmMapper;
    private final CoupleMissExpressMapper missMapper;
    private final CoupleSurpriseService surpriseService;
    private final CoupleGardenService gardenService;
    private final UserProfileMapper profileMapper;
    private final ImPushService push;

    public CoupleSurpriseJob(CoupleSpaceMapper spaceMapper, CoupleSweetAlarmMapper alarmMapper,
                             CoupleMissExpressMapper missMapper, CoupleSurpriseService surpriseService,
                             CoupleGardenService gardenService, UserProfileMapper profileMapper,
                             ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.alarmMapper = alarmMapper;
        this.missMapper = missMapper;
        this.surpriseService = surpriseService;
        this.gardenService = gardenService;
        this.profileMapper = profileMapper;
        this.push = push;
    }

    /** 每分钟：送达到点的心动闹钟与思念速递。 */
    @Scheduled(cron = "0 * * * * ?", zone = "Asia/Shanghai")
    public void fireDue() {
        long now = System.currentTimeMillis();
        for (CoupleSweetAlarm alarm : alarmMapper.findDueUnfired(now)) {
            CoupleSpace space = spaceMapper.selectById(alarm.getSpaceId());
            if (space == null || !CoupleSpace.STATUS_ACTIVE.equals(space.getStatus())) {
                alarm.setFired(true);
                alarm.setFiredAt(now);
                alarmMapper.updateById(alarm);
                continue;
            }
            alarm.setFired(true);
            alarm.setFiredAt(now);
            alarmMapper.updateById(alarm);
            push.pushCoupleEvent("alarm-fired", alarm.getFromUser(), space.partnerOf(alarm.getFromUser()),
                    "⏰ 心动闹钟响啦！TA 托小助手带话说：" + alarm.getMessage());
        }
        for (CoupleMissExpress miss : missMapper.findDueUndelivered(now)) {
            CoupleSpace space = spaceMapper.selectById(miss.getSpaceId());
            if (space == null || !CoupleSpace.STATUS_ACTIVE.equals(space.getStatus())) {
                miss.setDelivered(true);
                miss.setDeliveredAt(now);
                missMapper.updateById(miss);
                continue;
            }
            miss.setDelivered(true);
            miss.setDeliveredAt(now);
            missMapper.updateById(miss);
            push.pushCoupleEvent("miss-delivered", miss.getFromUser(), space.partnerOf(miss.getFromUser()),
                    "📮 思念速递：刚刚，TA 想你了 💗");
        }
    }

    /** 每天 09:15：重播「每年的今天」的告白。 */
    @Scheduled(cron = "0 15 9 * * ?", zone = "Asia/Shanghai")
    public void replayConfessions() {
        surpriseService.replayTodaysConfessions();
    }

    /** 每天 09:20：TA 的生日贺卡（F59，按资料生日）。 */
    @Scheduled(cron = "0 20 9 * * ?", zone = "Asia/Shanghai")
    public void birthdayCards() {
        String todayMonthDay = LocalDate.now().toString().substring(5);
        for (CoupleSpace space : spaceMapper.findAllActive()) {
            for (String user : List.of(space.getUserA(), space.getUserB())) {
                UserProfile profile = profileMapper.selectById(user);
                String birthday = profile == null ? null : profile.getBirthday();
                if (birthday == null || birthday.isBlank()) {
                    continue;
                }
                String monthDay = birthday.length() >= 10 ? birthday.substring(5) : birthday;
                if (!todayMonthDay.equals(monthDay)) {
                    continue;
                }
                String partner = space.partnerOf(user);
                String petName = space.nickOf(user);
                String who = petName == null || petName.isBlank() ? "TA" : petName;
                push.pushCoupleEvent("birthday-card", "system", user,
                        "🎂 生日快乐！这是你和 " + who + " 在一起的第 " + daysTogether(space)
                                + " 天，也是你被爱包围的一天，要开心呀 🎈");
                push.pushCoupleEvent("birthday-card", "system", partner,
                        "🎁 今天是 TA 的生日！小助手提醒你：礼物可以慢慢挑，祝福现在就要说出口～ 记得让 TA 今天最开心 💝");
            }
        }
    }

    /** 每天 10:15：爱情花园缺水巡检。 */
    @Scheduled(cron = "0 15 10 * * ?", zone = "Asia/Shanghai")
    public void checkGardens() {
        gardenService.checkWither();
    }

    /** 在一起天数：从纪念日（缺省取建立日）算到今天，含当天（与 CoupleService 口径一致）。 */
    private long daysTogether(CoupleSpace space) {
        LocalDate start;
        try {
            start = LocalDate.parse(space.getAnniversary());
        } catch (Exception e) {
            start = java.time.Instant.ofEpochMilli(space.getCreated())
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.now()) + 1;
        return Math.max(days, 1);
    }
}
