package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.bond.ActionRepository;
import com.smart.chat.couple.domain.bond.BondAction;
import com.smart.chat.couple.domain.mood.Mood;
import com.smart.chat.couple.domain.mood.MoodReaction;
import com.smart.chat.couple.domain.mood.MoodReactionRepository;
import com.smart.chat.couple.domain.mood.MoodRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.smart.chat.couple.application.DomainRules.guard;
import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 贴贴互动：一键发送亲密小动作（戳一戳/抱抱/亲亲/捏捏脸/蹭蹭/挠痒痒/在想你）、
 * 心情回应（给 TA 的心情贴抱抱/亲亲/加油/摸摸头）与专属爱称。
 * 情绪价值设计：动作必达（对方实时收到推送）、贴贴里程碑庆祝、心情不好时一键传递温度。
 */
@Service
public class CoupleBondService {

    // ========== VO ==========

    public record ActionVO(String id, String username, String kind, Long created) {
    }

    /** 单类动作统计：累计 / 我发的 / TA 发的 / 最近一次时间。 */
    public record KindStat(String kind, String emoji, String label, long total, long mine, long partner,
                           Long lastAt) {
    }

    public record BondStatsVO(List<KindStat> kinds, long todayCount, long todayMine, long todayPartner) {
    }

    /** 某天双方对彼此心情的回应（谁还没回应就是 null）。 */
    public record MoodReactionVO(String day, String myReaction, String partnerReaction) {
    }

    private final CoupleSpaceRepository spaceRepository;
    private final ActionRepository actionRepository;
    private final MoodReactionRepository moodReactionRepository;
    private final MoodRepository moodRepository;
    private final CoupleStreakService streakService;
    private final CoupleEventPublisher push;

    public CoupleBondService(CoupleSpaceRepository spaceRepository, ActionRepository actionRepository,
                             MoodReactionRepository moodReactionRepository, MoodRepository moodRepository,
                             CoupleStreakService streakService, CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.actionRepository = actionRepository;
        this.moodReactionRepository = moodReactionRepository;
        this.moodRepository = moodRepository;
        this.streakService = streakService;
        this.push = push;
    }

    // ========== 亲密小动作 ==========

    /** 发送一个贴贴动作：对方实时收到推送；命中里程碑时双方一起庆祝。 */
    public BondStatsVO sendAction(String me, String kind) {
        // 动作目录的闸门先于取空间：不认识的动作即便没有空间也是 400 不是 404（改造前后同序）
        guard(() -> BondAction.requireKind(kind));
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        BondAction action = BondAction.sent(space.id(), me, kind);
        actionRepository.append(action);
        push.pushCoupleEvent("bond-action", me, partner, action.pushText());
        // 贴贴即打卡：双方当天都发过动作才落一天（口径与心动值的 bondDays 同源）
        streakService.markTodayAfterAction(space, me);
        // 贴贴里程碑：抱抱/亲亲/想念累计到整数关口，双方一起庆祝
        if (BondAction.celebrates(kind)) {
            Long milestone = BondAction.milestoneAt(actionRepository.countByKind(space.id(), kind));
            if (milestone != null) {
                push.pushCoupleEventBoth("bond-milestone", me, me, partner,
                        BondAction.milestoneDetail(kind, milestone));
            }
        }
        return stats(me);
    }

    /** 最近动作流（新→旧，默认 50 条上限）。 */
    public List<ActionVO> recentActions(String me, Integer limit) {
        CoupleSpace space = requireSpace(me);
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        return actionRepository.listBySpace(space.id()).stream()
                .limit(size)
                .map(a -> new ActionVO(a.id(), a.username(), a.kind(), a.created()))
                .toList();
    }

    /** 贴贴统计：各类动作累计/双方占比/最近时间 + 今日双方动作数。 */
    public BondStatsVO stats(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        List<KindStat> kinds = new ArrayList<>();
        for (String kind : BondAction.displayedKinds()) {
            kinds.add(new KindStat(kind, BondAction.emojiOf(kind), BondAction.labelOf(kind),
                    actionRepository.countByKind(space.id(), kind),
                    actionRepository.countSentBy(space.id(), kind, me),
                    actionRepository.countSentBy(space.id(), kind, partner),
                    actionRepository.lastSentAt(space.id(), kind)));
        }
        String today = LocalDate.now().toString();
        long todayMine = 0;
        long todayPartner = 0;
        for (BondAction action : actionRepository.listBySpace(space.id())) {
            if (!action.onDay(today)) {
                break;
            }
            if (action.sentBy(me)) {
                todayMine++;
            } else {
                todayPartner++;
            }
        }
        return new BondStatsVO(kinds, todayMine + todayPartner, todayMine, todayPartner);
    }

    // ========== 心情回应 ==========

    /** 回应 TA 某天的心情（默认今天）：每人每天一条，重复提交视为修改。 */
    public MoodReactionVO reactMood(String me, String day, String reaction) {
        guard(() -> MoodReaction.requireReaction(reaction));
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String moodDay = rule(() -> Mood.dayOrToday(day));
        guard(() -> MoodReaction.requireNotFuture(moodDay));
        if (moodRepository.findBySpaceAndUserOn(space.id(), partner, moodDay).isEmpty()) {
            throw new BusinessException(400, "TA 那天还没记录心情，先提醒 TA 记一笔吧 💗");
        }
        Optional<MoodReaction> already = moodReactionRepository.findBySpaceAndUserOn(space.id(), moodDay, me);
        if (already.isPresent()) {
            MoodReaction mine = already.get();
            guard(() -> mine.revise(reaction));
            moodReactionRepository.save(mine);
        } else {
            moodReactionRepository.save(rule(() -> MoodReaction.give(space.id(), moodDay, me, reaction)));
        }
        push.pushCoupleEvent("mood-reacted", me, partner,
                "TA 回应了你 " + moodDay + " 的心情：" + MoodReaction.labelOf(reaction)
                        + " " + MoodReaction.emojiOf(reaction));
        return moodReactions(me, moodDay);
    }

    /** 某天（默认今天）双方给彼此心情的回应。 */
    public MoodReactionVO moodReactions(String me, String day) {
        CoupleSpace space = requireSpace(me);
        String moodDay = rule(() -> Mood.dayOrToday(day));
        MoodReaction mine = moodReactionRepository.findBySpaceAndUserOn(space.id(), moodDay, me).orElse(null);
        MoodReaction theirs = moodReactionRepository.findBySpaceAndUserOn(space.id(), moodDay,
                space.partnerOf(me)).orElse(null);
        return new MoodReactionVO(moodDay,
                mine == null ? null : mine.reaction(),
                theirs == null ? null : theirs.reaction());
    }

    // ========== 专属爱称 ==========

    /** 给 TA 设置专属爱称（传空串/null 清除），空间内 TA 的名字会变成它。 */
    public String setPetName(String me, String name) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String nick = rule(() -> space.renamePartner(me, name));
        spaceRepository.save(space);
        push.pushCoupleEvent("pet-name-changed", me, partner,
                nick == null ? "TA 清除了你的专属爱称" : "TA 给你起了新的专属爱称：「" + nick + "」，快去空间看看 🏷️");
        return nick;
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
