package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.streak.MakeupPolicy;
import com.smart.chat.couple.domain.streak.StreakDay;
import com.smart.chat.couple.domain.streak.StreakDayRepository;
import com.smart.chat.couple.domain.streak.StreakDays;
import com.smart.chat.couple.domain.streak.StreakTier;
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
 * 打卡口径只有一条——<b>双方当天都答完每日一问才算一天</b>。2026-10-05 二轮裁剪把贴贴卡删掉后，
 * 这一条不再挂在贴贴上（{@code CoupleQuestionService.answer} 在双方都答完时调用本服务），
 * 空间建立当天自动算第 1 天。连续天数与解锁档位一律读时算，
 * 唯一事实源是 {@code couple_streak_day} 的 day 集合。
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
                                List<StripCellVO> strip, int makeupWindowDays, int makeupLeftThisMonth,
                                boolean canMakeup) {
    }

    private final CoupleSpaceRepository spaceRepository;
    private final StreakDayRepository streakDayRepository;
    private final CoupleEventPublisher push;

    public CoupleStreakService(CoupleSpaceRepository spaceRepository, StreakDayRepository streakDayRepository,
                               CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.streakDayRepository = streakDayRepository;
        this.push = push;
    }

    // ========== 读 ==========

    public StreakBoardVO board(String me) {
        return boardOf(requireSpace(me));
    }

    /** 同一份看板的内部入口（百日回顾等复用，避免重复取空间）。 */
    public StreakBoardVO boardOf(CoupleSpace space) {
        seedCreationDay(space);
        // 打卡表只扫一遍：连续状态与补签标记都从同一份行集合派生
        List<StreakDay> rows = streakDayRepository.findBySpace(space.id());
        LocalDate today = LocalDate.now();
        StreakDays streak = StreakDays.of(dayTexts(rows), today);
        Set<String> days = streak.dayStrings();
        Set<String> makeupDays = new HashSet<>();
        for (StreakDay row : rows) {
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
        return new StreakBoardVO(today.toString(), streak.currentStreak(), streak.longestStreak(),
                streak.confirmedDays(), streak.checkedToday(), streak.missedYesterday(),
                streak.lastCheckinDay() == null ? null : streak.lastCheckinDay().toString(), tiers,
                next == null ? null : next.key(), next == null ? null : next.label(), streak.daysToNextTier(),
                strip, MakeupPolicy.WINDOW_DAYS, makeupLeft, streak.missedYesterday() && makeupLeft > 0);
    }

    /** 已确认的打卡日集合算出的连续状态（其它服务只读复用）。 */
    public StreakDays streakOf(String spaceId) {
        return StreakDays.of(dayTexts(streakDayRepository.findBySpace(spaceId)), LocalDate.now());
    }

    // ========== 写 ==========

    /**
     * 每日一问双方都答完之后确认今天的打卡：一天只落一行，重复调用不再生效。
     * 新落成功才推 streak-checkin，跨过档位再补推 streak-unlocked。
     */
    public void confirmBothAnswered(CoupleSpace space, String actor) {
        String today = LocalDate.now().toString();
        if (streakDayRepository.find(space.id(), today).isPresent()) {
            return;
        }
        int[] span = appendDay(space, today, StreakDay.SOURCE_AUTO, null);
        push.pushCoupleEventBoth("streak-checkin", actor, space.userA(), space.userB(),
                "今天的问答两个人都答完啦 🔥 连续 " + span[1] + " 天");
        pushCrossedTiers(space, actor, span[0], span[1]);
    }

    /** 空间建立当天自动算第 1 天：双方点头本身就是一次互动。 */
    public void confirmCreationDay(CoupleSpace space) {
        seedCreationDay(space);
    }

    /** 补签：闸门与话术在 {@link MakeupPolicy}，这里只做取数、落行与推送。 */
    public StreakBoardVO makeup(String me, String day) {
        CoupleSpace space = requireSpace(me);
        LocalDate target = rule(() -> MakeupPolicy.parseDay(day));
        LocalDate today = LocalDate.now();
        String text = target.toString();
        guard(() -> MakeupPolicy.assertAllowed(target, today, streakDayRepository.find(space.id(), text).isPresent(),
                makeupUsedThisMonth(space.id(), today)));

        int[] span = appendDay(space, text, StreakDay.SOURCE_MAKEUP, me);
        int left = MakeupPolicy.MONTHLY_QUOTA - makeupUsedThisMonth(space.id(), today);
        push.pushCoupleEventBoth("streak-makeup", me, space.userA(), space.userB(),
                "补上了 " + text + " 的打卡 ✍️ 本月还能补 " + left + " 次，现在连续 " + span[1] + " 天");
        pushCrossedTiers(space, me, span[0], span[1]);
        return boardOf(space);
    }

    // ========== 内部 ==========

    /**
     * 落一行打卡，返回 {@code {落行前的历史最长, 落行后的历史最长}}。
     * 只扫打卡表一次：after 是在 before 的集合上加上这一天算出来的，不必回查数据库。
     */
    private int[] appendDay(CoupleSpace space, String day, String source, String operator) {
        List<StreakDay> before = streakDayRepository.findBySpace(space.id());
        LocalDate today = LocalDate.now();
        int prev = StreakDays.of(dayTexts(before), today).longestStreak();
        streakDayRepository.append(StreakDay.confirm(space.id(), day, source, operator));
        List<String> after = dayTexts(before);
        after.add(day);
        int now = Math.max(prev, StreakDays.of(after, today).longestStreak());
        return new int[]{prev, now};
    }

    /** 只推「这次真的跨过了新档位」，断签后重新爬到同一个数字不会再庆祝一遍。 */
    private void pushCrossedTiers(CoupleSpace space, String actor, int prevLongest, int newLongest) {
        for (StreakTier tier : StreakDays.tiersCrossed(prevLongest, newLongest)) {
            push.pushCoupleEventBoth("streak-unlocked", actor, space.userA(), space.userB(),
                    "连续 " + tier.days() + " 天，解锁「" + tier.label() + "」" + tier.icon() + " " + tier.detail());
        }
    }

    /** 建立当天补一行（幂等：那一天已有行就什么都不做）。 */
    private void seedCreationDay(CoupleSpace space) {
        long at = space.created() <= 0 ? System.currentTimeMillis() : space.created();
        String createdDay = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate().toString();
        if (createdDay.compareTo(LocalDate.now().toString()) > 0) {
            return;
        }
        if (streakDayRepository.find(space.id(), createdDay).isEmpty()) {
            streakDayRepository.append(StreakDay.confirm(space.id(), createdDay, StreakDay.SOURCE_AUTO, null));
        }
    }

    private List<String> dayTexts(List<StreakDay> rows) {
        List<String> days = new ArrayList<>();
        for (StreakDay row : rows) {
            days.add(row.day());
        }
        return days;
    }

    private int makeupUsedThisMonth(String spaceId, LocalDate today) {
        YearMonth month = YearMonth.from(today);
        return (int) streakDayRepository.countMakeupBetween(spaceId, month.atDay(1).toString(),
                month.atEndOfMonth().toString());
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
