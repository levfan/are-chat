package com.smart.chat.identity.domain.registration;

import com.smart.chat.identity.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 注册申请状态机：一份申请只能被处理一次，第二次点要当场被拦（409），
 * 并且拒绝原因按列宽截断。这条闸门过去在 approve 与 reject 各写一遍，
 * 搬进聚合后只剩一处——「重复处理」的行为因此不可能在两处漂移。
 */
class RegistrationApplicationTest {

    private RegistrationApplication pending() {
        return RegistrationApplication.submit("13900001111", "zhangsan", "张三", "hash");
    }

    @Test
    void submitStartsPendingWithNoReviewTrace() {
        RegistrationApplication application = pending();

        assertThat(application.id()).isNotBlank();
        assertThat(application.status()).isEqualTo(RegistrationApplication.STATUS_PENDING);
        assertThat(application.pendingFlag()).isTrue();
        assertThat(application.rejectReason()).isNull();
        assertThat(application.reviewedAt()).isNull();
        assertThat(application.reviewedBy()).isNull();
        assertThat(application.created()).isNotNull();
    }

    @Test
    void approveRecordsReviewerAndTime() {
        RegistrationApplication application = pending();

        application.approve("admin", 2000L);

        assertThat(application.status()).isEqualTo(RegistrationApplication.STATUS_APPROVED);
        assertThat(application.reviewedAt()).isEqualTo(2000L);
        assertThat(application.reviewedBy()).isEqualTo("admin");
        assertThat(application.pendingFlag()).isFalse();
    }

    @Test
    void rejectTrimsAndCapsReasonAtColumnWidth() {
        RegistrationApplication application = pending();

        application.reject("admin", "  资料不全  ", 3000L);

        assertThat(application.status()).isEqualTo(RegistrationApplication.STATUS_REJECTED);
        assertThat(application.rejectReason()).isEqualTo("资料不全");

        RegistrationApplication longReason = pending();
        longReason.reject("admin", "说".repeat(RegistrationApplication.REASON_MAX + 30), 3000L);
        assertThat(longReason.rejectReason()).hasSize(RegistrationApplication.REASON_MAX);

        RegistrationApplication noReason = pending();
        noReason.reject("admin", null, 3000L);
        assertThat(noReason.rejectReason()).isEmpty();
    }

    @Test
    void processedApplicationCannotBeProcessedAgainInEitherDirection() {
        RegistrationApplication approved = pending();
        approved.approve("admin", 1L);

        assertThatThrownBy(() -> approved.approve("admin", 2L))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("该申请已处理过（APPROVED）")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(409);
        // 通过了就不能再拒绝，反之也一样
        assertThatThrownBy(() -> approved.reject("admin", "改主意了", 2L))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("该申请已处理过（APPROVED）");

        RegistrationApplication rejected = pending();
        rejected.reject("admin", "资料不全", 1L);
        assertThatThrownBy(() -> rejected.approve("admin", 2L))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("该申请已处理过（REJECTED）");

        assertThat(approved.status()).as("被拒的第二次操作不得改坏已有结论")
                .isEqualTo(RegistrationApplication.STATUS_APPROVED);
        assertThat(approved.reviewedAt()).isEqualTo(1L);
    }

    @Test
    void restoreToleratesHistoricalRows() {
        // 历史申请可能没有昵称（昵称是后来才必填的），必须照样读得出来
        RegistrationApplication legacy = RegistrationApplication.restore("a1", "13800000001", "old", null,
                "hash", RegistrationApplication.STATUS_PENDING, null, 1L, null, null);

        assertThat(legacy.nickname()).isNull();
        assertThat(legacy.pendingFlag()).isTrue();
    }

    @Test
    void phoneIsMaskedForAdminViewsAndPushes() {
        assertThat(pending().maskedPhone()).isEqualTo("139****1111");
    }

    @Test
    void statusLiteralsMatchStoredValues() {
        assertThat(RegistrationApplication.STATUS_PENDING).isEqualTo("PENDING");
        assertThat(RegistrationApplication.STATUS_APPROVED).isEqualTo("APPROVED");
        assertThat(RegistrationApplication.STATUS_REJECTED).isEqualTo("REJECTED");
    }
}
