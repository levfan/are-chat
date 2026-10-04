package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.streak.BondStreak;
import com.smart.chat.couple.domain.streak.MakeupPolicy;
import com.smart.chat.couple.domain.streak.StreakTier;
import com.smart.chat.couple.domain.bond.ActionRepository;
import com.smart.chat.couple.domain.bond.BondAction;
import com.smart.chat.couple.domain.streak.BondDayRepository;
import com.smart.chat.couple.domain.streak.BondDay;
import com.smart.chat.couple.domain.points.PointLedgerRepository;
import com.smart.chat.couple.domain.points.PointEntry;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.smart.chat.couple.application.DomainRules.guard;
import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 连续互动打卡：确认打卡日、算连续天数与七档解锁、受理补签。
 * <p>
 * 打卡口径沿用统一语言里已有的那一条——<b>双方当天都发过贴贴才算一天</b>
 * （与心动值的 {@code bondDays} 同源），不另立「互动日」这个第二定义。
 * 连续天数与解锁档位一律读时算，唯一事实源是 {@code couple_bond_day} 的 day 集合。
 */
@Service
public class CoupleStreakService {

    /** 打卡条往回铺多少格（前端日历带的天数） */
    private static final int STRIP_DAYS = 21;

    // ========== VO ==========

    public record TierVO(String key, int days, String label, String icon, String detail, boolean unlocked,
                         String unlockedDay) {
    }

    public record StripCellVO(String day, boolean checked, boolean makeupFlag, boolean todayFlag) {
    }

    public record StreakBoardVO(String day, int currentStreak, int longestStreak, int confirmedDays,
                                boolean checkedToday, boolean missedYesterday, String lastCheckinDay,
                                List<TierVO> tiers, String nextTierKey, String nextTierLabel, int daysToNext,
                                List<StripCellVO> strip, int makeupCost, int makeupLeftThisMonth, int balance,
                                boolean canMakeup) {
    }

    private final CoupleSpaceRepository spaceRepository;
    private final BondDayRepository bondDayRepository;
    private final ActionRepository actionRepository;
    private final PointLedgerRepository ledgerRepository;
    private final CoupleEventPublisher push;

    public CoupleStreakService(CoupleSpaceRepository spaceRepository, BondDayRepository bondDayRepository,
                               ActionRepository actionRepository, PointLedgerRepository ledgerRepository,
                               CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.bondDayRepository = bondDayRepository;
        this.actionRepository = actionRepository;
        this.ledgerRepository = ledgerRepository;
        this.push = push;
    }

    // ========== 读 ==========

    public StreakBoardVO board(String me) {
        return boardOf(requireSpace(me), me);
    }

    /** 同一份看板的内部入口（百日回顾等复用，避免重复取空间）。 */
    public StreakBoardVO boardOf(CoupleSpace space, String me) {
        seedCreationDay(space);
        // 打卡表只扫一遍：连续状态与补签标记都从同一份行集合派生
        List<BondDay> rows = bondDayRepository.findBySpace(space.id());
        BondStreak streak = BondStreak.of(dayTexts(rows), LocalDate.now());
        LocalDate today = LocalDate.now();
        Set<String> days = streak.dayStrings();
        Set<String> makeupDays = new HashSet<>();
        for (BondDay row : rows) {
            if (row.makeup()) {
                makeupDays.add(row.day());
            }
        }

        List<TierVO> tiers = new ArrayList<>();
        for (StreakTier tier : StreakTier.values()) {
            boolean unlocked = streak.longestStreak() >= tier.days();
            LocalDate at = unlocked ? streak.unlockedAt(tier) : null;
            tiers.add(new TierVO(tier.key(), tier.days(), tier.label(), tier.icon(), tier.detail(), unlocked,
                    at == null ? null : at.toString()));
        }
        List<StripCellVO> strip = new ArrayList<>();
        for (int i = STRIP_DAYS - 1; i >= 0; i--) {
            String text = today.minusDays(i).toString();
            strip.add(new StripCellVO(text, days.contains(text), makeupDays.contains(text),
                    text.equals(today.toString())));
        }
        StreakTier next = streak.nextTier();
        int makeupLeft = MakeupPolicy.MONTHLY_QUOTA - makeupUsedThisMonth(space.id(), today);
        int balance = balance(space, me);
        return new StreakBoardVO(today.toString(), streak.currentStreak(), streak.longestStreak(),
                streak.confirmedDays(), streak.checkedToday(), streak.missedYesterday(),
                streak.lastCheckinDay() == null ? null : streak.lastCheckinDay().toString(), tiers,
                next == null ? null : next.key(), next == null ? null : next.label(), streak.daysToNextTier(),
                strip, MakeupPolicy.COST, makeupLeft, balance,
                streak.missedYesterday() && makeupLeft > 0 && balance >= MakeupPolicy.COST);
    }

    /** 已确认的打卡日集合算出的连续状态（其它服务只读复用）。 */
    public BondStreak streakOf(String spaceId) {
        return BondStreak.of(dayTexts(bondDayRepository.findBySpace(spaceId)), LocalDate.now());
    }

    // ========== 写 ==========

