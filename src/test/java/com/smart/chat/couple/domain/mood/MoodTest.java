package com.smart.chat.couple.domain.mood;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 心情日记薄实体：只锁现役读法用到的两条裁决——「谁在哪天记的」与「那天低不低落」。
 * 低落集合是 night-care 的推送口径，多一个少一个都会改变谁在深夜被打扰。
 */
class MoodTest {

    @Test
    void fourKeysCountAsDowncast() {
        assertThat(mood("alice", "2026-10-05", "SAD").isDowncast()).isTrue();
        assertThat(mood("alice", "2026-10-05", "ANGRY").isDowncast()).isTrue();
        assertThat(mood("alice", "2026-10-05", "SICK").isDowncast()).isTrue();
        assertThat(mood("alice", "2026-10-05", "TIRED").isDowncast()).isTrue();
        assertThat(mood("alice", "2026-10-05", "LOVE").isDowncast()).isFalse();
        assertThat(mood("alice", "2026-10-05", "BUSY").isDowncast()).isFalse();
    }

    @Test
    void recordedByIsPerUserAndDayStaysTheNaturalKey() {
        Mood row = mood("alice", "2026-10-05", "CALM");

        assertThat(row.recordedBy("alice")).isTrue();
        assertThat(row.recordedBy("bob")).isFalse();
        assertThat(row.moodDay()).isEqualTo("2026-10-05");
        assertThat(row.mood()).isEqualTo("CALM");
    }

    @Test
    void restoreKeepsEveryColumnWithoutValidating() {
        Mood row = Mood.restore("m1", "s1", "bob", "2026-10-05", "NOT_A_MOOD", "一句话", 8L, 9L);

        assertThat(row.id()).isEqualTo("m1");
        assertThat(row.spaceId()).isEqualTo("s1");
        assertThat(row.mood()).isEqualTo("NOT_A_MOOD");
        assertThat(row.note()).isEqualTo("一句话");
        assertThat(row.created()).isEqualTo(8L);
        assertThat(row.updatedAt()).isEqualTo(9L);
        assertThat(row.isDowncast()).isFalse();
    }

    private Mood mood(String username, String day, String key) {
        return Mood.restore("m-" + username + "-" + day + "-" + key, "s1", username, day, key, null, 1L, null);
    }
}
