package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 共同养成·习惯系（F70/F71/F72/F77）：双人挑战赛、恋爱存折、百日之约、星座配对。
 * 情绪价值设计：每天一件小事，让「坚持」变成两个人的游戏；
 * 星座配对把玄学变成情趣。
 */
@Service
public class CoupleGrowthService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleChallengeMapper challengeMapper;
    private final CouplePassbookMapper passbookMapper;
    private final CoupleHundredMapper hundredMapper;
    private final CoupleHundredCheckinMapper checkinMapper;
    private final ImPushService push;

    public CoupleGrowthService(CoupleSpaceMapper spaceMapper, CoupleChallengeMapper challengeMapper,
                               CouplePassbookMapper passbookMapper, CoupleHundredMapper hundredMapper,
                               CoupleHundredCheckinMapper checkinMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.challengeMapper = challengeMapper;
        this.passbookMapper = passbookMapper;
        this.hundredMapper = hundredMapper;
        this.checkinMapper = checkinMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record ChallengeVO(String day, String taskText, boolean doneMine, boolean donePartner, boolean bothDone) {
    }

    public record ChallengeBoardVO(ChallengeVO today, List<ChallengeVO> history, long wonCount) {
    }

    public record PassbookEntryVO(String id, String fromUser, String day, String content, boolean mine) {
    }

    public record PassbookBoardVO(PassbookEntryVO mineToday, PassbookEntryVO partnerToday,
                                  int myStreak, String milestone, List<PassbookEntryVO> recent) {
    }

    public record HundredVO(String id, String goal, String startDay, String status, long dayNumber,
                            long bothCheckedDays, boolean todayCheckedMine, boolean todayCheckedPartner) {
    }

    public record ZodiacVO(String mine, String mineLabel, String partner, String partnerLabel, int score, String comment) {
    }

    // ========== F70 双人挑战赛 ==========

    public ChallengeBoardVO challenge(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        List<CoupleChallenge> all = challengeMapper.findBySpace(space.getId());
        CoupleChallenge todayRow = all.stream().filter(c -> c.getDay().equals(today)).findFirst().orElse(null);
        if (todayRow == null) {
            todayRow = CoupleChallenge.of(space.getId(), today, CoupleGrowthBank.pickChallenge(space.getId(), today));
            challengeMapper.insert(todayRow);
        }
        ChallengeVO todayVO = toChallengeVO(todayRow, space, me);
        List<ChallengeVO> history = all.stream()
                .filter(c -> !c.getDay().equals(today))
                .limit(14)
                .map(c -> toChallengeVO(c, space, me))
                .toList();
        long won = all.stream().filter(CoupleChallenge::bothDone).count();
        return new ChallengeBoardVO(todayVO, history, won);
    }

    /** 打卡今日挑战；双方都完成时宣布胜利。 */
    public ChallengeBoardVO checkChallenge(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        CoupleChallenge row = challengeMapper.find(space.getId(), today);
        if (row == null) {
            row = CoupleChallenge.of(space.getId(), today, CoupleGrowthBank.pickChallenge(space.getId(), today));
            challengeMapper.insert(row);
        }
        boolean mine = space.getUserA().equals(me);
        if (mine && !row.isDoneA()) {
            row.setDoneA(true);
            row.setDoneAtA(System.currentTimeMillis());
        } else if (!mine && !row.isDoneB()) {
            row.setDoneB(true);
            row.setDoneAtB(System.currentTimeMillis());
        }
        challengeMapper.updateById(row);
        if (row.bothDone()) {
            push.pushCoupleEventBoth("challenge-done", me, space.getUserA(), space.getUserB(),
                    "🏆 今日双人挑战达成：「" + row.getTaskText() + "」——一起完成的感觉就是不一样");
        } else {
            push.pushCoupleEvent("challenge-checked", me, space.partnerOf(me),
                    "⚡ TA 已完成今日挑战：「" + row.getTaskText() + "」就差你啦！");
        }
        return challenge(me);
    }

    private ChallengeVO toChallengeVO(CoupleChallenge row, CoupleSpace space, String me) {
        boolean mineIsA = space.getUserA().equals(me);
        return new ChallengeVO(row.getDay(), row.getTaskText(),
                mineIsA ? row.isDoneA() : row.isDoneB(),
                mineIsA ? row.isDoneB() : row.isDoneA(),
                row.bothDone());
    }

    // ========== F71 恋爱存折 ==========

    public PassbookBoardVO passbook(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String today = LocalDate.now().toString();
        List<CouplePassbook> all = passbookMapper.findBySpace(space.getId());
        PassbookEntryVO mineToday = all.stream()
                .filter(p -> p.getFromUser().equals(me) && p.getDay().equals(today))
                .findFirst().map(p -> toPassbookVO(p, true)).orElse(null);
        PassbookEntryVO partnerToday = all.stream()
                .filter(p -> p.getFromUser().equals(partner) && p.getDay().equals(today))
                .findFirst().map(p -> toPassbookVO(p, false)).orElse(null);
        int streak = passbookStreak(all, me, today);
        return new PassbookBoardVO(mineToday, partnerToday, streak,
                CoupleGrowthBank.passbookMilestone(streak),
                all.stream().limit(20).map(p -> toPassbookVO(p, p.getFromUser().equals(me))).toList());
    }

    /** 存一笔「今天为这段感情做的小事」（每人每天一笔，可修改）。 */
    public PassbookBoardVO depositPassbook(String me, String content) {
        if (content == null || content.isBlank() || content.length() > CouplePassbook.CONTENT_MAX) {
            throw new BusinessException(400, "小事再小也要写下来（" + CouplePassbook.CONTENT_MAX + " 字内）");
        }
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        CouplePassbook existing = passbookMapper.find(space.getId(), me, today);
        if (existing == null) {
            passbookMapper.insert(CouplePassbook.of(space.getId(), me, content.trim()));
        } else {
            existing.setContent(content.trim());
            passbookMapper.updateById(existing);
        }
        push.pushCoupleEvent("passbook-deposit", me, space.partnerOf(me),
                "💰 TA 在恋爱存折里存了一笔：「" + content.trim() + "」");
        return passbook(me);
    }

    /** 我的连续存款天数：从今天（未存则从昨天）往回数连续有记录的天数。 */
    private int passbookStreak(List<CouplePassbook> all, String me, String today) {
        Map<String, Long> byDay = all.stream()
                .filter(p -> p.getFromUser().equals(me))
                .collect(Collectors.groupingBy(CouplePassbook::getDay, Collectors.counting()));
        LocalDate cursor = LocalDate.parse(today);
        if (!byDay.containsKey(cursor.toString())) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        while (byDay.containsKey(cursor.toString())) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private PassbookEntryVO toPassbookVO(CouplePassbook p, boolean mine) {
        return new PassbookEntryVO(p.getId(), p.getFromUser(), p.getDay(), p.getContent(), mine);
    }

    // ========== F72 百日之约 ==========

    public List<HundredVO> hundreds(String me) {
        CoupleSpace space = requireSpace(me);
        String today = LocalDate.now().toString();
        return hundredMapper.findBySpace(space.getId()).stream()
                .map(p -> {
                    List<CoupleHundredCheckin> checkins = checkinMapper.findByPact(p.getId());
                    Map<String, Long> byDay = checkins.stream()
                            .collect(Collectors.groupingBy(CoupleHundredCheckin::getDay, Collectors.counting()));
                    long bothDays = byDay.values().stream().filter(n -> n >= 2).count();
                    boolean mineToday = checkins.stream().anyMatch(c -> c.getByUser().equals(me) && c.getDay().equals(today));
                    boolean partnerToday = checkins.stream().anyMatch(c -> !c.getByUser().equals(me) && c.getDay().equals(today));
                    long dayNumber = Math.min(CoupleHundred.GOAL_DAYS,
                            ChronoUnit.DAYS.between(LocalDate.parse(p.getStartDay()), LocalDate.parse(today)) + 1);
                    return new HundredVO(p.getId(), p.getGoal(), p.getStartDay(), p.getStatus(),
                            Math.max(0, dayNumber), bothDays, mineToday, partnerToday);
                })
                .toList();
    }

    /** 发起百日之约：同一时间只能有一个进行中的约定。 */
    public List<HundredVO> createHundred(String me, String goal, String startDay) {
        if (goal == null || goal.isBlank() || goal.length() > CoupleHundred.GOAL_MAX) {
            throw new BusinessException(400, "百日目标要写清楚（" + CoupleHundred.GOAL_MAX + " 字内）");
        }
        CoupleSpace space = requireSpace(me);
        boolean activeExists = hundredMapper.findBySpace(space.getId()).stream()
                .anyMatch(p -> CoupleHundred.STATUS_ACTIVE.equals(p.getStatus()));
        if (activeExists) {
            throw new BusinessException(400, "已经有一个进行中的百日之约啦，达成或中止后再开新的");
        }
        String start = startDay == null || startDay.isBlank() ? LocalDate.now().toString() : startDay;
        hundredMapper.insert(CoupleHundred.of(space.getId(), goal.trim(), start));
        push.pushCoupleEvent("hundred-started", me, space.partnerOf(me),
                "🎯 TA 发起了百日之约：「" + goal.trim() + "」，从 " + start + " 开始，每天来打卡");
        return hundreds(me);
    }

    /** 百日之约每日打卡：双方打卡满 100 天自动达成。 */
    public List<HundredVO> checkinHundred(String me, String pactId, String note) {
        if (note != null && note.length() > CoupleHundredCheckin.NOTE_MAX) {
            throw new BusinessException(400, "心得最多 " + CoupleHundredCheckin.NOTE_MAX + " 字");
        }
        CoupleSpace space = requireSpace(me);
        CoupleHundred pact = hundredMapper.selectById(pactId);
        if (pact == null || !pact.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这个百日之约哦");
        }
        if (!CoupleHundred.STATUS_ACTIVE.equals(pact.getStatus())) {
            throw new BusinessException(400, "这个百日之约已经结束啦");
        }
        String today = LocalDate.now().toString();
        CoupleHundredCheckin existing = checkinMapper.find(pact.getId(), me, today);
        if (existing == null) {
            checkinMapper.insert(CoupleHundredCheckin.of(pact.getId(), space.getId(), me, today, note));
        } else if (note != null && !note.isBlank()) {
            existing.setNote(note.trim());
        }
        // 判定达成：双方都有打卡记录的天数 >= 100
        long bothDays = checkinMapper.findByPact(pact.getId()).stream()
                .collect(Collectors.groupingBy(CoupleHundredCheckin::getDay, Collectors.counting()))
                .values().stream().filter(n -> n >= 2).count();
        if (bothDays >= CoupleHundred.GOAL_DAYS) {
            pact.setStatus(CoupleHundred.STATUS_DONE);
            pact.setDoneAt(System.currentTimeMillis());
            hundredMapper.updateById(pact);
            push.pushCoupleEventBoth("hundred-done", me, space.getUserA(), space.getUserB(),
                    "🎉 百日之约达成：「" + pact.getGoal() + "」坚持了 " + CoupleHundred.GOAL_DAYS + " 天！你们真的做到了");
        } else {
            push.pushCoupleEvent("hundred-checkin", me, space.partnerOf(me),
                    "📅 TA 完成了今天百日之约的打卡（第 " + bothDays + " 天）：" + pact.getGoal());
        }
        return hundreds(me);
    }

    /** 中止百日之约（双方均可，给坚持不下去的约定一个体面的出口）。 */
    public List<HundredVO> breakHundred(String me, String pactId) {
        CoupleSpace space = requireSpace(me);
        CoupleHundred pact = hundredMapper.selectById(pactId);
        if (pact == null || !pact.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这个百日之约哦");
        }
        if (CoupleHundred.STATUS_ACTIVE.equals(pact.getStatus())) {
            pact.setStatus(CoupleHundred.STATUS_BROKEN);
            hundredMapper.updateById(pact);
            push.pushCoupleEventBoth("hundred-broken", me, space.getUserA(), space.getUserB(),
                    "🕊️ 百日之约「" + pact.getGoal() + "」已中止——没关系，想坚持的事随时可以重新开始");
        }
        return hundreds(me);
    }

    // ========== F77 星座配对（静态） ==========

    public ZodiacVO zodiac(String mine, String partner) {
        if (mine == null || partner == null || !CoupleGrowthBank.zodiacKeys().contains(mine)
                || !CoupleGrowthBank.zodiacKeys().contains(partner)) {
            throw new BusinessException(400, "选好两个星座再测哦");
        }
        return CoupleGrowthBank.zodiacPair(mine, partner);
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
