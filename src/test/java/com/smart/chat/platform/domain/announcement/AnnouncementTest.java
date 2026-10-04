package com.smart.chat.platform.domain.announcement;

import com.smart.chat.platform.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 公告聚合的状态机与闸门：发布 → 关闭（单向），以及「同一时刻只有最新一条生效」。
 * 文案是产品口径（对外 400/404 的原话），逐字锁住。
 */
class AnnouncementTest {

    @Test
    void postTrimsContentAndStartsEnabled() {
        long before = System.currentTimeMillis();
        Announcement announcement = Announcement.post("  今晚八点维护  ", "admin");

        assertThat(announcement.id()).isNotBlank();
        assertThat(announcement.content()).isEqualTo("今晚八点维护");
        assertThat(announcement.createdBy()).isEqualTo("admin");
        assertThat(announcement.enabled()).isTrue();
        assertThat(announcement.created()).isGreaterThanOrEqualTo(before);
    }

    @Test
    void postRejectsBlankContent() {
        assertThatThrownBy(() -> Announcement.post("   ", "admin"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("公告内容不能为空");
        assertThatThrownBy(() -> Announcement.post(null, "admin"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("公告内容不能为空");
    }

    @Test
    void postRejectsTooLongContent() {
        String tooLong = "啊".repeat(Announcement.CONTENT_MAX + 1);

        assertThatThrownBy(() -> Announcement.post(tooLong, "admin"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("公告最长 500 个字");
        assertThatCode(() -> Announcement.post("啊".repeat(Announcement.CONTENT_MAX), "admin"))
                .doesNotThrowAnyException();
    }

    @Test
    void closeMovesEnabledToDisabledInOneDirection() {
        Announcement announcement = Announcement.post("公告", "admin");

        announcement.close();

        assertThat(announcement.enabled()).isFalse();
        // 状态只能停在停用：现役路由里没有「重新启用」，聚合也不提供把它变回生效的方法
    }

    @Test
    void closingTwiceIsIdempotentAndStaysSilent() {
        Announcement announcement = Announcement.restore("a1", "公告", "admin", true, 1L);

        announcement.close();
        // 管理员多点一次「关闭」不能变成失败——改造前就是这个口径，故意不设闸门
        assertThatCode(announcement::close).doesNotThrowAnyException();
        assertThat(announcement.enabled()).isFalse();
    }

    @Test
    void restoreAcceptsLegacyRowsWithoutValidating() {
        Announcement legacy = Announcement.restore("a2", null, null, null, null);

        assertThat(legacy.id()).isEqualTo("a2");
        assertThat(legacy.content()).isNull();
        assertThat(legacy.enabled()).as("enabled 为空按未生效看待").isFalse();
        assertThat(legacy.created()).isZero();
    }

    @Test
    void supersedeClosesOnlyTheStillEnabledOnesAndReturnsThem() {
        Announcement fresh = Announcement.post("新公告", "admin");
        Announcement active = Announcement.restore("old-1", "旧的一", "admin", true, 1L);
        Announcement alreadyClosed = Announcement.restore("old-2", "旧的二", "admin", false, 2L);
        List<Announcement> window = new ArrayList<>(List.of(active, alreadyClosed));

        List<Announcement> superseded = fresh.supersede(window);

        assertThat(superseded).containsExactly(active);
        assertThat(active.enabled()).isFalse();
        assertThat(alreadyClosed.enabled()).isFalse();
    }

    @Test
    void supersedeOnEmptyWindowReturnsNothing() {
        Announcement fresh = Announcement.post("第一条公告", "admin");

        assertThat(fresh.supersede(List.of())).isEmpty();
        assertThat(fresh.enabled()).isTrue();
    }

    @Test
    void supersededListIsTheWholeWindowWhenAllEnabled() {
        Announcement fresh = Announcement.post("新公告", "admin");
        List<Announcement> window = new ArrayList<>(List.of(
                Announcement.restore("a", "一", "admin", true, 1L),
                Announcement.restore("b", "二", "admin", true, 2L)));

        assertThat(fresh.supersede(window)).hasSize(2)
                .allSatisfy(one -> assertThat(one.enabled()).isFalse());
    }
}
