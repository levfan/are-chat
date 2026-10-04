package com.smart.chat.couple.domain.surprise;

import com.smart.chat.couple.domain.RuleViolation;

import java.util.UUID;

/**
 * 爱情刮刮乐的一张周券：系统每周给两个人各发一张「来自 TA」的奖励券，
 * 收券人亲手刮开、送券人亲手核销——券的两端各压着一个人，所以它是聚合而不是流水。
 * <p>
 * 它守的是 {@code CONTEXT.md} 给刮刮乐写下的三条约束：
 * <ol>
 *   <li><b>只有收券人能刮</b>：{@link #scratchBy}，替 TA 刮是越权（403 原话）；刮过再点不报错、
 *       不重复推送，返回 false 让用例早退；</li>
 *   <li><b>送券人核销</b>：{@link #redeemBy}，兑现这一签只归送券的那位（403 原话），
 *       而且<b>必须先被刮开</b>（400 原话）；核销时刻就是幂等闸——已有 {@code redeemedAt} 就不再推进，
 *       用例据此不再补分；</li>
 *   <li><b>券面保密</b>：{@link #visiblePrizeFor} 是唯一回显入口，没刮开时只有送券人自己看得到券面，
 *       收券人提前看到就没得刮了。</li>
 * </ol>
 * 「周券懒生成」这条口径需要读存储（本周谁还没有券），判在
 * {@link ScratchRepository#findByWeek} 那一侧，由用例补齐——不在这里编造。
 */
public final class Scratch {

    private final String id;
    private final String spaceId;
    private final String weekKey;
    private final String fromUser;
    private final String owner;
    private final String prizeKind;
    private final String prizeText;
    private boolean scratched;
    private Long scratchedAt;
    private Long redeemedAt;
    private final long created;

    private Scratch(String id, String spaceId, String weekKey, String fromUser, String owner, String prizeKind,
                    String prizeText, boolean scratched, Long scratchedAt, Long redeemedAt, long created) {
        this.id = id;
        this.spaceId = spaceId;
        this.weekKey = weekKey;
        this.fromUser = fromUser;
        this.owner = owner;
        this.prizeKind = prizeKind;
        this.prizeText = prizeText;
        this.scratched = scratched;
        this.scratchedAt = scratchedAt;
        this.redeemedAt = redeemedAt;
        this.created = created;
    }

    /**
     * 发一张周券：券面由外部内容库按「空间+周+收券人」稳定抽好，这里只管发票。
     * 现役没有「人工写券面」的入口，所以不校验内容——编造一道闸门才是假的。
     */
    public static Scratch issue(String spaceId, String weekKey, String fromUser, String owner, String prizeKind,
                               String prizeText) {
        return new Scratch(UUID.randomUUID().toString(), spaceId, weekKey, fromUser, owner, prizeKind, prizeText,
                false, null, null, System.currentTimeMillis());
    }

    /** 从存储重建：不校验——存量券必须读得出来，时间戳为空就按「没刮、没核销」看待。 */
    public static Scratch restore(String id, String spaceId, String weekKey, String fromUser, String owner,
                                  String prizeKind, String prizeText, boolean scratched, Long scratchedAt,
                                  Long redeemedAt, Long created) {
        return new Scratch(id, spaceId, weekKey, fromUser, owner, prizeKind, prizeText, scratched, scratchedAt,
                redeemedAt, created == null ? 0L : created);
    }

    /**
     * 收券人刮开这张券。
     *
     * @return true=刚刮开；false=早就刮过了（幂等，用例据此不再重复推送）
     * @throws RuleViolation 不是收券人（对外 403，文案原样）
     */
    public boolean scratchBy(String me) {
        if (!owner.equals(me)) {
            throw RuleViolation.forbidden("这是 TA 的刮刮乐，不能替 TA 刮哦");
        }
        if (scratched) {
            return false;
        }
        scratched = true;
        scratchedAt = System.currentTimeMillis();
        return true;
    }

    /**
     * 送券人点「已兑现」，让承诺闭环。
     *
     * @return true=这一次真的兑现了（用例据此记一笔分）；false=已经核销过（幂等，不再补分）
     * @throws RuleViolation 不是送券人（403）／还没刮开（400），文案原样
     */
    public boolean redeemBy(String me) {
        if (!fromUser.equals(me)) {
            throw RuleViolation.forbidden("只有送券的人才能点核销哦");
        }
        if (!scratched) {
            throw new RuleViolation("TA 还没刮开这张券呢");
        }
        if (redeemedAt != null) {
            return false;
        }
        redeemedAt = System.currentTimeMillis();
        return true;
    }

    /**
     * 站在某人视角看到的券面：没刮开时只有送券人自己知道内容，
     * 收券人提前看到就没得刮了。对外回显一律走这里，不许直接读 {@link #prizeText()}。
     */
    public String visiblePrizeFor(String viewer) {
        return scratched || fromUser.equals(viewer) ? prizeText : null;
    }

    /** 这张券是不是发给某人的（周券懒生成按人补发要用）。 */
    public boolean ownedBy(String me) {
        return owner.equals(me);
    }

    /** 是不是已经核销过（台账补分的闸门就在这一列上）。 */
    public boolean redeemed() {
        return redeemedAt != null;
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String weekKey() {
        return weekKey;
    }

    public String fromUser() {
        return fromUser;
    }

    public String owner() {
        return owner;
    }

    public String prizeKind() {
        return prizeKind;
    }

    public String prizeText() {
        return prizeText;
    }

    public boolean scratched() {
        return scratched;
    }

    public Long scratchedAt() {
        return scratchedAt;
    }

    public Long redeemedAt() {
        return redeemedAt;
    }

    public long created() {
        return created;
    }
}
