package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 被接住系列（F60/F63/F64/F65）：求抱抱、陪聊话题卡、情绪同步率、深夜陪伴。
 * 情绪价值设计：让「我需要安慰」可以被大声说出来、被立刻接住；
 * 让低落的夜晚永远有一句晚安兜底。
 */
@Service
public class CoupleComfortService {

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleComfortMapper comfortMapper;
    private final CoupleMoodMapper moodMapper;
    private final ImPushService push;

    public CoupleComfortService(CoupleSpaceMapper spaceMapper, CoupleComfortMapper comfortMapper,
                                CoupleMoodMapper moodMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.comfortMapper = comfortMapper;
        this.moodMapper = moodMapper;
        this.push = push;
    }

    // ========== VO ==========

    /** 求抱抱条目：fromUser 对看的人而言是「谁在求抱抱」。 */
    public record ComfortVO(String id, String fromUser, String day, String feeling, String feelingLabel,
                            String feelingEmoji, boolean handled, String handledNote, Long handledAt) {
    }

    public record ComfortBoardVO(ComfortVO mine, ComfortVO partnerPending, List<ComfortVO> history) {
    }

    /** 情绪同步率：双方都记录过心情的日子里，心情一致的比例与连续同步天数。 */
    public record MoodSyncVO(long bothDays, long syncedDays, int syncRate, boolean todaySync,
                             String todayMoodMine, String todayMoodPartner, int streak) {
    }

    // ========== F60 求抱抱 ==========

    public ComfortBoardVO comfortBoard(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String today = LocalDate.now().toString();
        List<ComfortVO> history = comfortMapper.findBySpace(space.getId()).stream()
                .map(c -> toVO(c))
                .toList();
        ComfortVO mine = history.stream()
                .filter(c -> c.fromUser().equals(me) && c.day().equals(today))
                .findFirst().orElse(null);
        ComfortVO pending = history.stream()
                .filter(c -> c.fromUser().equals(partner) && !c.handled() && c.day().equals(today))
                .findFirst().orElse(null);
        return new ComfortBoardVO(mine, pending, history.stream().limit(20).toList());
    }

    /** 求抱抱：一键告诉 TA「我现在很难过/委屈/累…」（每人每天一条，重复提交视为更新感受）。 */
    public ComfortBoardVO askForComfort(String me, String feeling) {
        if (!CoupleComfort.FEELINGS.contains(feeling)) {
            throw new BusinessException(400, "感受只能是 难过/委屈/累/焦虑/emo 哦");
        }
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String today = LocalDate.now().toString();
        CoupleComfort existing = comfortMapper.find(space.getId(), me, today);
        if (existing == null) {
            comfortMapper.insert(CoupleComfort.of(space.getId(), me, feeling));
        } else {
            existing.setFeeling(feeling);
            existing.setHandled(false);
            existing.setHandledNote(null);
            existing.setHandledAt(null);
            comfortMapper.updateById(existing);
        }
        push.pushCoupleEvent("comfort-sent", me, partner,
                "🫂 TA 说 TA 现在有点" + CoupleComfort.feelingLabel(feeling) + " "
                        + CoupleComfort.feelingEmoji(feeling) + "，需要一个抱抱——去 TA 那里给 TA 一点温柔吧");
        return comfortBoard(me);
    }

    /** TA 的安慰话术卡：按感受随机出 3 张，选一张送出去（也可以自己手写）。 */
    public List<String> comfortCards(String feeling) {
        if (!CoupleComfort.FEELINGS.contains(feeling)) {
            throw new BusinessException(400, "不认识这种感受哦");
        }
        return CoupleTalkBank.comfortWords(feeling, 3);
    }

