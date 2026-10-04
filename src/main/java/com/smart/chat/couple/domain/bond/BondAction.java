package com.smart.chat.couple.domain.bond;

import com.smart.chat.couple.domain.RuleViolation;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * 一次贴贴：一键发给 TA 的轻量示好动作（戳一戳/抱抱/亲亲/捏捏脸/蹭蹭/挠痒痒/在想你）。
 * <p>
 * 发出去就是<b>一行只追加的流水</b>，没有「改一次贴贴」这种操作，所以这里只有带校验的工厂和访问器，
 * 不编造行为。搬进这一层的三件事都是对外契约：
 * <ul>
 *   <li><b>动作目录</b>：{@link #emojiOf}／{@link #labelOf} 的 emoji 与中文名逐个字符照自
 *       {@code CoupleActionPO}（PO 那份按禁改留着，两边由 ActionRepositoryAdapterTest 逐字对齐），
 *       前端在按这套目录渲染；{@link #displayedKinds} 是看板里各类动作的展示顺序；</li>
 *   <li><b>推送话术</b>：{@link #pushText()} 是对方实时收到的那句，改一个字都是改产品口径；</li>
 *   <li><b>里程碑</b>：只有 {@link #celebrates} 里那三类动作参与，累计到 {@code 1/10/50/100/520/1314}
 *       次时双方一起庆祝（{@link #milestoneAt} 命中一个就够，不再连击）。</li>
 * </ul>
 * 「双方当天都发过贴贴才算一天」不在这里判——那要读整条流水，见 {@code CoupleStreakService} 与
 * {@code bondDays} 的同源口径（{@code CONTEXT.md}）。
 */
public final class BondAction {

    public static final String KIND_POKE = "POKE";
    public static final String KIND_HUG = "HUG";
    public static final String KIND_KISS = "KISS";
    public static final String KIND_PAT = "PAT";
    public static final String KIND_NUZZLE = "NUZZLE";
    public static final String KIND_TICKLE = "TICKLE";
    public static final String KIND_MISS = "MISS";

    /** 看板里动作目录的展示顺序（改了就是换前端卡片的顺序）。 */
    private static final List<String> DISPLAY_ORDER =
            List.of(KIND_MISS, KIND_HUG, KIND_KISS, KIND_POKE, KIND_PAT, KIND_NUZZLE, KIND_TICKLE);

    /** 参与里程碑庆祝的动作：只有抱抱/亲亲/想念会计整数关口。 */
    private static final List<String> MILESTONE_KINDS = List.of(KIND_HUG, KIND_KISS, KIND_MISS);

    /** 贴贴里程碑关口（某类动作累计达到次数时双方推送庆祝）。 */
    private static final long[] MILESTONES = {1, 10, 50, 100, 520, 1314};

    private final String id;
    private final String spaceId;
    private final String username;
    private final String kind;
    private final Long created;

    private BondAction(String id, String spaceId, String username, String kind, Long created) {
        this.id = id;
        this.spaceId = spaceId;
        this.username = username;
        this.kind = kind;
        this.created = created;
    }

    /**
     * 发一次贴贴：认得的动作才发得出去，id 与时刻现场盖。
     *
     * @throws RuleViolation 动作不在目录里（对外 400，文案原样）
     */
    public static BondAction sent(String spaceId, String username, String kind) {
        requireKind(kind);
        return new BondAction(UUID.randomUUID().toString(), spaceId, username, kind, System.currentTimeMillis());
    }

    /** 从存储重建：不校验——存量行必须读得出来（历史上可能存过已下线的动作键）。 */
    public static BondAction restore(String id, String spaceId, String username, String kind, Long created) {
        return new BondAction(id, spaceId, username, kind, created);
    }

    /** 动作白名单：目录之外的键一律不认。 */
    public static String requireKind(String kind) {
        if (!isValidKind(kind)) {
            throw new RuleViolation("不认识这个动作哦，换一个试试～");
        }
        return kind;
    }

    public static boolean isValidKind(String kind) {
        return KIND_POKE.equals(kind) || KIND_HUG.equals(kind) || KIND_KISS.equals(kind)
                || KIND_PAT.equals(kind) || KIND_NUZZLE.equals(kind) || KIND_TICKLE.equals(kind)
                || KIND_MISS.equals(kind);
    }

    /** 动作的 emoji 图标。 */
    public static String emojiOf(String kind) {
        return switch (kind == null ? "" : kind) {
            case KIND_POKE -> "👉";
            case KIND_HUG -> "🤗";
            case KIND_KISS -> "💋";
            case KIND_PAT -> "🫳";
            case KIND_NUZZLE -> "😚";
            case KIND_TICKLE -> "🤭";
            case KIND_MISS -> "💌";
            default -> "💕";
        };
    }

    /** 动作的中文名。 */
    public static String labelOf(String kind) {
        return switch (kind == null ? "" : kind) {
            case KIND_POKE -> "戳一戳";
            case KIND_HUG -> "抱抱";
            case KIND_KISS -> "亲亲";
            case KIND_PAT -> "捏捏脸";
            case KIND_NUZZLE -> "蹭蹭";
            case KIND_TICKLE -> "挠痒痒";
            case KIND_MISS -> "在想你";
            default -> "贴贴";
        };
    }

    /** 看板要列的动作类别（按现役展示顺序）。 */
    public static List<String> displayedKinds() {
        return DISPLAY_ORDER;
    }

    /** 这一类动作累计到整数关口时要不要双方一起庆祝。 */
    public static boolean celebrates(String kind) {
        return MILESTONE_KINDS.contains(kind);
    }

    /** 累计次数正好踩中哪个里程碑；没踩中返回 null。 */
    public static Long milestoneAt(long total) {
        for (long milestone : MILESTONES) {
            if (total == milestone) {
                return milestone;
            }
        }
        return null;
    }

    /** 里程碑那句庆祝话。 */
    public static String milestoneDetail(String kind, long milestone) {
        return "第 " + milestone + " 次「" + labelOf(kind) + "」达成 🎉" + emojiOf(kind) + " 你们好甜！";
    }

    /** 对方实时收到的那句推送文案。 */
    public String pushText() {
        return switch (kind) {
            case KIND_POKE -> "TA 戳了戳你 👉 快回戳！";
            case KIND_HUG -> "TA 给了你一个大大的拥抱 🤗 快抱回去！";
            case KIND_KISS -> "TA 亲了你一口 💋 嘻嘻";
            case KIND_PAT -> "TA 捏了捏你的脸 🫳 好软";
            case KIND_NUZZLE -> "TA 蹭了蹭你 😚 好黏人";
            case KIND_TICKLE -> "TA 挠你痒痒 🤭 哈哈哈别跑！";
            case KIND_MISS -> "TA 说 TA 在想你 💌 现在立刻马上";
            default -> "TA 贴了贴你 💕";
        };
    }

    /** 这一下是谁发的（今日双方动作数按这个分堆）。 */
    public boolean sentBy(String me) {
        return username.equals(me);
    }

    /** 是不是发在指定的那一天（按服务器时区的自然日，现役口径）。 */
    public boolean onDay(String day) {
        return Instant.ofEpochMilli(created).atZone(ZoneId.systemDefault()).toLocalDate().toString().equals(day);
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String username() {
        return username;
    }

    public String kind() {
        return kind;
    }

    public Long created() {
        return created;
    }
}
