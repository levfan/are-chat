package com.smart.chat.couple.domain.surprise;

import com.smart.chat.couple.domain.RuleViolation;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 恋爱盲盒：一句话或小任务装进盒子，指定日子留给对方拆——制造一天的期待。
 * <p>
 * 它守的是 {@code CONTEXT.md} 给盲盒写下的三条约束：
 * <ol>
 *   <li><b>装盒闸门</b>：{@link #pack} 里 kind 只能是 whisper/task、内容不能空且不超过
 *       {@value #CONTENT_MAX} 字、开箱日必须是合法日期且<b>最早明天</b>（四句原话照搬）；</li>
 *   <li><b>装盒人不能自拆</b>：{@link #openBy} 先过归属闸门（403 原话），再看日子；</li>
 *   <li><b>到日才可拆</b>：没到日子就把还差几天算进话术里（400 原话），拆过再点返回 false 走早退。</li>
 * </ol>
 * 盒子的内容在拆开之前对收盒人保密，但到期日一到就藏不住了（{@link #visibleContentFor}
 * 与现役投影口径一致：到日之前只对装盒人本人保密）。
 */
public final class MysteryBox {

    /** 悄悄话（枚举字面量是对外契约，照 PO 原值写死）。 */
    public static final String KIND_WHISPER = "whisper";
    /** 小任务。 */
    public static final String KIND_TASK = "task";
    /** 盒子容量（与 {@code couple_mystery_box.content} 的列宽一致）。 */
    public static final int CONTENT_MAX = 300;

    private final String id;
    private final String spaceId;
    private final String fromUser;
    private final String kind;
    private final String content;
    private final String openDay;
    private boolean opened;
    private Long openedAt;
    private final long created;

    private MysteryBox(String id, String spaceId, String fromUser, String kind, String content, String openDay,
                       boolean opened, Long openedAt, long created) {
        this.id = id;
        this.spaceId = spaceId;
        this.fromUser = fromUser;
        this.kind = kind;
        this.content = content;
        this.openDay = openDay;
        this.opened = opened;
        this.openedAt = openedAt;
        this.created = created;
    }

    /**
     * 装一个盲盒：闸门全在这里，顺序也和现役一致——先校验内容与日期，再谈空间归属。
     * 此时盒子还不知道自己进哪个空间，由 {@link #intoSpace} 落位。
     *
     * @param today 今天，用于「最早明天才能拆」这条闸
     * @throws RuleViolation 类型不对／内容为空或超长／日期不合法或不到明天（对外 400，文案原样）
     */
    public static MysteryBox pack(String fromUser, String kind, String content, String openDay, LocalDate today) {
        if (!isValidKind(kind)) {
            throw new RuleViolation("盲盒只能是悄悄话或小任务哦");
        }
        if (content == null || content.isBlank()) {
            throw new RuleViolation("盒子里总要放点什么吧～");
        }
        // 现役长度闸量的是用户提交的原样，落库才去首尾空白
        if (content.length() > CONTENT_MAX) {
            throw new RuleViolation("盒子太小啦，最多装 " + CONTENT_MAX + " 个字");
        }
        LocalDate open;
        try {
            open = LocalDate.parse(openDay);
        } catch (Exception e) {
            throw new RuleViolation("开箱日期不认识，选一个明天以后的日子吧");
        }
        if (!open.isAfter(today)) {
            throw new RuleViolation("盲盒最早明天才能拆哦，期待感要留足 ✨");
        }
        return new MysteryBox(UUID.randomUUID().toString(), null, fromUser, kind, content.trim(), open.toString(),
                false, null, System.currentTimeMillis());
    }

    /** 落位到某个空间：返回装进那个空间后的同一个盒子（现役「先校验再取空间」的顺序需要这一步）。 */
    public MysteryBox intoSpace(String spaceId) {
        return new MysteryBox(id, spaceId, fromUser, kind, content, openDay, opened, openedAt, created);
    }

    /** 从存储重建：不校验——存量盒子必须读得出来，日期口径由 {@link #openDay()} 原样保留。 */
    public static MysteryBox restore(String id, String spaceId, String fromUser, String kind, String content,
                                     String openDay, boolean opened, Long openedAt, Long created) {
        return new MysteryBox(id, spaceId, fromUser, kind, content, openDay, opened, openedAt,
                created == null ? 0L : created);
    }

    /**
     * 收盒人拆盒。
     *
     * @return true=刚拆开（用例据此推送 box-opened）；false=早就拆过了（幂等，不再推第二遍）
     * @throws RuleViolation 装盒人自拆（403）／还没到开箱日（400），文案原样
     */
    public boolean openBy(String me, LocalDate today) {
        if (fromUser.equals(me)) {
            throw RuleViolation.forbidden("自己装的盒子自己拆，就不惊喜了呀 😝");
        }
        if (openDay.compareTo(today.toString()) > 0) {
            long waitDays = LocalDate.parse(openDay).toEpochDay() - today.toEpochDay();
            throw new RuleViolation("还没到开箱日！再等 " + waitDays + " 天，让期待多飞一会儿 🎈");
        }
        if (opened) {
            return false;
        }
        opened = true;
        openedAt = System.currentTimeMillis();
        return true;
    }

    /**
     * 站在某人视角看到盒子里的话：拆开了、或者本来就是自己装的、或者日子已经到了，
     * 才看得到内容；否则返回 null——前端那一格留白就是「还有期待」。
     */
    public String visibleContentFor(String viewer, LocalDate today) {
        return opened || fromUser.equals(viewer) || openDay.compareTo(today.toString()) <= 0 ? content : null;
    }

    /** 这一格按钮能不能按：不是自己装的、还没拆、日子到了。 */
    public boolean openableBy(String me, LocalDate today) {
        return !fromUser.equals(me) && !opened && openDay.compareTo(today.toString()) <= 0;
    }

    /** 开箱日（推送文案要报「几月几日」）。 */
    public LocalDate openDate() {
        return LocalDate.parse(openDay);
    }

    public static boolean isValidKind(String kind) {
        return KIND_WHISPER.equals(kind) || KIND_TASK.equals(kind);
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String fromUser() {
        return fromUser;
    }

    public String kind() {
        return kind;
    }

    public String content() {
        return content;
    }

    public String openDay() {
        return openDay;
    }

    public boolean opened() {
        return opened;
    }

    public Long openedAt() {
        return openedAt;
    }

    public long created() {
        return created;
    }
}
