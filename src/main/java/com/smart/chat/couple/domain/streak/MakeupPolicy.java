package com.smart.chat.couple.domain.streak;

import com.smart.chat.couple.domain.RuleViolation;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 补签规则：断签可以补，但补签是「花钱买回一段连续」，不是无限回溯。
 * <p>
 * 三条闸门都在领域里，Service 只负责取数：
 * 七天窗口（太久以前的日子补回来没有情绪价值，也骗不了自己）、
 * 每个自然月最多三次（否则积分充足就等于不会断签）、
 * 余额不足要说清楚去哪儿赚分。
 */
public final class MakeupPolicy {

    /** 补一次要花的积分（与愿望券的 10 分同台账口径，补签更值钱因为它直接换解锁进度） */
    public static final int COST = 20;
    /** 只能补往前 7 天以内的缺口 */
    public static final int WINDOW_DAYS = 7;
    /** 一个自然月最多补 3 次 */
    public static final int MONTHLY_QUOTA = 3;

    private MakeupPolicy() {
    }

    /**
     * 这天能不能补：闸门不过就抛产品原话。
     *
     * @param day        想补的日子
     * @param today      今天
     * @param alreadySet 这个日子是否已经是打卡日（含之前补过的）
     * @param usedThisMonth 本自然月已经补过的次数
     * @param balance    补签人当前积分余额
     */
    public static void assertAllowed(LocalDate day, LocalDate today, boolean alreadySet,
                                     int usedThisMonth, int balance) {
        if (day == null) {
            throw new RuleViolation("想补哪一天？日期没给呢");
        }
        if (!day.isBefore(today)) {
            throw new RuleViolation("今天还不能补——贴一贴就算打卡，不用花钱 😉");
        }
        if (day.isBefore(today.minusDays(WINDOW_DAYS))) {
            throw new RuleViolation("只能补最近 " + WINDOW_DAYS + " 天里的缺口，太久以前的那天就让它过去吧");
        }
        if (alreadySet) {
            throw new RuleViolation(day + " 已经打过卡了，不用补");
        }
        if (usedThisMonth >= MONTHLY_QUOTA) {
            throw new RuleViolation("这个月已经补过 " + MONTHLY_QUOTA + " 次了，下个月再来吧（"
                    + YearMonth.from(today) + "）");
        }
        if (balance < COST) {
            throw new RuleViolation("补一次签要 " + COST + " 分，你只有 " + balance
                    + " 分——先去好事簿记一笔 TA 为你做过的事吧");
        }
    }

    /** 解析并校验日期字符串（对外只收 yyyy-MM-dd）。 */
    public static LocalDate parseDay(String value) {
        try {
            return LocalDate.parse(value == null ? "" : value.trim());
        } catch (Exception e) {
            throw new RuleViolation("日期格式应为 yyyy-MM-dd");
        }
    }
}
