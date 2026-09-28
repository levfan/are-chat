package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 贴贴互动：一键发送亲密小动作（戳一戳/抱抱/亲亲/捏捏脸/蹭蹭/挠痒痒/在想你）、
 * 心情回应（给 TA 的心情贴抱抱/亲亲/加油/摸摸头）与专属爱称。
 * 情绪价值设计：动作必达（对方实时收到推送）、贴贴里程碑庆祝、心情不好时一键传递温度。
 */
@Service
public class CoupleBondService {

    /** 贴贴里程碑：某类动作累计达到次数时双方推送庆祝。 */
    private static final long[] MILESTONES = {1, 10, 50, 100, 520, 1314};

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

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleActionMapper actionMapper;
    private final CoupleMoodReactionMapper moodReactionMapper;
    private final CoupleMoodMapper moodMapper;
    private final ImPushService push;

    public CoupleBondService(CoupleSpaceMapper spaceMapper, CoupleActionMapper actionMapper,
                             CoupleMoodReactionMapper moodReactionMapper, CoupleMoodMapper moodMapper,
                             ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.actionMapper = actionMapper;
        this.moodReactionMapper = moodReactionMapper;
        this.moodMapper = moodMapper;
        this.push = push;
    }

    // ========== 亲密小动作 ==========

    /** 发送一个贴贴动作：对方实时收到推送；命中里程碑时双方一起庆祝。 */
    public BondStatsVO sendAction(String me, String kind) {
        if (!CoupleAction.isValidKind(kind)) {
            throw new BusinessException(400, "不认识这个动作哦，换一个试试～");
        }
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        actionMapper.insert(CoupleAction.of(space.getId(), me, kind));
        push.pushCoupleEvent("bond-action", me, partner, actionPushDetail(kind));
        // 贴贴里程碑：抱抱/亲亲/想念累计到整数关口，双方一起庆祝
        if (CoupleAction.KIND_HUG.equals(kind) || CoupleAction.KIND_KISS.equals(kind)
                || CoupleAction.KIND_MISS.equals(kind)) {
            long total = actionMapper.countByKind(space.getId(), kind);
            for (long milestone : MILESTONES) {
                if (total == milestone) {
                    String detail = "第 " + milestone + " 次「" + CoupleAction.labelOf(kind) + "」达成 🎉"
                            + CoupleAction.emojiOf(kind) + " 你们好甜！";
                    push.pushCoupleEventBoth("bond-milestone", me, me, partner, detail);
                    break;
                }
            }
        }
        return stats(me);
    }