    /** 回应 TA 的求抱抱：只有对方能回应，回应后求抱抱的人会收到抱抱与那句话。 */
    public ComfortVO handleComfort(String me, String note) {
        if (note == null || note.isBlank() || note.length() > CoupleComfort.NOTE_MAX) {
            throw new BusinessException(400, "写一句 " + CoupleComfort.NOTE_MAX + " 字以内的话，把抱抱送过去");
        }
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String today = LocalDate.now().toString();
        CoupleComfort pending = comfortMapper.find(space.getId(), partner, today);
        if (pending == null) {
            throw new BusinessException(404, "TA 今天还没有发过求抱抱哦");
        }
        if (pending.isHandled()) {
            return toVO(pending);
        }
        pending.setHandled(true);
        pending.setHandledNote(note.trim());
        pending.setHandledAt(System.currentTimeMillis());
        comfortMapper.updateById(pending);
        push.pushCoupleEvent("comfort-given", me, partner,
                "🤗 TA 给了你一个抱抱，还对你说：「" + note.trim() + "」");
        return toVO(pending);
    }

    // ========== F63 陪聊话题卡 ==========

    /** 低落时不知道聊什么？抽 3 张话题卡。 */
    public List<String> chatTopics(String me) {
        requireSpace(me);
        return CoupleTalkBank.chatTopics(3);
    }

    // ========== F64 情绪同步率 ==========

    /** 双方心情的同频程度：一致天数占比 + 当前是否同步 + 连续同步天数。 */
    public MoodSyncVO moodSync(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        Map<String, String> mine = moodMap(space.getId(), me);
        Map<String, String> theirs = moodMap(space.getId(), partner);
        long bothDays = 0;
        long syncedDays = 0;
        for (Map.Entry<String, String> entry : mine.entrySet()) {
            String other = theirs.get(entry.getKey());
            if (other == null) {
                continue;
            }
            bothDays++;
            if (other.equals(entry.getValue())) {
                syncedDays++;
            }
        }
        String today = LocalDate.now().toString();
        String myToday = mine.get(today);
        String partnerToday = theirs.get(today);
        boolean todaySync = myToday != null && myToday.equals(partnerToday);
        int streak = 0;
        LocalDate cursor = LocalDate.now();
        while (true) {
            String day = cursor.toString();
            String a = mine.get(day);
            String b = theirs.get(day);
            if (a != null && a.equals(b)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else {
                break;
            }
        }
        int rate = bothDays == 0 ? 0 : (int) Math.round(syncedDays * 100.0 / bothDays);
        return new MoodSyncVO(bothDays, syncedDays, rate, todaySync, myToday, partnerToday, streak);
    }

    private Map<String, String> moodMap(String spaceId, String username) {
        Map<String, String> map = new HashMap<>();
        for (CoupleMood mood : moodMapper.findBySpace(spaceId)) {
            if (mood.getUsername().equals(username)) {
                map.put(mood.getMoodDay(), mood.getMood());
            }
        }
        return map;
    }

    // ========== F65 深夜陪伴（定时任务调用） ==========

    /** 每天 23:00：谁今天心情低落还没被安慰，就提醒对方去陪陪 TA。 */
    public void remindNightCare() {
        String today = LocalDate.now().toString();
        List<String> negatives = List.of("SAD", "ANGRY", "SICK", "TIRED");
        for (CoupleSpace space : spaceMapper.findAllActive()) {
            for (String user : List.of(space.getUserA(), space.getUserB())) {
                CoupleMood mood = moodMapper.find(space.getId(), user, today).orElse(null);
                if (mood == null || !negatives.contains(mood.getMood())) {
                    continue;
                }
                CoupleComfort comfort = comfortMapper.find(space.getId(), user, today);
                if (comfort != null && comfort.isHandled()) {
                    continue; // 已经被接住，不打扰
                }
                push.pushCoupleEvent("night-care", "system", space.partnerOf(user),
                        CoupleTalkBank.nightCareLine());
            }
        }
    }

    // ========== 内部工具 ==========

    private ComfortVO toVO(CoupleComfort c) {
        return new ComfortVO(c.getId(), c.getFromUser(), c.getDay(), c.getFeeling(),
                CoupleComfort.feelingLabel(c.getFeeling()), CoupleComfort.feelingEmoji(c.getFeeling()),
                c.isHandled(), c.getHandledNote(), c.getHandledAt());
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
