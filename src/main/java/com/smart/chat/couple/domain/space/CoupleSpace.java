package com.smart.chat.couple.domain.space;

import com.smart.chat.couple.domain.RuleViolation;

import java.util.UUID;

/**
 * 情侣空间聚合根：两个人绑定的唯一容器，持有关系的全部不变式。
 * <p>
 * 刻意<b>不暴露 setter</b>——状态只能通过带语义的方法改变，这样才能守住：
 * <ul>
 *   <li>userA/userB 永远是用户名（区分大小写）字典序小者为 A，全系统只在这一处规范化；</li>
 *   <li>一个空间只能从 ACTIVE 走到 DISSOLVED，且解散要落解散时刻。</li>
 * </ul>
 * 违规一律抛 {@link RuleViolation}，消息就是用户看到的那句原话（话术属于领域，见
 * {@code couple.domain.RuleViolation}）；与持久化模型（{@code CoupleSpacePO}）的换算只发生在
 * infrastructure 的仓储适配器里。
 * <p>
 * 2026-10-20 三轮裁剪：宣言、主题、爱称三项个性化随其入口一起下线，聚合不再持有它们
 * （{@code couple_space} 的对应列留作死存储，与 {@code couple_notify} 同口径）。
 */
public final class CoupleSpace {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISSOLVED = "DISSOLVED";

    private final String id;
    private final String userA;
    private final String userB;
    private final long created;
    private String status;
    private String anniversary;
    private Long dissolvedAt;

    private CoupleSpace(String id, String left, String right, String status, long created, String anniversary,
                        Long dissolvedAt) {
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
        this.dissolvedAt = dissolvedAt;
    }

    /** 新开空间：双方点头之后才存在 */
    public static CoupleSpace open(String left, String right, long at) {
        return new CoupleSpace(UUID.randomUUID().toString(), left, right, STATUS_ACTIVE, at, null, null);
    }

    /** 从存储还原（不做「是否将来日」这类业务判断，只保证结构合法） */
    public static CoupleSpace restore(String id, String userA, String userB, String status, long created,
                                      String anniversary, Long dissolvedAt) {
        return new CoupleSpace(id, userA, userB, status, created, anniversary, dissolvedAt);
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

    public Long dissolvedAt() {
        return dissolvedAt;
    }
}
