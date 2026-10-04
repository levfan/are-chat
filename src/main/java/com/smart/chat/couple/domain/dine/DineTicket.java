package com.smart.chat.couple.domain.dine;

import com.smart.chat.couple.domain.RuleViolation;

import java.util.UUID;

/**
 * 今晚饭票：每人每天一道提名，改票即覆盖同一行（uk(space_id, day, from_user) 一人一票）。
 * <p>
 * 聚合守住两条真实规则（{@code docs/ddd/05-tactical-playbook.md} 第 1 节第 3 条）：
 * <ol>
 *   <li><b>内容闸门</b>：菜名必填不空白、最长 {@value #DISH_MAX} 字；理由可空、最长 {@value #TEXT_MAX} 字——
 *       原话照搬现役文案；</li>
 *   <li><b>撞菜判定</b>：{@link #sameDish} 是「你们投了同一道」这条缘分判定，忽略大小写，
 *       命中与否决定推 {@code dine-hit} 还是 {@code dine-ticket}。</li>
 * </ol>
 * 「吃什么」的 stableHash 裁决不在此列——它要吃 {@code infrastructure.content} 的哈希工具，
 * 而领域层不许依赖别的层（见 ArchitectureGuardTest 的 domainLayerStaysPure），故裁决留在 application 用例里，
 * 结果口径与原实现逐字一致（同空间同一天必给同一道、双方同见）。
 */
public final class DineTicket {

    public static final int DISH_MAX = 30;
    public static final int TEXT_MAX = 80;

    private final String id;
    private final String spaceId;
    private final String day;
    private final String fromUser;
    private final String dish;
    private final String reason;
    private final long created;

    private DineTicket(String id, String spaceId, String day, String fromUser, String dish, String reason, long created) {
        this.id = id;
        this.spaceId = spaceId;
        this.day = day;
        this.fromUser = fromUser;
        this.dish = dish;
        this.reason = reason;
        this.created = created;
    }

    /**
     * 投今晚一票：清洗菜名与理由，现场发 id 与提名时刻。
     *
     * @throws RuleViolation 菜名空白／超长／理由超长（对外 400，文案原样）
     */
    public static DineTicket offer(String spaceId, String day, String fromUser, String dish, String reason) {
        String d = dish == null ? "" : dish.trim();
        if (d.isEmpty()) {
            throw new RuleViolation("先写下今晚想吃什么呀 🍚");
        }
        if (d.length() > DISH_MAX) {
            throw new RuleViolation("菜名 30 字以内哦");
        }
        String r = reason == null ? "" : reason.trim();
        if (r.length() > TEXT_MAX) {
            throw new RuleViolation("理由 80 字以内哦");
        }
        return new DineTicket(UUID.randomUUID().toString(), spaceId, day, fromUser, d, r, System.currentTimeMillis());
    }

    /** 从存储重建：不校验——存量票必须读得出来。 */
    public static DineTicket restore(String id, String spaceId, String day, String fromUser, String dish,
                                     String reason, Long created) {
        return new DineTicket(id, spaceId, day, fromUser, dish, reason, created == null ? 0L : created);
    }

    /** 撞菜：两个人投了同一道菜（忽略大小写），对方没投或菜名为空都不算。 */
    public boolean sameDish(DineTicket other) {
        return other != null && dish != null && dish.equalsIgnoreCase(other.dish);
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String day() {
        return day;
    }

    public String fromUser() {
        return fromUser;
    }

    public String dish() {
        return dish;
    }

    public String reason() {
        return reason;
    }

    public long created() {
        return created;
    }
}
