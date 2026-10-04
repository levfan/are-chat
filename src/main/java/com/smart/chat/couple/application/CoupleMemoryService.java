package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.memory.RelationSummary;
import com.smart.chat.couple.domain.streak.BondStreak;
import com.smart.chat.couple.domain.streak.StreakTier;
import com.smart.chat.couple.domain.wish.Wish;
import com.smart.chat.couple.infrastructure.persistence.CoupleBondDayMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleBondDayPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestionAnswerMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestionAnswerPO;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.infrastructure.persistence.CoupleWishMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleWishPO;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 隐藏彩蛋页（连续 100 天解锁）：把散落的事实拼成一条回顾时间轴 + 一句话总结。
 * <p>
 * 闸门在服务端也必须挡一次——前端把页签藏起来不等于接口没人能调。
 * 时间轴只收<b>有真实时间戳或可精确派生日期</b>的事件，凑不出日子的历史一律不上轴。
 */
@Service
public class CoupleMemoryService {

    /** 触发隐藏页所需的连续天数（与 StreakTier.EASTER_EGG 同一个数，别再写第二份） */
    private static final int REQUIRED_DAYS = StreakTier.EASTER_EGG.days();
    /** 时间轴最多带多少条 */
    private static final int TIMELINE_MAX = 80;

    // ========== VO ==========

    public record TimelineItemVO(String day, String kind, String title, String detail) {
    }

    public record MemoryVO(String summary, long daysTogether, int confirmedDays, int longestStreak,
                           int currentStreak, int makeupDays, int bothAnsweredDays, int fulfilledWishes,
                           String intimacyTitle, String unlockedDay, List<TimelineItemVO> timeline) {
    }

    private final CoupleSpaceRepository spaceRepository;
    private final CoupleBondDayMapper bondDayMapper;
    private final CoupleQuestionAnswerMapper answerMapper;
    private final CoupleWishMapper wishMapper;
    private final CoupleService coupleService;

    public CoupleMemoryService(CoupleSpaceRepository spaceRepository, CoupleBondDayMapper bondDayMapper,
                               CoupleQuestionAnswerMapper answerMapper, CoupleWishMapper wishMapper,
                               CoupleService coupleService) {
        this.spaceRepository = spaceRepository;
        this.bondDayMapper = bondDayMapper;
        this.answerMapper = answerMapper;
        this.wishMapper = wishMapper;
        this.coupleService = coupleService;
    }

    // ========== 读 ==========

    public MemoryVO memory(String me) {
        CoupleSpace space = requireSpace(me);
        BondStreak streak = streakOf(space.id());
        if (streak.longestStreak() < REQUIRED_DAYS) {
            throw new BusinessException(400, "这一页要连续贴满 " + REQUIRED_DAYS + " 天才打开，现在还差 "
                    + (REQUIRED_DAYS - streak.longestStreak()) + " 天");
        }

        List<CoupleBondDayPO> checkins = bondDayMapper.findBySpace(space.id());
        int makeupDays = 0;
        for (CoupleBondDayPO row : checkins) {
            if (row.makeupFlag()) {
                makeupDays++;
            }
        }
        List<CoupleQuestionAnswerPO> answers = answerMapper.findBySpace(space.id());
        int bothAnswered = bothAnsweredDays(answers);
        List<Wish> wishes = new ArrayList<>();
        int fulfilled = 0;
        for (CoupleWishPO row : wishMapper.findBySpace(space.id())) {
            Wish wish = Wish.restore(row.getId(), row.getOwnerUser(), row.getCreatorUser(), row.getTitle(),
                    row.getNote(), row.getStatus(), row.getPreparedBy(), row.getPreparedAt(), row.getFulfilledAt());
            wishes.add(wish);
            if (wish.fulfilledFlag()) {
                fulfilled++;
            }
        }

        CoupleService.IntimacyVO intimacy = coupleService.intimacy(me);
        List<TimelineItemVO> timeline = buildTimeline(space, answers, wishes, streak);
        RelationSummary.Facts facts = new RelationSummary.Facts(daysTogether(space), streak.confirmedDays(),
                streak.longestStreak(), makeupDays, bothAnswered, fulfilled, intimacy.title(),
                latestUnlockLabel(streak));
        LocalDate unlockedAt = streak.unlockedAt(StreakTier.EASTER_EGG);
        return new MemoryVO(RelationSummary.compose(facts), daysTogether(space), streak.confirmedDays(),
                streak.longestStreak(), streak.currentStreak(), makeupDays, bothAnswered, fulfilled,
                intimacy.title(), unlockedAt == null ? null : unlockedAt.toString(), timeline);
    }

    // ========== 内部 ==========

