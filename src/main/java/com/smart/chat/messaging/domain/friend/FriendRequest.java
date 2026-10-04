package com.smart.chat.messaging.domain.friend;

import com.smart.chat.messaging.domain.RuleViolation;

import java.util.UUID;

/**
 * 一条好友申请：{@code fromUser} 向 {@code toUser} 发出的入会请求，可附一句留言。
 * <p>
 * 状态机是这条聚合的全部难点：<b>只有接收方能处理</b>（同意/拒绝都是单方面裁决）、
 * <b>PENDING 之外不再可动</b>（处理过的申请重复点要撞 409），
 * 而「一人一票、不重复申请」的闸门要靠仓储的待处理查询（{@link FriendRequestRepository#findPendingBetween}）。
 * 状态字面量 PENDING/ACCEPTED/REJECTED 是数据库与前端在用的值，照 PO 原值写死。
 */
public final class FriendRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final int MESSAGE_MAX = 100;

    private final String id;
    private final String fromUser;
    private final String toUser;
    private final Long created;
    private String message;
    private String status;
    private Long updatedAt;

    private FriendRequest(String id, String fromUser, String toUser, String message, String status,
                          Long created, Long updatedAt) {
        this.id = id;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.message = message;
        this.status = status;
        this.created = created;
        this.updatedAt = updatedAt;
    }

    /** 发起申请：不能向自己发（这条边没有另一端），初始状态 PENDING。 */
    public static FriendRequest offer(String from, String to, long at) {
        if (from.equals(to)) {
            throw RuleViolation.of("不能添加自己为好友");
        }
        return new FriendRequest(UUID.randomUUID().toString(), from, to, null, STATUS_PENDING, at, null);
    }

    /** 从存储重建：不校验。 */
    public static FriendRequest restore(String id, String fromUser, String toUser, String message, String status,
                                        Long created, Long updatedAt) {
        return new FriendRequest(id, fromUser, toUser, message, status, created, updatedAt);
    }

    /**
     * 附一句留言：空白视为没写（改造前后口径一致——列里留 null，对外投影成空串）。
     * 超过 {@value #MESSAGE_MAX} 字直接拒。
     */
    public void attachMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String note = raw.trim();
        if (note.length() > MESSAGE_MAX) {
            throw RuleViolation.of("申请留言最长 " + MESSAGE_MAX + " 个字");
        }
        this.message = note;
    }

    /** 同意：只有接收方能同意，且只能在 PENDING 上同意一次。 */
    public void acceptBy(String me, long at) {
        requireRecipient(me);
        requirePending();
        this.status = STATUS_ACCEPTED;
        this.updatedAt = at;
    }

    /** 拒绝：同上，一锤子买卖。 */
    public void rejectBy(String me, long at) {
        requireRecipient(me);
        requirePending();
        this.status = STATUS_REJECTED;
        this.updatedAt = at;
    }

    public boolean pendingFlag() {
        return STATUS_PENDING.equals(status);
    }

    private void requireRecipient(String me) {
        if (!toUser.equals(me)) {
            throw RuleViolation.forbidden("只能处理发给自己的申请");
        }
    }

    private void requirePending() {
        if (!pendingFlag()) {
            throw RuleViolation.conflict("该申请已经处理过了");
        }
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

    public Long created() {
        return created;
    }

    public Long updatedAt() {
        return updatedAt;
    }
}
