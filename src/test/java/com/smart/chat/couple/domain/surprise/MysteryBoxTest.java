package com.smart.chat.couple.domain.surprise;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 恋爱盲盒聚合的单测：装盒四道闸（类型、内容空、内容超长、开箱日）、
 * 到日才可拆、装盒人不能自拆、拆过不再推第二遍，以及内容在拆开前的保密回显。
 */
class MysteryBoxTest {

    private static final LocalDate TODAY = LocalDate.now();

    private static MysteryBox pack(String kind, String content, String openDay) {
        return MysteryBox.pack("alice", kind, content, openDay, TODAY).intoSpace("s1");
    }

    @Test
    void packKeepsOnlyWhisperAndTask() {
        assertThat(MysteryBox.isValidKind(MysteryBox.KIND_WHISPER)).isTrue();
        assertThat(MysteryBox.isValidKind(MysteryBox.KIND_TASK)).isTrue();
        assertThat(MysteryBox.isValidKind("gift")).isFalse();
        assertThatThrownBy(() -> pack("gift", "喜欢你", TODAY.plusDays(1).toString()))
                .isInstanceOf(RuleViolation.class).hasMessage("盲盒只能是悄悄话或小任务哦");
    }

    @Test
    void packTrimsContentButMeasuresWhatTheUserTyped() {
        MysteryBox box = pack(MysteryBox.KIND_WHISPER, "  喜欢你  ", TODAY.plusDays(1).toString());
        assertThat(box.content()).isEqualTo("喜欢你");
        assertThat(box.openDay()).isEqualTo(TODAY.plusDays(1).toString());
        assertThat(box.opened()).isFalse();
        assertThat(box.spaceId()).isEqualTo("s1");

        assertThatThrownBy(() -> pack(MysteryBox.KIND_WHISPER, "   ", TODAY.plusDays(1).toString()))
                .isInstanceOf(RuleViolation.class).hasMessage("盒子里总要放点什么吧～");
        assertThatThrownBy(() -> pack(MysteryBox.KIND_WHISPER, null, TODAY.plusDays(1).toString()))
                .isInstanceOf(RuleViolation.class).hasMessage("盒子里总要放点什么吧～");
        // 超长量的是原样：前后空格也占位
        assertThatThrownBy(() -> pack(MysteryBox.KIND_WHISPER, " " + "字".repeat(MysteryBox.CONTENT_MAX),
                TODAY.plusDays(1).toString()))
                .isInstanceOf(RuleViolation.class).hasMessage("盒子太小啦，最多装 300 个字");
    }

    @Test
    void openDayMustBeADateAndNoSoonerThanTomorrow() {
        assertThatThrownBy(() -> pack(MysteryBox.KIND_TASK, "一起散步", "下周三"))
                .isInstanceOf(RuleViolation.class).hasMessage("开箱日期不认识，选一个明天以后的日子吧");
        assertThatThrownBy(() -> pack(MysteryBox.KIND_TASK, "一起散步", null))
                .isInstanceOf(RuleViolation.class).hasMessage("开箱日期不认识，选一个明天以后的日子吧");
        assertThatThrownBy(() -> pack(MysteryBox.KIND_TASK, "一起散步", TODAY.toString()))
                .isInstanceOf(RuleViolation.class).hasMessage("盲盒最早明天才能拆哦，期待感要留足 ✨");
        assertThatThrownBy(() -> pack(MysteryBox.KIND_TASK, "一起散步", TODAY.minusDays(1).toString()))
                .isInstanceOf(RuleViolation.class).hasMessage("盲盒最早明天才能拆哦，期待感要留足 ✨");
    }

    @Test
    void sealerCannotOpenItHimselfEvenAfterTheDayArrives() {
        MysteryBox box = MysteryBox.restore("b1", "s1", "alice", MysteryBox.KIND_WHISPER, "喜欢你",
                TODAY.toString(), false, null, 1L);
        assertThatThrownBy(() -> box.openBy("alice", TODAY))
                .isInstanceOfSatisfying(RuleViolation.class, e -> {
                    assertThat(e.getMessage()).isEqualTo("自己装的盒子自己拆，就不惊喜了呀 😝");
                    assertThat(e.status()).isEqualTo(403);
                });
    }

    @Test
    void waitingForTheOpenDayIsCountedIntoTheMessage() {
        MysteryBox box = MysteryBox.restore("b1", "s1", "alice", MysteryBox.KIND_TASK, "一起散步",
                TODAY.plusDays(3).toString(), false, null, 1L);
        assertThatThrownBy(() -> box.openBy("bob", TODAY))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("还没到开箱日！再等 3 天，让期待多飞一会儿 🎈");
        assertThat(box.openBy("bob", TODAY.plusDays(3))).isTrue();
        assertThat(box.opened()).isTrue();
        assertThat(box.openedAt()).isNotNull();
        // 拆过再点：不报错也不推第二遍
        assertThat(box.openBy("bob", TODAY.plusDays(4))).isFalse();
    }

    @Test
    void contentIsVisibleToTheReceiverOnlyAfterItIsOpenedOrDue() {
        MysteryBox future = MysteryBox.restore("b1", "s1", "alice", MysteryBox.KIND_WHISPER, "喜欢你",
                TODAY.plusDays(2).toString(), false, null, 1L);
        assertThat(future.visibleContentFor("bob", TODAY)).isNull();
        assertThat(future.visibleContentFor("alice", TODAY)).isEqualTo("喜欢你");
        assertThat(future.openableBy("bob", TODAY)).isFalse();
        assertThat(future.openableBy("alice", TODAY)).isFalse();
        assertThat(future.openDate()).isEqualTo(TODAY.plusDays(2));

        MysteryBox due = MysteryBox.restore("b2", "s1", "alice", MysteryBox.KIND_WHISPER, "喜欢你",
                TODAY.toString(), false, null, 1L);
        assertThat(due.visibleContentFor("bob", TODAY)).isEqualTo("喜欢你");
        assertThat(due.openableBy("bob", TODAY)).isTrue();
        assertThat(due.openableBy("alice", TODAY)).isFalse();
    }

    @Test
    void restoreKeepsLegacyRowAsIs() {
        MysteryBox legacy = MysteryBox.restore("b3", "s1", "bob", MysteryBox.KIND_TASK, "老盒子",
                "2026-01-01", true, 9L, null);
        assertThat(legacy.opened()).isTrue();
        assertThat(legacy.openedAt()).isEqualTo(9L);
        assertThat(legacy.created()).isZero();
        assertThat(legacy.kind()).isEqualTo(MysteryBox.KIND_TASK);
    }
}
