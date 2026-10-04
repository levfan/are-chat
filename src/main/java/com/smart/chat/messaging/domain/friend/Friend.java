package com.smart.chat.messaging.domain.friend;

import com.smart.chat.messaging.domain.RuleViolation;

import java.util.UUID;

/**
 * 好友关系的一条边：{@code owner} 这一侧的视角（同一对人是两行、两条边，见 CONTEXT.md「好友」）。
 * <p>
 * 这条聚合存在的理由是「边上的归属与私设」：备注/分组/置顶/免打扰/拉黑都只属于拥有者那一侧，
 * 对方的那条边一个字也不该被改动；删除必须成对（{@link FriendRepository#deletePair}），
 * 只删一边就会留下半条关系。这些裁决放在这里，而不是散在 Service 的 if 里。
 * <p>
 * {@code lastSeenAt} 是<b>只读列</b>：它由 {@link FriendRepository#touchLastSeenOf} 按人批量刷新
 * （WS 上下线走的是那条 UPDATE），聚合读它是为了联系人列表显示「x 分钟前在线」，
 * 但 {@code save} 不回写它——否则内存里的 null 会把别人刚刷新的时间清掉。
 */
public final class Friend {

    public static final int REMARK_MAX = 32;
    public static final int TAG_MAX = 16;

    private final String id;
    private final String ownerUsername;
    private final String friendUsername;
    private final Long created;
    private final Long lastSeenAt;
    private String remark;
    private String tag;
    private Boolean pinned;
    private Boolean muted;
    private Integer blocked;
    private Long lastReadAt;

    private Friend(String id, String ownerUsername, String friendUsername, String remark, String tag,
                   Boolean pinned, Boolean muted, Integer blocked, Long lastReadAt, Long lastSeenAt, Long created) {
        this.id = id;
        this.ownerUsername = ownerUsername;
        this.friendUsername = friendUsername;
        this.remark = remark;
        this.tag = tag;
        this.pinned = pinned;
        this.muted = muted;
        this.blocked = blocked;
        this.lastReadAt = lastReadAt;
        this.lastSeenAt = lastSeenAt;
        this.created = created;
    }

    /**
     * 建立一条边（同意后两边各建一条）：默认未置顶、未免打扰、未读阈值为 0。
     * 口径与改造前的 {@code FriendPO.of} 逐字一致，只是把取时间交给调用方，便于用例断言。
     */
    public static Friend add(String owner, String friend, long at) {
        return new Friend(UUID.randomUUID().toString(), owner, friend, "", null,
                Boolean.FALSE, Boolean.FALSE, null, 0L, null, at);
    }

    /** 从存储重建：不校验，历史行里的空值必须读得出来。 */
    public static Friend restore(String id, String ownerUsername, String friendUsername, String remark, String tag,
                                 Boolean pinned, Boolean muted, Integer blocked, Long lastReadAt,
                                 Long lastSeenAt, Long created) {
        return new Friend(id, ownerUsername, friendUsername, remark, tag, pinned, muted, blocked,
                lastReadAt, lastSeenAt, created);
    }

    /** 这条边是不是 {@code me} 自己的；不是就当不存在（口径沿用改造前的 404 文案）。 */
    public void requireOwnedBy(String me) {
        if (!ownerUsername.equals(me)) {
            throw RuleViolation.notFound("好友不存在");
        }
    }

    /** 改备注：只有拥有者改自己那条边，最长 32 字。 */
    public void changeRemark(String remark) {
        String trimmed = remark.trim();
        if (trimmed.length() > REMARK_MAX) {
            throw RuleViolation.of("备注最长 " + REMARK_MAX + " 个字");
        }
        this.remark = trimmed;
    }

    /** 改分组标签，最长 16 字。 */
    public void changeTag(String tag) {
        String trimmed = tag.trim();
        if (trimmed.length() > TAG_MAX) {
            throw RuleViolation.of("分组标签最长 " + TAG_MAX + " 个字");
        }
        this.tag = trimmed;
    }

    public void changePinned(boolean pinned) {
        this.pinned = pinned;
    }

    public void changeMuted(boolean muted) {
        this.muted = muted;
    }

    /** 拉黑/解除拉黑：落的是 1/0，口径与 private_message 那边「Integer 值判拉黑」保持一致。 */
    public void changeBlocked(boolean blocked) {
        this.blocked = blocked ? 1 : 0;
    }

    /** 已读游标：会话页打开时推进，未读红点就是按它算的。 */
    public void markReadAt(long at) {
        this.lastReadAt = at;
    }

    public boolean pinnedFlag() {
        return Boolean.TRUE.equals(pinned);
    }

    public boolean mutedFlag() {
        return Boolean.TRUE.equals(muted);
    }

    /** 是否处于拉黑位（1 才算，null/0 都是未拉黑）。 */
    public boolean blockedFlag() {
        return Integer.valueOf(1).equals(blocked);
    }

    public String id() {
        return id;
    }

    public String ownerUsername() {
        return ownerUsername;
    }

    public String friendUsername() {
        return friendUsername;
    }

    public String remark() {
        return remark;
    }

    public String tag() {
        return tag;
    }

    public Boolean pinned() {
        return pinned;
    }

    public Boolean muted() {
        return muted;
    }

    /** 拉黑位的原始值（1/0/null）：存储契约列，判定请用 {@link #blockedFlag()}。 */
    public Integer blocked() {
        return blocked;
    }

    public Long lastReadAt() {
        return lastReadAt;
    }

    /** 只读列，见类注释。 */
    public Long lastSeenAt() {
        return lastSeenAt;
    }

    public Long created() {
        return created;
    }
}
