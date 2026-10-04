package com.smart.chat.couple.domain.mood;

import com.smart.chat.couple.domain.RuleViolation;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 心情回应：看到 TA 某天的日记，贴一个抱抱/亲亲/加油/摸摸头过去。
 * <p>
 * 它是独立于 {@link Mood} 的一条记录（{@code CONTEXT.md}：回应另算），守三条现役规矩：
 * <ol>
 *   <li><b>回应只有四种</b>：目录之外的键一律不认（{@link #requireReaction}）；</li>
 *   <li><b>不能回应未来</b>：还没发生的日子没有心情可接（{@link #requireNotFuture}，按字符串比自然日）；</li>
 *   <li><b>每人每天一条，重复提交视为修改</b>：改写只动回应本身并盖修改时刻，
 *       第一次贴则只盖创建时刻（与 {@code CoupleMoodReactionPO.of} 同口径）。</li>
 * </ol>
 * 「那天 TA 到底记没记心情」需要读存储，不在聚合里判，见 {@code CoupleBondService.reactMood}。
 * 回应归属也不在这里判：端口只按 {@code (空间, 心情日, 回应人)} 取行，取的永远是自己的那一行。
 */
public final class MoodReaction {

    public static final String REACTION_HUG = "HUG";
    public static final String REACTION_KISS = "KISS";
    public static final String REACTION_CHEER = "CHEER";
    public static final String REACTION_PAT = "PAT";

    private final String id;
    private final String spaceId;
    private final String moodDay;
    private final String fromUser;
    private String reaction;
    private final Long created;
    private Long updatedAt;

    private MoodReaction(String id, String spaceId, String moodDay, String fromUser, String reaction,
                         Long created, Long updatedAt) {
        this.id = id;
        this.spaceId = spaceId;
        this.moodDay = moodDay;
        this.fromUser = fromUser;
        this.reaction = reaction;
        this.created = created;
        this.updatedAt = updatedAt;
    }

    /**
     * 第一次给 TA 那天的心情贴回应：盖过白名单与「不能回应未来」两道闸门，时刻现场盖。
     *
     * @throws RuleViolation 回应不在目录里／那天还在未来（对外 400，文案原样）
     */
    public static MoodReaction give(String spaceId, String moodDay, String fromUser, String reaction) {
        requireReaction(reaction);
        requireNotFuture(moodDay);
        return new MoodReaction(UUID.randomUUID().toString(), spaceId, moodDay, fromUser, reaction,
                System.currentTimeMillis(), null);
    }

    /** 从存储重建：不校验——存量行必须读得出来。 */
    public static MoodReaction restore(String id, String spaceId, String moodDay, String fromUser, String reaction,
                                      Long created, Long updatedAt) {
        return new MoodReaction(id, spaceId, moodDay, fromUser, reaction, created, updatedAt);
    }

    /**
     * 同一天改主意：换一个回应并记下修改时刻。
     *
     * @throws RuleViolation 回应不在目录里（对外 400，文案原样）
     */
    public void revise(String reaction) {
        this.reaction = requireReaction(reaction);
        this.updatedAt = System.currentTimeMillis();
    }

    /** 回应白名单。 */
    public static String requireReaction(String reaction) {
        if (!isValidReaction(reaction)) {
            throw new RuleViolation("回应只能是抱抱/亲亲/加油/摸摸头哦");
        }
        return reaction;
    }

    public static boolean isValidReaction(String reaction) {
        return REACTION_HUG.equals(reaction) || REACTION_KISS.equals(reaction)
                || REACTION_CHEER.equals(reaction) || REACTION_PAT.equals(reaction);
    }

    /** 那天的闸门：日子写成未来就接不到（现役按 yyyy-MM-dd 字符串比）。 */
    public static String requireNotFuture(String moodDay) {
        if (moodDay.compareTo(LocalDate.now().toString()) > 0) {
            throw new RuleViolation("不能回应未来的心情哦");
        }
        return moodDay;
    }

    /** 回应的中文名（推送文案在用，目录照搬 {@code CoupleMoodReactionPO.labelOf}）。 */
    public static String labelOf(String reaction) {
        return switch (reaction == null ? "" : reaction) {
            case REACTION_HUG -> "抱抱";
            case REACTION_KISS -> "亲亲";
            case REACTION_CHEER -> "加油";
            case REACTION_PAT -> "摸摸头";
            default -> "回应";
        };
    }

    /** 回应的表情（同上）。 */
    public static String emojiOf(String reaction) {
        return switch (reaction == null ? "" : reaction) {
            case REACTION_HUG -> "🤗";
            case REACTION_KISS -> "💋";
            case REACTION_CHEER -> "💪";
            case REACTION_PAT -> "🫶";
            default -> "💕";
        };
    }

    /** 这条是谁贴的。 */
    public boolean respondedBy(String me) {
        return fromUser.equals(me);
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String moodDay() {
        return moodDay;
    }

    public String fromUser() {
        return fromUser;
    }

    public String reaction() {
        return reaction;
    }

    public Long created() {
        return created;
    }

    public Long updatedAt() {
        return updatedAt;
    }
}
