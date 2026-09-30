package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 体验优化（F95/F96）：今日看点聚合卡 + 年度互动热力日历。
 * 情绪价值设计：今天该做的甜蜜小事一眼看清，一整年的用心画成一片星空。
 */
@Service
public class CoupleTodayService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleChallengeMapper challengeMapper;
    private final CoupleTruthMapper truthMapper;
    private final CoupleMoodMapper moodMapper;
    private final CouplePassbookMapper passbookMapper;
    private final CoupleHundredMapper hundredMapper;
    private final CoupleHundredCheckinMapper checkinMapper;
    private final CoupleCapsuleMapper capsuleMapper;

    public CoupleTodayService(CoupleSpaceMapper spaceMapper, CoupleChallengeMapper challengeMapper,
                              CoupleTruthMapper truthMapper, CoupleMoodMapper moodMapper,
                              CouplePassbookMapper passbookMapper, CoupleHundredMapper hundredMapper,
                              CoupleHundredCheckinMapper checkinMapper, CoupleCapsuleMapper capsuleMapper) {
        this.spaceMapper = spaceMapper;
        this.challengeMapper = challengeMapper;
        this.truthMapper = truthMapper;
        this.moodMapper = moodMapper;
        this.passbookMapper = passbookMapper;
        this.hundredMapper = hundredMapper;
        this.checkinMapper = checkinMapper;
        this.capsuleMapper = capsuleMapper;
    }

    // ========== VO ==========

    /** 今日看点：今天值得做的甜蜜小事，做完一项亮一项。 */
    public record TodayBoardVO(String day, boolean challengeDone, boolean truthAnswered, boolean moodLogged,
                               boolean passbookDeposited, boolean hundredChecked, Long pactDayNumber,
                               String nextCapsuleDay, Long capsuleDaysLeft) {
    }

    /** 热力日历单格：count 当日互动次数，level 0-3。 */
    public record HeatmapDayVO(String day, int count, int level) {
    }

    public record HeatmapVO(int year, List<HeatmapDayVO> days, int totalActive) {
    }

    // ========== F95 今日看点 ==========

    public TodayBoardVO today(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        CoupleChallenge todayChallenge = challengeMapper.findBySpace(space.getId()).stream()
                .filter(c -> today.equals(c.getDay())).findFirst().orElse(null);
        boolean challengeDone = todayChallenge != null
                && ((space.getUserA().equals(me) && todayChallenge.isDoneA())
                || (space.getUserB().equals(me) && todayChallenge.isDoneB()));
        boolean truthAnswered = truthMapper.findBySpace(space.getId()).stream()
                .anyMatch(t -> today.equals(t.getDay()) && me.equals(t.getAnswerer()));
        boolean moodLogged = moodMapper.findBySpace(space.getId()).stream()
                .anyMatch(m -> today.equals(m.getMoodDay()) && me.equals(m.getUsername()));
        boolean passbookDeposited = passbookMapper.find(space.getId(), me, today) != null;

        boolean hundredChecked = false;
        Long pactDayNumber = null;
        CoupleHundred active = hundredMapper.findBySpace(space.getId()).stream()
                .filter(h -> CoupleHundred.STATUS_ACTIVE.equals(h.getStatus()))
                .findFirst().orElse(null);
        if (active != null) {
            hundredChecked = checkinMapper.find(active.getId(), me, today) != null;
            pactDayNumber = (long) Math.floorDiv(checkinMapper.findByPact(active.getId()).size(), 2) + 1;
        }

        String nextCapsuleDay = null;
        Long capsuleDaysLeft = null;
        LocalDate todayDate = LocalDate.now();
        for (CoupleCapsule capsule : capsuleMapper.findBySpace(space.getId())) {
            if (!CoupleCapsule.STATUS_SEALED.equals(capsule.getStatus())) {
                continue;
            }
            try {
                LocalDate openDate = LocalDate.parse(capsule.getOpenDay());
                if (!openDate.isBefore(todayDate)
                        && (capsuleDaysLeft == null || openDate.isBefore(LocalDate.parse(nextCapsuleDay)))) {
                    nextCapsuleDay = capsule.getOpenDay();
                    capsuleDaysLeft = ChronoUnit.DAYS.between(todayDate, openDate);
                }
            } catch (Exception ignored) {
                // 非法日期不参与聚合
            }
        }
        return new TodayBoardVO(today, challengeDone, truthAnswered, moodLogged, passbookDeposited,
                hundredChecked, pactDayNumber, nextCapsuleDay, capsuleDaysLeft);
    }

    // ========== F96 年度热力日历 ==========

    /** 指定年份的互动热力：心情/存折/挑战/百日打卡/真心话，每天计数分级。 */
    public HeatmapVO heatmap(String me, Integer year) {
        CoupleSpace space = requireSpace(me);
        int targetYear = year == null ? LocalDate.now().getYear() : year;
        Map<String, Integer> counter = new HashMap<>();
        for (CoupleMood m : moodMapper.findBySpace(space.getId())) {
            bump(counter, m.getMoodDay(), targetYear);
        }
        for (CouplePassbook p : passbookMapper.findBySpace(space.getId())) {
            bump(counter, p.getDay(), targetYear);
        }
        for (CoupleChallenge c : challengeMapper.findBySpace(space.getId())) {
            bump(counter, c.getDay(), targetYear);
        }
        for (CoupleTruth t : truthMapper.findBySpace(space.getId())) {
            bump(counter, t.getDay(), targetYear);
        }
        for (CoupleHundred pact : hundredMapper.findBySpace(space.getId())) {
            for (CoupleHundredCheckin checkin : checkinMapper.findByPact(pact.getId())) {
                bump(counter, checkin.getDay(), targetYear);
            }
        }
        List<HeatmapDayVO> days = new ArrayList<>();
        LocalDate cursor = LocalDate.of(targetYear, 1, 1);
        LocalDate end = LocalDate.of(targetYear, 12, 31);
        while (!cursor.isAfter(end)) {
            String day = cursor.toString();
            int count = counter.getOrDefault(day, 0);
            days.add(new HeatmapDayVO(day, count, levelOf(count)));
            cursor = cursor.plusDays(1);
        }
        int totalActive = (int) counter.keySet().stream().filter(d -> d.startsWith(String.valueOf(targetYear))).count();
        return new HeatmapVO(targetYear, days, totalActive);
    }

    private void bump(Map<String, Integer> counter, String day, int targetYear) {
        if (day == null || !day.startsWith(String.valueOf(targetYear))) {
            return;
        }
        counter.merge(day, 1, Integer::sum);
    }

    private int levelOf(int count) {
        if (count <= 0) {
            return 0;
        }
        if (count < 3) {
            return 1;
        }
        return count < 6 ? 2 : 3;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
