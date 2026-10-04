package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.comfort.ComfortRepository;
import com.smart.chat.couple.domain.comfort.ComfortRequest;
import com.smart.chat.couple.domain.mood.Mood;
import com.smart.chat.couple.domain.mood.MoodRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.infrastructure.content.CoupleTalkBank;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 被接住系列（F60/F63/F64/F65）：求抱抱、陪聊话题卡、情绪同步率、深夜陪伴。
 * 情绪价值设计：让「我需要安慰」可以被大声说出来、被立刻接住；
 * 让低落的夜晚永远有一句晚安兜底。
 */
import static com.smart.chat.couple.application.DomainRules.guard;
import static com.smart.chat.couple.application.DomainRules.rule;

@Service
public class CoupleComfortService {

    private final CoupleSpaceRepository spaceRepository;
    private final ComfortRepository comfortRepository;
    private final MoodRepository moodRepository;
    private final CoupleEventPublisher push;

    public CoupleComfortService(CoupleSpaceRepository spaceRepository, ComfortRepository comfortRepository,
                                MoodRepository moodRepository, CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.comfortRepository = comfortRepository;
        this.moodRepository = moodRepository;
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
        List<ComfortRequest> rows = comfortRepository.listBySpace(space.id());
        ComfortVO mine = rows.stream().filter(c -> c.askedBy(me) && c.raisedOn(today))
                .findFirst().map(this::toVO).orElse(null);
        ComfortVO pending = rows.stream().filter(c -> c.askedBy(partner) && c.raisedOn(today) && !c.handled())
                .findFirst().map(this::toVO).orElse(null);
        return new ComfortBoardVO(mine, pending, rows.stream().limit(20).map(this::toVO).toList());
    }

    /** 求抱抱：一键告诉 TA「我现在很难过/委屈/累…」（每人每天一条，重复提交视为更新感受）。 */
    public ComfortBoardVO askForComfort(String me, String feeling) {
        // 感受闸门先于取空间：未知感受时即便没有空间也是 400 不是 404（改造前后同序）
        guard(() -> ComfortRequest.requireFeeling(feeling));
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String today = LocalDate.now().toString();
        ComfortRequest request = comfortRepository.findBySpaceAndUserOn(space.id(), me, today)
                .orElseGet(() -> ComfortRequest.askOn(space.id(), me, today, feeling));
        guard(() -> request.reask(feeling));
        comfortRepository.save(request);
        push.pushCoupleEvent("comfort-sent", me, partner,
                "🫂 TA 说 TA 现在有点" + request.feelingLabel() + " "
                        + request.feelingEmoji() + "，需要一个抱抱——去 TA 那里给 TA 一点温柔吧");
        return comfortBoard(me);
    }

    /** TA 的安慰话术卡：按感受随机出 3 张，选一张送出去（也可以自己手写）。 */
    public List<String> comfortCards(String feeling) {
        guard(() -> ComfortRequest.requireKnown(feeling));
        return CoupleTalkBank.comfortWords(feeling, 3);
    }

    /** 回应 TA 的求抱抱：只有对方能回应，回应后求抱抱的人会收到抱抱与那句话。 */
    public ComfortVO handleComfort(String me, String note) {
        String words = rule(() -> ComfortRequest.requireNote(note));
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String today = LocalDate.now().toString();
        // 归属闸门：只取对方那一行——自己求的抱抱在这里根本取不到，也就接不了
        ComfortRequest pending = comfortRepository.findBySpaceAndUserOn(space.id(), partner, today)
                .orElseThrow(() -> new BusinessException(404, "TA 今天还没有发过求抱抱哦"));
        if (pending.handled()) {
            return toVO(pending);
        }
        guard(() -> pending.hold(words, System.currentTimeMillis()));
        comfortRepository.save(pending);
        push.pushCoupleEvent("comfort-given", me, partner,
                "🤗 TA 给了你一个抱抱，还对你说：「" + pending.note() + "」");
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
        Map<String, String> mine = moodMap(space.id(), me);
        Map<String, String> theirs = moodMap(space.id(), partner);
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
        for (Mood mood : moodRepository.listBySpace(spaceId)) {
            if (mood.recordedBy(username)) {
                map.put(mood.moodDay(), mood.mood());
            }
        }
        return map;
    }

    // ========== F65 深夜陪伴（定时任务调用） ==========

    /** 每天 23:00：谁今天心情低落还没被安慰，就提醒对方去陪陪 TA。 */
    public void remindNightCare() {
        String today = LocalDate.now().toString();
        for (CoupleSpace space : spaceRepository.findAllActive()) {
            for (String user : List.of(space.userA(), space.userB())) {
                Optional<Mood> downcast = moodRepository.findBySpaceAndUserOn(space.id(), user, today)
                        .filter(Mood::isDowncast);
                if (downcast.isEmpty()) {
                    continue; // 那天没记心情，或者记的不是低落
                }
                boolean alreadyHeld = comfortRepository.findBySpaceAndUserOn(space.id(), user, today)
                        .filter(ComfortRequest::handled).isPresent();
                if (alreadyHeld) {
                    continue; // 已经被接住，不打扰
                }
                push.pushCoupleEvent("night-care", "system", space.partnerOf(user),
                        CoupleTalkBank.nightCareLine());
            }
        }
    }

    // ========== 内部工具 ==========

    private ComfortVO toVO(ComfortRequest c) {
        return new ComfortVO(c.id(), c.by(), c.day(), c.feeling(), c.feelingLabel(), c.feelingEmoji(),
                c.handled(), c.note(), c.heldAt());
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