    /** 最近动作流（新→旧，默认 50 条上限）。 */
    public List<ActionVO> recentActions(String me, Integer limit) {
        CoupleSpace space = requireSpace(me);
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, 200);
        return actionMapper.findBySpace(space.getId()).stream()
                .limit(size)
                .map(a -> new ActionVO(a.getId(), a.getUsername(), a.getKind(), a.getCreated()))
                .toList();
    }

    /** 贴贴统计：各类动作累计/双方占比/最近时间 + 今日双方动作数。 */
    public BondStatsVO stats(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        List<KindStat> kinds = new ArrayList<>();
        for (String kind : List.of(CoupleAction.KIND_MISS, CoupleAction.KIND_HUG, CoupleAction.KIND_KISS,
                CoupleAction.KIND_POKE, CoupleAction.KIND_PAT, CoupleAction.KIND_NUZZLE,
                CoupleAction.KIND_TICKLE)) {
            kinds.add(new KindStat(kind, CoupleAction.emojiOf(kind), CoupleAction.labelOf(kind),
                    actionMapper.countByKind(space.getId(), kind),
                    actionMapper.countByKindAndUser(space.getId(), kind, me),
                    actionMapper.countByKindAndUser(space.getId(), kind, partner),
                    actionMapper.lastCreatedAt(space.getId(), kind)));
        }
        String today = LocalDate.now().toString();
        long todayMine = 0;
        long todayPartner = 0;
        for (CoupleAction action : actionMapper.findBySpace(space.getId())) {
            String day = java.time.Instant.ofEpochMilli(action.getCreated())
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
            if (!today.equals(day)) {
                break;
            }
            if (action.getUsername().equals(me)) {
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
        if (!CoupleMoodReaction.isValidReaction(reaction)) {
            throw new BusinessException(400, "回应只能是抱抱/亲亲/加油/摸摸头哦");
        }
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String moodDay = day == null || day.isBlank() ? LocalDate.now().toString() : normalizeDay(day);
        if (moodDay.compareTo(LocalDate.now().toString()) > 0) {
            throw new BusinessException(400, "不能回应未来的心情哦");
        }
        if (moodMapper.find(space.getId(), partner, moodDay).isEmpty()) {
            throw new BusinessException(400, "TA 那天还没记录心情，先提醒 TA 记一笔吧 💗");
        }
        CoupleMoodReaction existing = moodReactionMapper.find(space.getId(), moodDay, me);
        if (existing != null) {
            existing.setReaction(reaction);
            existing.setUpdatedAt(System.currentTimeMillis());
            moodReactionMapper.updateById(existing);
        } else {
            moodReactionMapper.insert(CoupleMoodReaction.of(space.getId(), moodDay, me, reaction));
        }
        push.pushCoupleEvent("mood-reacted", me, partner,
                "TA 回应了你 " + moodDay + " 的心情：" + CoupleMoodReaction.labelOf(reaction)
                        + " " + CoupleMoodReaction.emojiOf(reaction));
        return moodReactions(me, moodDay);
    }

    /** 某天（默认今天）双方给彼此心情的回应。 */
    public MoodReactionVO moodReactions(String me, String day) {
        CoupleSpace space = requireSpace(me);
        String moodDay = day == null || day.isBlank() ? LocalDate.now().toString() : normalizeDay(day);
        CoupleMoodReaction mine = moodReactionMapper.find(space.getId(), moodDay, me);
        CoupleMoodReaction theirs = moodReactionMapper.find(space.getId(), moodDay, space.partnerOf(me));
        return new MoodReactionVO(moodDay,
                mine == null ? null : mine.getReaction(),
                theirs == null ? null : theirs.getReaction());
    }

    // ========== 专属爱称 ==========

    /** 给 TA 设置专属爱称（传空串/null 清除），空间内 TA 的名字会变成它。 */
    public String setPetName(String me, String name) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String nick = name == null ? null : name.trim();
        if (nick != null && nick.isEmpty()) {
            nick = null;
        }
        if (nick != null && nick.length() > 30) {
            throw new BusinessException(400, "爱称最长 30 个字");
        }
        space.setNickOf(partner, nick);
        spaceMapper.updateById(space);
        push.pushCoupleEvent("pet-name-changed", me, partner,
                nick == null ? "TA 清除了你的专属爱称" : "TA 给你起了新的专属爱称：「" + nick + "」，快去空间看看 🏷️");
        return nick;
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    private String normalizeDay(String value) {
        try {
            return LocalDate.parse(value.trim()).toString();
        } catch (Exception e) {
            throw new BusinessException(400, "日期格式应为 yyyy-MM-dd");
        }
    }

    private String actionPushDetail(String kind) {
        return switch (kind) {
            case CoupleAction.KIND_POKE -> "TA 戳了戳你 👉 快回戳！";
            case CoupleAction.KIND_HUG -> "TA 给了你一个大大的拥抱 🤗 快抱回去！";
            case CoupleAction.KIND_KISS -> "TA 亲了你一口 💋 嘻嘻";
            case CoupleAction.KIND_PAT -> "TA 捏了捏你的脸 🫳 好软";
            case CoupleAction.KIND_NUZZLE -> "TA 蹭了蹭你 😚 好黏人";
            case CoupleAction.KIND_TICKLE -> "TA 挠你痒痒 🤭 哈哈哈别跑！";
            case CoupleAction.KIND_MISS -> "TA 说 TA 在想你 💌 现在立刻马上";
            default -> "TA 贴了贴你 💕";
        };
    }
}
