package com.smart.chat.couple.domain.pin;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 收藏卡聚合：清洗、去重、六枚上限与键长闸门都锁在这里，
 * 「读存量不校验」也要能读出历史上的脏行。
 */
class UserPinTest {

    @Test
    void trimsDedupesAndKeepsClickOrder() {
        UserPin pin = UserPin.blank("s1", "alice");
        pin.replaceWith(Arrays.asList(" meeting ", "meeting", "", null, "  host  "));

        assertThat(pin.keys()).containsExactly("meeting", "host");
        assertThat(pin.joined()).isEqualTo("meeting,host");
        assertThat(pin.updatedAt()).isNotNull();
    }

    @Test
    void sixPinsAreAcceptedButSeventhIsNot() {
        UserPin pin = UserPin.blank("s1", "alice");
        pin.replaceWith(List.of("a", "b", "c", "d", "e", "f"));
        assertThat(pin.keys()).hasSize(6);

        assertThatThrownBy(() -> pin.replaceWith(List.of("a", "b", "c", "d", "e", "f", "g")))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("最多收藏 6 个，先放下一个再钉新的");
        assertThat(pin.keys()).as("被拒的那次不改写卡集，也不留痕").containsExactly("a", "b", "c", "d", "e", "f");
    }

    @Test
    void overLongKeyIsRejected() {
        UserPin pin = UserPin.blank("s1", "alice");
        assertThatThrownBy(() -> pin.replaceWith(List.of("x".repeat(41))))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("收藏键太长啦");
        assertThat("x".repeat(40)).as("恰好 40 字符是合法的").hasSize(UserPin.KEY_MAX);
    }

    @Test
    void restoringDirtyRowDoesNotValidate() {
        UserPin pin = UserPin.restore("p1", "s1", "alice", " a , ,b ", 7L, null);

        assertThat(pin.id()).isEqualTo("p1");
        assertThat(pin.keys()).containsExactly("a", "b");
        assertThat(pin.created()).isEqualTo(7L);
        assertThat(UserPin.restore("p2", "s1", "bob", null, null, null).keys()).isEmpty();
    }

    @Test
    void blankBoardSavesAsEmptyRow() {
        UserPin pin = UserPin.blank("s1", "alice");

        assertThat(pin.keys()).isEmpty();
        assertThat(pin.joined()).isEmpty();
    }
}
