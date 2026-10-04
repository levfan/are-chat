package com.smart.chat.couple.domain.invite;

import com.smart.chat.couple.domain.RuleViolation;

import java.util.UUID;

/**
 * 情侣空间邀请：A 发起 → B 处理，一对用户之间同时只允许一条待处理邀请（方向任意）。
 * <p>
 * 状态机是单向的：PENDING → ACCEPTED / REJECTED / CANCELED，落了子状态就不能再动——
 * 「该邀请已经处理过了」这条口径原先在 accept/reject/cancel 三个方法里各写一遍，
 * 现在收敛到 {@link #requirePending} 一处，三个出口不可能再各自漂移。
 * 归属闸门（只能处理发给自己的、只能撤回自己发的）也在这里，因为它决定状态能不能推进。
 */
public final class Invite {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_CANCELED = "CANCELED";

    /** 邀请留言上限（F01） */
    public static final int MESSAGE_MAX = 100;

    private final String id;
    private final String fromUser;
    private final String toUser;
    private final String message;
    private final long created;
    private String status;
    private Long updatedAt;

    private Invite(String id, String fromUser, String toUser, String message, String status,
                   Long created, Long updatedAt) {
        this.id = id;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.message = message;
        this.status = status;
        this.created = created == null ? 0L : created;
        this.updatedAt = updatedAt;
    }

    /** 发起邀请：留言可空，超长当场拒（原话就是用户看到的那句） */
    public static Invite send(String fromUser, String toUser, String message) {
        String note = message == null ? "" : message.trim();
        if (note.length() > MESSAGE_MAX) {
            throw new RuleViolation("邀请留言最长 " + MESSAGE_MAX + " 个字");
        }
        return new Invite(UUID.randomUUID().toString(), fromUser, toUser,
                note.isEmpty() ? null : note, STATUS_PENDING, System.currentTimeMillis(), null);
    }

    /** 从存储还原 */
    public static Invite restore(String id, String fromUser, String toUser, String message, String status,
                                 Long created, Long updatedAt) {
        return new Invite(id, fromUser, toUser, message, status, created, updatedAt);
    }

    /** B 同意 */
    public void acceptBy(String me, long at) {
        requireHandler(me);
        status = STATUS_ACCEPTED;
        updatedAt = at;
    }

    /** B 婉拒 */
    public void rejectBy(String me, long at) {
        requireHandler(me);
        status = STATUS_REJECTED;
        updatedAt = at;
    }

    /** A 撤回自己发出的邀请 */
    public void cancelBy(String me, long at) {
        if (!fromUser.equals(me)) {
            throw RuleViolation.forbidden("只能撤回自己发出的邀请");
        }
        requirePending();
        status = STATUS_CANCELED;
        updatedAt = at;
    }

    /** 发给自己的邀请才能由自己处理，而且只能处理一次 */
    private void requireHandler(String me) {
        if (!toUser.equals(me)) {
            throw RuleViolation.forbidden("只能处理发给自己的邀请");
        }
        requirePending();
    }

    private void requirePending() {
        if (!STATUS_PENDING.equals(status)) {
            throw RuleViolation.conflict("该邀请已经处理过了");
        }
    }

    public boolean pending() {
        return STATUS_PENDING.equals(status);
    }

    public String id() {
        return id;
    }

    public String fromUser() {
        return fromUser;
    }

    public String toUser() {
        return toUser;
    }

    public String message() {
        return message;
    }

    public String status() {
        return status;
    }

    public long created() {
        return created;
    }

    public Long updatedAt() {
        return updatedAt;
    }
}