    /**
     * 贴贴之后试确认今天的打卡：只有两个人当天都发过动作才落一行 AUTO。
     * 新落成功才推 streak-checkin，跨过档位再补推 streak-unlocked。
     */
    public void markTodayAfterAction(CoupleSpace space, String actor) {
        String today = LocalDate.now().toString();
        if (bondDayRepository.find(space.id(), today).isPresent()) {
            return;
        }
        if (usersActiveSince(space.id(), startOfToday()).size() < 2) {
            return;
        }
        int[] span = appendDay(space, today, BondDay.SOURCE_AUTO, null);
        push.pushCoupleEventBoth("streak-checkin", actor, space.userA(), space.userB(),
                "今天也贴到了 🔥 连续 " + span[1] + " 天");
        pushCrossedTiers(space, actor, span[0], span[1]);
    }

    /** 补签：闸门与话术在 {@link MakeupPolicy}，这里只做取数、扣分、落行与推送。 */
    public StreakBoardVO makeup(String me, String day) {
        CoupleSpace space = requireSpace(me);
        LocalDate target = rule(() -> MakeupPolicy.parseDay(day));
        LocalDate today = LocalDate.now();
        String text = target.toString();
        guard(() -> MakeupPolicy.assertAllowed(target, today, bondDayRepository.find(space.id(), text).isPresent(),
                makeupUsedThisMonth(space.id(), today), balance(space, me)));

        ledgerRepository.append(PointEntry.spend(space.id(), me, "补签 " + text, MakeupPolicy.COST));
        int[] span = appendDay(space, text, BondDay.SOURCE_MAKEUP, me);
        push.pushCoupleEventBoth("streak-makeup", me, space.userA(), space.userB(),
                "补上了 " + text + " 的打卡 ✍️ 花了 " + MakeupPolicy.COST + " 分，现在连续 " + span[1] + " 天");
        pushCrossedTiers(space, me, span[0], span[1]);
        return boardOf(space, me);
    }

    // ========== 内部 ==========

    /**
     * 落一行打卡，返回 {@code {落行前的历史最长, 落行后的历史最长}}。
     * 只扫打卡表一次：after 是在 before 的集合上加上这一天算出来的，不必回查数据库。
     */
    private int[] appendDay(CoupleSpace space, String day, String source, String operator) {
        List<BondDay> before = bondDayRepository.findBySpace(space.id());
        LocalDate today = LocalDate.now();
        int prev = BondStreak.of(dayTexts(before), today).longestStreak();
        bondDayRepository.append(BondDay.confirm(space.id(), day, source, operator));
        List<String> after = dayTexts(before);
        after.add(day);
        int now = Math.max(prev, BondStreak.of(after, today).longestStreak());
        return new int[]{prev, now};
    }

    /** 只推「这次真的跨过了新档位」，断签后重新爬到同一个数字不会再庆祝一遍。 */
    private void pushCrossedTiers(CoupleSpace space, String actor, int prevLongest, int newLongest) {
        for (StreakTier tier : BondStreak.tiersCrossed(prevLongest, newLongest)) {
            push.pushCoupleEventBoth("streak-unlocked", actor, space.userA(), space.userB(),
                    "连续 " + tier.days() + " 天，解锁「" + tier.label() + "」" + tier.icon() + " " + tier.detail());
        }
    }

    /**
     * 在一起的第 1 天自动补一行：双方点头建立空间本身就是一次互动，
     * 不该因为「那天还没来得及贴贴」而让连续天数从 0 开始。
     */
    private void seedCreationDay(CoupleSpace space) {
        long at = space.created() <= 0 ? System.currentTimeMillis() : space.created();
        String createdDay = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate().toString();
        if (createdDay.compareTo(LocalDate.now().toString()) > 0) {
            return;
        }
        if (bondDayRepository.find(space.id(), createdDay).isEmpty()) {
            bondDayRepository.append(BondDay.confirm(space.id(), createdDay, BondDay.SOURCE_AUTO, null));
        }
    }

    private List<String> dayTexts(List<BondDay> rows) {
        List<String> days = new ArrayList<>();
        for (BondDay row : rows) {
            days.add(row.day());
        }
        return days;
    }

    private int makeupUsedThisMonth(String spaceId, LocalDate today) {
        YearMonth month = YearMonth.from(today);
        return (int) bondDayRepository.countMakeupBetween(spaceId, month.atDay(1).toString(),
                month.atEndOfMonth().toString());
    }

    /** 当天 00:00 起的贴贴流水里出现过哪些人（只取当天，不把全历史拉进内存）。 */
    private Set<String> usersActiveSince(String spaceId, long fromMs) {
        Set<String> users = new HashSet<>();
        for (BondAction action : actionRepository.listSentSince(spaceId, fromMs)) {
            if (action.username() != null) {
                users.add(action.username());
            }
        }
        return users;
    }

    private long startOfToday() {
        return LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** 本人积分余额 = 累计 EARN − 累计 SPEND（与愿望券本同一口径）。 */
    private int balance(CoupleSpace space, String me) {
        return ledgerRepository.findBySpace(space.id()).stream()
                .filter(l -> me.equals(l.fromUser()))
                .mapToInt(l -> l.earned() ? l.points() : -l.points())
                .sum();
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
