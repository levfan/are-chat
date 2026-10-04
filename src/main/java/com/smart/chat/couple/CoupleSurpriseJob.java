package com.smart.chat.couple;

import com.smart.chat.im.ImPushService;
import com.smart.chat.im.UserProfile;
import com.smart.chat.im.UserProfileMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 情侣空间的生日定时任务（F59/F92）：09:20 生日当天送自动贺卡，生日前 3 天给双方预告。
 * 只依赖空间与资料卡，不依赖任何已下线的功能表。
 */
@Component
public class CoupleSurpriseJob {

    private final CoupleSpaceMapper spaceMapper;
    private final UserProfileMapper profileMapper;
    private final ImPushService push;

    public CoupleSurpriseJob(CoupleSpaceMapper spaceMapper, UserProfileMapper profileMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.profileMapper = profileMapper;
        this.push = push;
    }

    /** 每天 09:20：TA 的生日贺卡（F59，按资料生日）+ 生日前 3 天预告（F92）。 */
    @Scheduled(cron = "0 20 9 * * ?", zone = "Asia/Shanghai")
    public void birthdayCards() {
        String todayMonthDay = LocalDate.now().toString().substring(5);
        String eveMonthDay = LocalDate.now().plusDays(3).toString().substring(5);
        for (CoupleSpace space : spaceMapper.findAllActive()) {
            for (String user : List.of(space.getUserA(), space.getUserB())) {
                UserProfile profile = profileMapper.selectById(user);
                String birthday = profile == null ? null : profile.getBirthday();
                if (birthday == null || birthday.isBlank()) {
                    continue;
                }
                String monthDay = birthday.length() >= 10 ? birthday.substring(5) : birthday;
                // F92 生日前 3 天预告：给双方一个准备惊喜的缓冲
                if (eveMonthDay.equals(monthDay)) {
                    String petName = space.nickOf(user);
                    String who = petName == null || petName.isBlank() ? "TA" : petName;
                    push.pushCoupleEvent("birthday-eve", "system", space.partnerOf(user),
                            "⏳ 小声提醒：3 天后（" + birthday + "）是 " + who + " 的生日——礼物可以慢慢挑，但惊喜要开始准备啦 🎁");
                }
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

    private long daysTogether(CoupleSpace space) {
        if (space.getAnniversary() == null || space.getAnniversary().isBlank()) {
            long since = space.getCreated() == null ? System.currentTimeMillis() : space.getCreated();
            return Math.max(1, (System.currentTimeMillis() - since) / 86_400_000L + 1);
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(space.getAnniversary()), LocalDate.now()) + 1;
    }
}
