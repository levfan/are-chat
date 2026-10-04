package com.smart.chat.couple.domain.comfort;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 求抱抱聚合的「整行」那一半：今天这一条怎么新建、同一天再说一次改写了什么、
 * 感受的中文目录（VO 与推送在用）以及「已被接住」为什么认 handled 标记而不是时刻。
 */
class ComfortRequestRowTest {

    @Test
    void askOnCarriesTheWholeRowAndStartsUnhandled() {
        ComfortRequest request = ComfortRequest.askOn("s1", "alice", "2026-10-05", "WRONGED");

        assertThat(request.id()).isNotBlank();
        assertThat(request.spaceId()).isEqualTo("s1");
        assertThat(request.by()).isEqualTo("alice");
        assertThat(request.day()).isEqualTo("2026-10-05");
        assertThat(request.created()).isNotNull();
        assertThat(request.handled()).isFalse();
        assertThat(request.raisedOn("2026-10-05")).isTrue();
        assertThat(request.raisedOn("2026-10-04")).isFalse();
    }

    @Test
    void askOnRejectsUnknownFeelingWithTheProductWording() {
        assertThatThrownBy(() -> ComfortRequest.askOn("s1", "alice", "2026-10-05", "HUNGRY"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("感受只能是 难过/委屈/累/焦虑/emo 哦");
    }

    @Test
    void reaskRewritesFeelingAndClearsTheHeldMark() {
        ComfortRequest request = ComfortRequest.restore("c1", "alice", "SAD", "抱抱过了", 5L, true,
                "s1", "2026-10-05", 1L);

        request.reask("TIRED");

        assertThat(request.feeling()).isEqualTo("TIRED");
        assertThat(request.handled()).as("重新开口就得重新被接").isFalse();
    }

    @Test
    void heldMarkComesFromTheRowNotFromTheTimestamp() {
        // 改写感受时 NOT_NULL 更新策略不会清空 handled_note / handled_at：库里会留上一次的残值
        ComfortRequest stale = ComfortRequest.restore("c2", "alice", "EMO", "上次的话", 9L, false,
                "s1", "2026-10-05", 1L);
        assertThat(stale.handled()).isFalse();
        assertThat(stale.isHeld()).as("时刻只是 payload，不能反推状态").isTrue();

        ComfortRequest held = ComfortRequest.restore("c3", "alice", "EMO", "接住了", 9L, true,
                "s1", "2026-10-05", 1L);
        assertThat(held.handled()).isTrue();
    }

    @Test
    void holdMarksTheRequestHandledAndKeepsTheTrimmedWords() {
        ComfortRequest request = ComfortRequest.askOn("s1", "bob", "2026-10-05", "SAD");

        request.hold("  我在，晚点聊  ", 777L);

        assertThat(request.handled()).isTrue();
        assertThat(request.note()).isEqualTo("我在，晚点聊");
        assertThat(request.heldAt()).isEqualTo(777L);
    }

    @Test
    void feelingCatalogMatchesTheFrontendDirectory() {
        assertThat(labelEmoji("SAD")).containsExactly("难过", "😢");
        assertThat(labelEmoji("WRONGED")).containsExactly("委屈", "🥺");
        assertThat(labelEmoji("TIRED")).containsExactly("好累", "😮‍💨");
        assertThat(labelEmoji("ANXIOUS")).containsExactly("焦虑", "😖");
        assertThat(labelEmoji("EMO")).containsExactly("emo", "🌧️");
        assertThat(labelEmoji("WHATEVER")).containsExactly("不太好", "🫂");
    }

    private java.util.List<String> labelEmoji(String feeling) {
        ComfortRequest request = ComfortRequest.restore("c9", "alice", feeling, null, null);
        return java.util.List.of(request.feelingLabel(), request.feelingEmoji());
    }
}
