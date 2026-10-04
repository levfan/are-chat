package com.smart.chat.identity.domain.registration;

import com.smart.chat.identity.domain.RuleViolation;
import com.smart.chat.identity.domain.account.AccountRules;

import java.util.UUID;

/**
 * 注册申请聚合根：审批制的入会申请（{@code CONTEXT.md} 的「注册申请」）。
 * <p>
 * 两条产品口径由它守死：
 * <ul>
 *   <li><b>注册本身不产生登录态</b>——申请阶段只写这张表，一个账号都不会有；
 *       管理员点「通过」之后才由 {@code AppUserService.createAccount} 建号；</li>
 *   <li><b>一份申请只能被处理一次</b>——{@code PENDING → APPROVED/REJECTED} 是单向迁移，
 *       再点一次就按「该申请已处理过（X）」拒绝（409）。这条判定过去散在 approve 与 reject 两个方法里，
 *       所以才会出现「许愿人重复点已准备」同类的漏网（见 {@code docs/adr/0007}）。</li>
 * </ul>
 * 密码以 {@code PasswordHasher} 编码后的哈希存在申请行里，审批通过时原样搬进账号——聚合只当它是opaque串。
 */
public final class RegistrationApplication {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    /** 拒绝原因列宽（varchar(200)），超出按前 200 字截断——与改造前一致 */
    public static final int REASON_MAX = 200;

    private final String id;
    private final String phone;
    private final String username;
    private final String passwordHash;
    private final Long created;
    private final String nickname;
    private String status;
    private String rejectReason;
    private Long reviewedAt;
    private String reviewedBy;

    private RegistrationApplication(String id, String phone, String username, String nickname, String passwordHash,
                                    String status, String rejectReason, Long created, Long reviewedAt,
                                    String reviewedBy) {
        this.id = id;
        this.phone = phone;
        this.username = username;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.status = status;
        this.rejectReason = rejectReason;
        this.created = created;
        this.reviewedAt = reviewedAt;
        this.reviewedBy = reviewedBy;
    }

    /**
     * 提交申请：进待审队列。格式校验（手机号/用户名/密码/昵称）由用例在调用前做完，
     * 这里只负责「新申请一定是 PENDING」这条不变式。
     */
    public static RegistrationApplication submit(String phone, String username, String nickname, String passwordHash) {
        return new RegistrationApplication(UUID.randomUUID().toString(), phone, username, nickname, passwordHash,
                STATUS_PENDING, null, System.currentTimeMillis(), null, null);
    }

    /** 从存储重建：历史申请可能没有昵称、状态也可能是空，一律原样读出来 */
    public static RegistrationApplication restore(String id, String phone, String username, String nickname,
                                                 String passwordHash, String status, String rejectReason, Long created,
                                                 Long reviewedAt, String reviewedBy) {
        return new RegistrationApplication(id, phone, username, nickname, passwordHash, status, rejectReason, created,
                reviewedAt, reviewedBy);
    }

    // ===== 状态迁移 =====

    /** 通过：落审批时间与审批人 */
    public void approve(String reviewer, long at) {
        requirePending();
        this.status = STATUS_APPROVED;
        this.reviewedAt = at;
        this.reviewedBy = reviewer;
    }

    /**
     * 拒绝：原因去空格、按列宽截断；没填原因也照样成立（审计里写成「未填原因」，由 Service 拼装）。
     */
    public void reject(String reviewer, String reason, long at) {
        requirePending();
        this.status = STATUS_REJECTED;
        this.rejectReason = cleanReason(reason);
        this.reviewedAt = at;
        this.reviewedBy = reviewer;
    }

    private void requirePending() {
        if (!STATUS_PENDING.equals(status)) {
            throw new RuleViolation(RuleViolation.CONFLICT, "该申请已处理过（" + status + "）");
        }
    }

    /** 原因清洗：null 视作空串，去空格，超过 {@link #REASON_MAX} 个字符就截 */
    private static String cleanReason(String reason) {
        String clean = reason == null ? "" : reason.trim();
        return clean.length() > REASON_MAX ? clean.substring(0, REASON_MAX) : clean;
    }

    // ===== 查询辅助 =====

    public boolean pendingFlag() {
        return STATUS_PENDING.equals(status);
    }

    /** 手机号脱敏：与账号列表同一口径 */
    public String maskedPhone() {
        return AccountRules.maskPhone(phone);
    }

    // ===== 访问器 =====

    public String id() {
        return id;
    }

    public String phone() {
        return phone;
    }

    public String username() {
        return username;
    }

    /** 注册时填写的昵称，历史申请可能为 null */
    public String nickname() {
        return nickname;
    }

    /** 密码哈希：审批通过时原样搬进 app_user */
    public String passwordHash() {
        return passwordHash;
    }

    public String status() {
        return status;
    }

    public String rejectReason() {
        return rejectReason;
    }

    public Long created() {
        return created;
    }

    public Long reviewedAt() {
        return reviewedAt;
    }

    public String reviewedBy() {
        return reviewedBy;
    }
}
