package com.smart.chat.couple.domain.wish;

import com.smart.chat.couple.domain.RuleViolation;

/**
 * 愿望清单里的一条愿望：谁想要的（owner）与谁记下来的（creator）可以不是同一个人。
 * <p>
 * 「已准备」是这条聚合存在的全部理由——它必须<b>对被许愿人保密</b>：
 * 偷偷准备的东西一旦提前泄露就没惊喜了，所以 {@link #visibleStatusFor} 与
 * {@link #keepsPreparationSecretFrom} 是领域规则，不是 Controller 的显示开关。
 * 状态单向推进：OPEN → PREPARED（可撤销回 OPEN）→ FULFILLED，实现之后不可回退。
 */
public final class Wish {

    public static final int TITLE_MAX = 80;
    public static final int NOTE_MAX = 200;

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_PREPARED = "PREPARED";
    public static final String STATUS_FULFILLED = "FULFILLED";

    private final String id;
    private final String ownerUser;
    private final String creatorUser;
    private final String title;
    private String note;
    private String status;
    private String preparedBy;
    private Long preparedAt;
    private Long fulfilledAt;

    private Wish(String id, String ownerUser, String creatorUser, String title, String note, String status,
                 String preparedBy, Long preparedAt, Long fulfilledAt) {
        this.id = id;
        this.ownerUser = ownerUser;
        this.creatorUser = creatorUser;
        this.title = title;
        this.note = note;
        this.status = status;
        this.preparedBy = preparedBy;
        this.preparedAt = preparedAt;
        this.fulfilledAt = fulfilledAt;
    }

    /** 新记一条愿望：标题必填不超长，许愿人默认是记录人本人。 */
    public static Wish add(String ownerUser, String creatorUser, String title, String note) {
        String text = title == null ? "" : title.trim();
        if (text.isEmpty()) {
            throw new RuleViolation("想要什么总得写一句呀");
        }
        if (text.length() > TITLE_MAX) {
            throw new RuleViolation("愿望最多 " + TITLE_MAX + " 个字，剩下的见面再说");
        }
        String extra = note == null ? null : note.trim();
        if (extra != null && extra.isEmpty()) {
            extra = null;
        }
        if (extra != null && extra.length() > NOTE_MAX) {
            throw new RuleViolation("补充说明最多 " + NOTE_MAX + " 个字");
        }
        String owner = ownerUser == null || ownerUser.isBlank() ? creatorUser : ownerUser.trim();
        return new Wish(null, owner, creatorUser, text, extra, STATUS_OPEN, null, null, null);
    }

    public static Wish restore(String id, String ownerUser, String creatorUser, String title, String note,
                               String status, String preparedBy, Long preparedAt, Long fulfilledAt) {
        return new Wish(id, ownerUser, creatorUser, title, note, status, preparedBy, preparedAt, fulfilledAt);
    }

    /** 偷偷标记「已准备」：只有被许愿的那一位能标，且实现过的愿望不再改。 */
    public void prepareBy(String me, long at) {
        // 顺序即隐私：先过归属闸门，再谈状态。否则许愿人重复点一次就会收到
        // 「已经标过『已准备』了」——这句话本身把惊喜说出去了（本地真后端探针实测到）。
        requireCounterpartCanTouchPreparation(me);
        if (STATUS_FULFILLED.equals(status)) {
            throw new RuleViolation("这个愿望已经实现啦");
        }
        if (STATUS_PREPARED.equals(status)) {
            throw new RuleViolation("已经标过「已准备」了，别再点一次");
        }
        this.status = STATUS_PREPARED;
        this.preparedBy = me;
        this.preparedAt = at;
    }

    /** 撤销「已准备」：只有当初点的那个人能撤，别人撤等于偷看别人的进度。 */
    public void unprepareBy(String me) {
        requireCounterpartCanTouchPreparation(me);
        if (!STATUS_PREPARED.equals(status)) {
            throw new RuleViolation("这条愿望没被标记过「已准备」");
        }
        if (preparedBy == null || !preparedBy.equals(me)) {
            throw new RuleViolation("「已准备」是谁标的，就只能由谁撤掉");
        }
        this.status = STATUS_OPEN;
        this.preparedBy = null;
        this.preparedAt = null;
    }

    /** 确认实现：只有许愿人本人能说「我收到了」，实现之后不可回退。 */
    public void fulfillBy(String me, long at) {
        if (STATUS_FULFILLED.equals(status)) {
            throw new RuleViolation("这个愿望已经实现啦，不用再点一次");
        }
        if (!ownerUser.equals(me)) {
            throw new RuleViolation("只有许愿的人自己能确认愿望实现了");
        }
        this.status = STATUS_FULFILLED;
        this.fulfilledAt = at;
    }

    /** 删除：谁记下来的谁删。 */
    public void requireDeletableBy(String me) {
        if (STATUS_FULFILLED.equals(status)) {
            throw new RuleViolation("已经实现的愿望要留在记录里，删不掉咯");
        }
        if (!creatorUser.equals(me)) {
            throw new RuleViolation("只有记这条愿望的人能删掉它");
        }
    }

    /** 改写补充说明：只有记录人能改，改完仍是那条愿望。 */
    public void editNote(String me, String note) {
        if (!creatorUser.equals(me)) {
            throw new RuleViolation("只有记这条愿望的人能改它");
        }
        String extra = note == null ? null : note.trim();
        if (extra != null && extra.isEmpty()) {
            extra = null;
        }
        if (extra != null && extra.length() > NOTE_MAX) {
            throw new RuleViolation("补充说明最多 " + NOTE_MAX + " 个字");
        }
        this.note = extra;
    }

    /**
     * 站在某人视角看到的状态：愿望人本人看不到「已准备」，只看到 OPEN。
     * 这是「偷偷」的全部实现——VO 拼装必须走这个方法，不许直接读 {@link #status()}。
     */
    public String visibleStatusFor(String me) {
        if (STATUS_PREPARED.equals(status) && keepsPreparationSecretFrom(me)) {
            return STATUS_OPEN;
        }
        return status;
    }

    /** 这条「已准备」是不是要对某人保密：只对许愿人本人保密，标记人自己当然知道。 */
    public boolean keepsPreparationSecretFrom(String me) {
        return STATUS_PREPARED.equals(status) && ownerUser.equals(me);
    }

    /**
     * 「已准备」这一格只有对方能碰。许愿人自己来动时必须在这一层就被挡下，
     * 而且话术不能透露当前状态——否则等于告诉她「有人准备了」。
     */
    private void requireCounterpartCanTouchPreparation(String me) {
        if (ownerUser.equals(me)) {
            throw new RuleViolation("这条愿望是你自己许的，「已准备」那一格是给 TA 留的");
        }
    }

    public boolean preparedFlag() {
        return STATUS_PREPARED.equals(status);
    }

    public boolean fulfilledFlag() {
        return STATUS_FULFILLED.equals(status);
    }

    public String id() {
        return id;
    }

    public String ownerUser() {
        return ownerUser;
    }

    public String creatorUser() {
        return creatorUser;
    }

    public String title() {
        return title;
    }

    public String note() {
        return note;
    }

    /** 真状态（持久化与归属判定用）；对外回显一律走 {@link #visibleStatusFor}。 */
    public String status() {
        return status;
    }

    public String preparedBy() {
        return preparedBy;
    }

    public Long preparedAt() {
        return preparedAt;
    }

    public Long fulfilledAt() {
        return fulfilledAt;
    }
}
