package com.smart.chat.couple.domain.coupon;

import com.smart.chat.couple.domain.RuleViolation;

/**
 * 愿望券聚合：一张券从发出来到被兑现是单向的；发券要先付积分——积分是这套关系里唯一的花法，
 * 所以「余额够不够」是券自己的规则，不是 Service 里的算术。
 * 余额不足的话术要指一条出路（先去好事簿记一笔），这条出路也是产品规则。
 */
public final class WishCoupon {

    public static final int TITLE_MAX = 80;
    /** 发一张券固定花 10 分 */
    public static final int COST = 10;
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_USED = "USED";

    private final String id;
    private final String spaceId;
    private final String title;
    private final String grantedBy;
    private final String ref;
    private final Long issuedAt;
    private String status;
    private String usedBy;
    private Long usedAt;

    private WishCoupon(String id, String spaceId, String title, String grantedBy, String ref, Long issuedAt,
                       String status, String usedBy, Long usedAt) {
        this.id = id;
        this.spaceId = spaceId;
        this.title = title;
        this.grantedBy = grantedBy;
        this.ref = ref;
        this.issuedAt = issuedAt;
        this.status = status;
        this.usedBy = usedBy;
        this.usedAt = usedAt;
    }

    /** 发券：券面必填不超长，且发券人余额够付 COST */
    public static WishCoupon grant(String title, String from, int balance) {
        return grant(null, title, from, balance);
    }

    /** 发券（带所属空间）：规则同上，空间只是这枚券的归属，不参与判定 */
    public static WishCoupon grant(String spaceId, String title, String from, int balance) {
        String text = title == null ? "" : title.trim();
        if (text.isEmpty()) {
            throw new RuleViolation("券面写点什么愿望吧");
        }
        if (text.length() > TITLE_MAX) {
            throw new RuleViolation("最多 " + TITLE_MAX + " 个字，心意不在字数");
        }
        if (balance < COST) {
            throw new RuleViolation("发一张愿望券要 " + COST + " 分，你只有 " + balance
                    + " 分——先去好事簿记一笔 TA 为你做过的事吧");
        }
        return new WishCoupon(null, spaceId, text, from, null, null, STATUS_OPEN, null, null);
    }

    public static WishCoupon restore(String id, String title, String grantedBy, String status,
                                     String usedBy, Long usedAt) {
        return new WishCoupon(id, null, title, grantedBy, null, null, status, usedBy, usedAt);
    }

    /** 从存储还原完整一张券（含所属空间、备注 ref 与发出时刻——券本列表要按它们排序与回显） */
    public static WishCoupon restore(String id, String spaceId, String title, String grantedBy, String ref,
                                     Long issuedAt, String status, String usedBy, Long usedAt) {
        return new WishCoupon(id, spaceId, title, grantedBy, ref, issuedAt, status, usedBy, usedAt);
    }

    /** 兑现：一张券只能用掉一次，用过再点是业务闸门（幂等由 Service 在更外层处理） */
    public void useBy(String me, long at) {
        if (!isOpen()) {
            throw new RuleViolation("这张券已经核销过了");
        }
        this.status = STATUS_USED;
        this.usedBy = me;
        this.usedAt = at;
    }

    public boolean isOpen() {
        return STATUS_OPEN.equals(status);
    }

    /** 这张券是不是发给我的（发券人自己不算收券人） */
    public boolean addressedTo(String me) {
        return grantedBy != null && !grantedBy.equals(me);
    }

    public String id() {
        return id;
    }

    /** 所属空间（券不出空间，适配器按它回查） */
    public String spaceId() {
        return spaceId;
    }

    /** 券面备注（现役一律空串，仍要读回来才能整行回写） */
    public String ref() {
        return ref;
    }

    /** 发出时刻，券本列表按它倒序 */
    public Long issuedAt() {
        return issuedAt;
    }

    public String title() {
        return title;
    }

    public String grantedBy() {
        return grantedBy;
    }

    public String status() {
        return status;
    }

    public String usedBy() {
        return usedBy;
    }

    public Long usedAt() {
        return usedAt;
    }
}