    private List<TimelineItemVO> buildTimeline(CoupleSpace space,
                                               List<CoupleQuestionAnswerPO> answers, List<Wish> wishes,
                                               BondStreak streak) {
        // 保留类事件（建立、解锁、答完的题、实现的愿望）一条都不能被截掉——
        // 截断如果按日期取前 80，100 天里最早的那几次解锁就正好消失在轴尾。
        List<TimelineItemVO> kept = new ArrayList<>();
        kept.add(new TimelineItemVO(dayOf(space.created() <= 0 ? System.currentTimeMillis()
                : space.created()).toString(), "space", "情侣空间开启 🎉", "这一天你们点头了"));

        for (StreakTier tier : StreakTier.values()) {
            LocalDate at = streak.unlockedAt(tier);
            if (at != null) {
                kept.add(new TimelineItemVO(at.toString(), "unlock",
                        "解锁「" + tier.label() + "」" + tier.icon(), "连续 " + tier.days() + " 天：" + tier.detail()));
            }
        }
        Set<String> seenQuestionDays = new HashSet<>();
        for (CoupleQuestionAnswerPO row : answers) {
            if (!seenQuestionDays.add(row.getDay())) {
                continue;
            }
            if (bothAnsweredOn(answers, row.getDay())) {
                kept.add(new TimelineItemVO(row.getDay(), "question", "答完一道每日一问 💬", row.getQuestion()));
            }
        }
        for (Wish wish : wishes) {
            if (wish.fulfilledFlag() && wish.fulfilledAt() != null) {
                kept.add(new TimelineItemVO(dayOf(wish.fulfilledAt()).toString(), "wish",
                        "愿望实现啦 🎁", wish.ownerUser() + " 想要「" + wish.title() + "」"));
            }
        }
        // 填充类事件（把峰值推高的那些日子）从新的往旧补，补齐到上限为止
        List<TimelineItemVO> fillers = new ArrayList<>();
        for (String day : longestRuns(streak)) {
            fillers.add(new TimelineItemVO(day, "streak", "连着贴满一段 🔥", "这一天的打卡把连续天数推到了新高"));
        }
        fillers.sort(Comparator.comparing(TimelineItemVO::day).reversed());

        List<TimelineItemVO> items = new ArrayList<>(kept);
        for (TimelineItemVO filler : fillers) {
            if (items.size() >= TIMELINE_MAX) {
                break;
            }
            items.add(filler);
        }
        items.sort(Comparator.comparing(TimelineItemVO::day).reversed());
        return items;
    }

    /** 每次把历史最长推高的那一天——这些是真正的「里程碑日」，其它打卡不上轴。 */
    private List<String> longestRuns(BondStreak streak) {
        List<String> days = new ArrayList<>();
        List<String> sorted = new ArrayList<>(streak.dayStrings());
        sorted.sort(String::compareTo);
        int best = 0;
        String previous = null;
        int run = 0;
        for (String day : sorted) {
            run = previous != null && LocalDate.parse(previous).plusDays(1).toString().equals(day) ? run + 1 : 1;
            previous = day;
            if (run > best) {
                best = run;
                days.add(day);
            }
        }
        return days;
    }

    private String latestUnlockLabel(BondStreak streak) {
        String label = null;
        for (StreakTier tier : StreakTier.values()) {
            if (streak.unlocked(tier.key())) {
                label = tier.label();
            }
        }
        return label;
    }

    private int bothAnsweredDays(List<CoupleQuestionAnswerPO> answers) {
        Set<String> days = new HashSet<>();
        for (CoupleQuestionAnswerPO row : answers) {
            if (row.getAnswer() != null && !row.getAnswer().isBlank() && bothAnsweredOn(answers, row.getDay())) {
                days.add(row.getDay());
            }
        }
        return days.size();
    }

    private boolean bothAnsweredOn(List<CoupleQuestionAnswerPO> answers, String day) {
        int count = 0;
        Set<String> who = new HashSet<>();
        for (CoupleQuestionAnswerPO row : answers) {
            if (row.getDay().equals(day) && row.getAnswer() != null && !row.getAnswer().isBlank()
                    && who.add(row.getUsername())) {
                count++;
            }
        }
        return count >= 2;
    }

    private BondStreak streakOf(String spaceId) {
        List<String> days = new ArrayList<>();
        for (CoupleBondDayPO row : bondDayMapper.findBySpace(spaceId)) {
            days.add(row.getDay());
        }
        return BondStreak.of(days, LocalDate.now());
    }

    private long daysTogether(CoupleSpace space) {
        LocalDate start;
        try {
            start = LocalDate.parse(space.anniversary());
        } catch (Exception e) {
            start = dayOf(space.created() <= 0 ? System.currentTimeMillis() : space.created());
        }
        return Math.max(java.time.temporal.ChronoUnit.DAYS.between(start, LocalDate.now()) + 1, 1);
    }

    private LocalDate dayOf(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
