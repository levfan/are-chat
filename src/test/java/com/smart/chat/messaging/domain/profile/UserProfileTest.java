package com.smart.chat.messaging.domain.profile;

import com.smart.chat.messaging.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 资料卡每个字段自己的取值范围 + F42 生日的剩余天数口径。 */
class UserProfileTest {

    private static UserProfile profile() {
        return UserProfile.restore("alice", "alice", "", "c0", "online", null, 1L);
    }

    @Test
    void provisioningUsesTheSameDefaultsAsBefore() {
        UserProfile fresh = UserProfile.provision("bob", "bob", "c0", 9L);

        assertThat(fresh.username()).isEqualTo("bob");
        assertThat(fresh.nickname()).isEqualTo("bob");
        assertThat(fresh.signature()).isEmpty();
        assertThat(fresh.avatar()).isEqualTo("c0");
        assertThat(fresh.presenceStatus()).isEqualTo("online");
        assertThat(fresh.birthday()).isNull();
        assertThat(fresh.updatedAt()).isEqualTo(9L);
    }

    @Test
    void nicknameIsOneToThirtyTwoCharactersAfterTrim() {
        UserProfile p = profile();

        p.changeNickname("  新昵称  ");
        assertThat(p.nickname()).isEqualTo("新昵称");

        assertThatThrownBy(() -> p.changeNickname("   "))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("昵称需为 1~32 个字")
                .extracting(e -> ((RuleViolation) e).code()).isEqualTo(400);
        assertThatThrownBy(() -> p.changeNickname("x".repeat(33)))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("昵称需为 1~32 个字");
    }

    @Test
    void signatureMayBeEmptiedButNotOversized() {
        UserProfile p = profile();

        p.changeSignature("  ");
        assertThat(p.signature()).isEmpty();

        assertThatThrownBy(() -> p.changeSignature("x".repeat(101)))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("签名最长 100 个字");
    }

    @Test
    void avatarOnlyAcceptsPresetColorSlots() {
        UserProfile p = profile();

        p.changeAvatar(" c7 ");
        assertThat(p.avatar()).isEqualTo("c7");

        assertThatThrownBy(() -> p.changeAvatar("https://evil/a.png"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("头像请从预设色档中选择");
    }

    @Test
    void presenceOnlyHasThreeStates() {
        UserProfile p = profile();

        p.changePresenceStatus("busy");
        assertThat(p.presenceStatus()).isEqualTo("busy");

        assertThatThrownBy(() -> p.changePresenceStatus("sleeping"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("在线状态仅支持 online / busy / away");
    }

    @Test
    void unfilledPresenceDisplaysAsOnline() {
        UserProfile legacy = UserProfile.restore("bob", "bob", "", "c0", null, null, 1L);

        assertThat(legacy.presenceStatus()).isNull();
        assertThat(legacy.presenceStatusForView()).isEqualTo("online");

        legacy.fillPresenceStatusIfMissing();
        assertThat(legacy.presenceStatus()).isEqualTo("online");
    }

    @Test
    void birthdayAcceptsBothShapesAndBlankClearsIt() {
        UserProfile p = profile();

        p.changeBirthday("1990-01-02");
        assertThat(p.birthday()).isEqualTo("1990-01-02");
        p.changeBirthday("01-02");
        assertThat(p.birthday()).isEqualTo("01-02");
        p.changeBirthday("   ");
        assertThat(p.birthday()).isNull();

        assertThatThrownBy(() -> p.changeBirthday("1-2"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("生日格式应为 yyyy-MM-dd 或 MM-dd");
        assertThatThrownBy(() -> p.changeBirthday("1990-13-01"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("生日格式应为 yyyy-MM-dd 或 MM-dd");
    }

    @Test
    void daysUntilBirthdayRollsForwardOnceTheDateHasPassed() {
        LocalDate today = LocalDate.of(2026, 10, 5);

        assertThat(profile().daysUntilBirthday(today)).isEqualTo(OptionalLong.empty());

        UserProfile birthdayToday = profile();
        birthdayToday.changeBirthday("2000-10-05");
        assertThat(birthdayToday.daysUntilBirthday(today)).hasValue(0L);

        UserProfile birthdayTomorrow = profile();
        birthdayTomorrow.changeBirthday("10-06");
        assertThat(birthdayTomorrow.daysUntilBirthday(today)).hasValue(1L);

        UserProfile alreadyPassed = profile();
        alreadyPassed.changeBirthday("01-01");
        assertThat(alreadyPassed.daysUntilBirthday(today)).hasValue(88L);
    }

    @Test
    void illegibleBirthdayIsSkippedInsteadOfBlowingTheListUp() {
        // 存量行里可能有校验收紧之前写进去的怪值：列表必须静默跳过它，而不是整页 500
        UserProfile legacy = UserProfile.restore("bob", "bob", "", "c0", "online", "随便写的", 1L);

        assertThat(legacy.daysUntilBirthday(LocalDate.of(2026, 10, 5))).isEqualTo(OptionalLong.empty());

        // 闰日填法在平年按 SMART 口径落到 2 月 28 日（改造前后同一套解析，不新增异常）
        UserProfile leapDay = profile();
        leapDay.changeBirthday("02-29");
        assertThat(leapDay.daysUntilBirthday(LocalDate.of(2026, 10, 5))).hasValue(146L);
    }
}
