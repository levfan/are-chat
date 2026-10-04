package com.smart.chat.couple.domain.space;

import java.util.Set;
import java.util.UUID;

/**
 * 情侣空间聚合根：两个人绑定的唯一容器，持有关系的全部不变式。
 * <p>
 * 刻意<b>不暴露 setter</b>——状态只能通过带语义的方法改变，这样才能守住：
 * <ul>
 *   <li>userA/userB 永远是用户名（区分大小写）字典序小者为 A，全系统只在这一处规范化；</li>
 *   <li>一个空间只能从 ACTIVE 走到 DISSOLVED，且解散要落解散时刻；</li>
 *   <li>主题必须在白名单内，贴纸佩戴数不超过 {@link #STICKER_MAX}。</li>
 * </ul>
 * 与持久化模型（{@code CoupleSpacePO}）的换算只发生在 infrastructure 的仓储适配器里。
 */
public final class CoupleSpace {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DISSOLVED = "DISSOLVED";

    /** F27 空间主题白名单 */
    public static final Set<String> THEMES = Set.of("classic", "cherry", "ocean", "forest", "night");
    /** F28 贴纸墙佩戴上限 */
    public static final int STICKER_MAX = 6;

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
    private String stickers;
    private Long dissolvedAt;

    private CoupleSpace(String id, String left, String right, String status, long created, String anniversary,
                        String nickA, String nickB, String slogan, String theme, String stickers, Long dissolvedAt) {
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
        this.stickers = stickers;
        this.dissolvedAt = dissolvedAt;
    }

    /** 新开空间：双方点头之后才存在 */
    public static CoupleSpace open(String left, String right, long at) {
        return new CoupleSpace(UUID.randomUUID().toString(), left, right, STATUS_ACTIVE, at,
                null, null, null, null, "classic", null, null);
    }

    /** 从存储还原（不做「是否将来日」这类业务判断，只保证结构合法） */
    public static CoupleSpace restore(String id, String userA, String userB, String status, long created,
                                      String anniversary, String nickA, String nickB, String slogan, String theme,
                                      String stickers, Long dissolvedAt) {
        return new CoupleSpace(id, userA, userB, status, created, anniversary, nickA, nickB, slogan, theme,
                stickers, dissolvedAt);
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

    /** 爱称只能由另一半来改，本人不能给自己起 */
    public void renamePartner(String me, String nick) {
        String partner = partnerOf(me);
        if (userA.equals(partner)) {
            nickA = blankToNull(nick);
        } else {
            nickB = blankToNull(nick);
        }
    }

    // ===== 状态迁移 =====

    /** 解散：只有进行中的空间能解散，且必须落解散时刻 */
    public void dissolve(long at) {
        if (!isActive()) {
            throw new IllegalStateException("这个空间已经解散了");
        }
        this.status = STATUS_DISSOLVED;
        this.dissolvedAt = at;
    }

    /** 绑定「在一起」的日子（yyyy-MM-dd 的格式校验由调用方在入口做，这里是业务合法性） */
    public void bindAnniversary(String date) {
        if (!isActive()) {
            throw new IllegalStateException("已解散的空间不再改纪念日");
        }
        this.anniversary = blankToNull(date);
    }

    /** 宣言 + 主题 + 贴纸墙三件套：主题与贴纸数量都在这里有闸 */
    public void decorate(String slogan, String theme, String stickers) {
        if (!isActive()) {
            throw new IllegalStateException("已解散的空间不能改装扮");
        }
        if (theme != null && !THEMES.contains(theme)) {
            throw new IllegalArgumentException("没有这个空间主题：" + theme);
        }
        if (stickers != null && countStickers(stickers) > STICKER_MAX) {
            throw new IllegalArgumentException("贴纸最多佩戴 " + STICKER_MAX + " 枚");
        }
        this.slogan = blankToNull(slogan);
        if (theme != null) {
            this.theme = theme;
        }
        if (stickers != null) {
            this.stickers = blankToNull(stickers);
        }
    }

    private static int countStickers(String csv) {
        int n = 0;
        for (String part : csv.split(",")) {
            if (!part.isBlank()) {
                n++;
            }
        }
        return n;
    }

    private void requireMember(String me) {
        if (!contains(me)) {
            throw new IllegalArgumentException("不是这个空间的成员：" + me);
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

    public String stickers() {
        return stickers;
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
