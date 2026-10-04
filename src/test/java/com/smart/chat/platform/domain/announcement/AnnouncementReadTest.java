package com.smart.chat.platform.domain.announcement;

import com.smart.chat.platform.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 已读记录是薄实体：只验「确认已读」的工厂口径与缺失公告闸门，不给它编造行为。 */
class AnnouncementReadTest {

    @Test
    void confirmIssuesIdAndReadAt() {
        long before = System.currentTimeMillis();
        AnnouncementRead read = AnnouncementRead.confirm("alice", "ann-1");

        assertThat(read.id()).isNotBlank();
        assertThat(read.username()).isEqualTo("alice");
        assertThat(read.announcementId()).isEqualTo("ann-1");
        assertThat(read.readAt()).isGreaterThanOrEqualTo(before);
    }

    @Test
    void confirmNeedsAnAnnouncement() {
        assertThatThrownBy(() -> AnnouncementRead.confirm("alice", "  "))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("公告不存在");
    }

    @Test
    void restoreDoesNotValidate() {
        AnnouncementRead legacy = AnnouncementRead.restore("r1", "bob", "ann-2", null);

        assertThat(legacy.id()).isEqualTo("r1");
        assertThat(legacy.readAt()).as("read_at 为空按 0 看待").isZero();
    }
}
