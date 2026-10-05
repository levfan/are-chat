package com.smart.chat.couple.domain.space;

import com.smart.chat.couple.domain.RuleViolation;

import java.util.Set;
import java.util.UUID;

/**
 * 情侣空间聚合根：两个人绑定的唯一容器，持有关系的全部不变式。
 * <p>
 * 刻意<b>不暴露 setter</b>——状态只能通过带语义的方法改变，这样才能守住：
 * <ul>
 *   <li>userA/userB 永远是用户名（区分大小写）字典序小者为 A，全系统只在这一处规范化；</li>
 *   <li>一个空间只能从 ACTIVE 走到 DISSOLVED，且解散要落解散时刻；</li>
 *   <li>主题必须在白名单内，爱称与宣言各有长度闸。</li>
 * </ul>
 * 违规一律抛 {@link RuleViolation}，消息就是用户看到的那句原话（话术属于领域，见
 * {@code couple.domain.RuleViolation}）；与持久化模型（{@code CoupleSpacePO}）的换算只发生在
 * infrastructure 的仓储适配器里。
 */
public final class CoupleSpace {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISSOLVED = "DISSOLVED";

    /** F27 空间主题白名单 */
    public static final Set<String> THEMES = Set.of("classic", "cherry", "ocean", "forest", "night");
    /** 专属爱称上限（F105） */
    public static final int NICK_MAX = 30;
    /** 空间宣言上限（F27） */
    public static final int SLOGAN_MAX = 60;

    private final String id;
    private final String userA;
    private final String userB;
    private final long created;
    private String status;
    private String anniversary;
    private String nickA;
    private String nickB;
    private String slogan;
    private String theme;
    private Long dissolvedAt;

    private CoupleSpace(String id, String left, String right, String status, long created, String anniversary,
                        String nickA, String nickB, String slogan, String theme, Long dissolvedAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("空间必须有 id");
        }
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            throw new IllegalArgumentException("空间必须有两个人");
        }
        // 字典序规范化：A 永远是较小的那个用户名，不接受调用方自觉
        boolean swapped = left.compareTo(right) > 0;
        this.userA = swapped ? right : left;
        this.userB = swapped ? left : right;
        if (this.userA.equals(this.userB)) {
            throw new IllegalArgumentException("不能和自己绑定");
        }
        this.id = id;
        this.status = status;
        this.created = created;
        this.anniversary = anniversary;
        this.nickA = swapped ? nickB : nickA;
        this.nickB = swapped ? nickA : nickB;
        this.slogan = slogan;
        this.theme = theme;
        this.dissolvedAt = dissolvedAt;
    }

    /** 新开空间：双方点头之后才存在 */
    public static CoupleSpace open(String left, String right, long at) {
        return new CoupleSpace(UUID.randomUUID().toString(), left, right, STATUS_ACTIVE, at,
                null, null, null, null, "classic", null);
    }

    /** 从存储还原（不做「是否将来日」这类业务判断，只保证结构合法） */
    public static CoupleSpace restore(String id, String userA, String userB, String status, long created,
                                      String anniversary, String nickA, String nickB, String slogan, String theme,
                                      Long dissolvedAt) {
        return new CoupleSpace(id, userA, userB, status, created, anniversary, nickA, nickB, slogan, theme,
                dissolvedAt);
    }

    // ===== 关系视角 =====

    /** 我在这段关系里的另一半。 */
    public String partnerOf(String me) {
        requireMember(me);
        return userA.equals(me) ? userB : userA;
    }

    public boolean contains(String username) {
        return userA.equals(username) || userB.equals(username);
    }

    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    /** TA 的专属爱称（由对方起的那个），没起过返回 null */
    public String nickOf(String username) {
        return userA.equals(username) ? nickA : nickB;
    }

    /** 爱称只能由另一半来改，本人不能给自己起；空串等于清除，最长 {@link #NICK_MAX} 个字。返回落定的爱称（null 表示已清除）。 */
    public String renamePartner(String me, String nick) {
        String partner = partnerOf(me);
        String cleaned = nick == null ? null : nick.trim();
        if (cleaned != null && cleaned.length() > NICK_MAX) {
            throw new RuleViolation("爱称最长 " + NICK_MAX + " 个字");
        }
        cleaned = blankToNull(cleaned);
        if (userA.equals(partner)) {
            nickA = cleaned;
        } else {
            nickB = cleaned;
        }
        return cleaned;
    }

    // ===== 状态迁移 =====

    /** 解散：只有进行中的空间能解散，且必须落解散时刻 */
    public void dissolve(long at) {
        requireActive("这个空间已经解散了");
        this.status = STATUS_DISSOLVED;
        this.dissolvedAt = at;
    }

    /** 绑定「在一起」的日子（yyyy-MM-dd 的解析由 application 在入口做，这里守业务合法性） */
    public void bindAnniversary(String date) {
        requireActive("已解散的空间不再改纪念日");
        this.anniversary = blankToNull(date);
    }

    /** 宣言 + 主题两件套：每一项的闸都在这里，任一传 null 表示该项不修改 */
    public void decorate(String slogan, String theme) {
        requireActive("已解散的空间不能改装扮");
        if (slogan != null) {
            String text = slogan.trim();
            if (text.length() > SLOGAN_MAX) {
                throw new RuleViolation("宣言最多 " + SLOGAN_MAX + " 字，留白也很美");
            }
            // 空串才是清除；null 表示这一项不动（原来 null 会顺手把宣言抹掉，与注释相反）
            this.slogan = text.isEmpty() ? null : text;
        }
        if (theme != null && !THEMES.contains(theme)) {
            throw new RuleViolation("这个主题还没上架哦");
        }
        if (theme != null) {
            this.theme = theme;
        }
    }

    private void requireActive(String whyNot) {
        if (!isActive()) {
            throw new RuleViolation(whyNot);
        }
    }

    private void requireMember(String me) {
        if (!contains(me)) {
            throw new RuleViolation("不是这个空间的成员：" + me);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public String id() {
        return id;
    }

    public String userA() {
        return userA;
    }

    public String userB() {
        return userB;
    }

    public String status() {
        return status;
    }

    public long created() {
        return created;
    }

    public String anniversary() {
        return anniversary;
    }

    public String slogan() {
        return slogan;
    }

    public String theme() {
        return theme;
    }

    public String nickA() {
        return nickA;
    }

    public String nickB() {
        return nickB;
    }

    public Long dissolvedAt() {
        return dissolvedAt;
    }
}
