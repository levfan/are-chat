package com.smart.chat.couple.domain.surprise;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 刮刮乐周券聚合的单测：收券人才能刮、送券人才核销、没刮开不能核销、
 * 两个动作都幂等，以及「券面在未刮开时对收券人保密」这条回显口径。
 */
class ScratchTest {

    private static Scratch issued() {
        // alice 送、bob 收
        return Scratch.issue("s1", "2026-W40", "alice", "bob", "hug", "一个不少于 10 秒的用力抱抱");
    }

    @Test
    void issueHandsOutABrandNewCard() {
        Scratch card = issued();
        assertThat(card.id()).isNotBlank();
        assertThat(card.scratched()).isFalse();
        assertThat(card.redeemed()).isFalse();
        assertThat(card.scratchedAt()).isNull();
        assertThat(card.redeemedAt()).isNull();
        assertThat(card.created()).isPositive();
        assertThat(card.ownedBy("bob")).isTrue();
        assertThat(card.ownedBy("alice")).isFalse();
    }

    @Test
    void onlyTheReceiverCanScratchAndScratchingTwiceChangesNothing() {
        Scratch card = issued();
        assertThatThrownBy(() -> card.scratchBy("alice"))
                .isInstanceOfSatisfying(RuleViolation.class, e -> {
                    assertThat(e.getMessage()).isEqualTo("这是 TA 的刮刮乐，不能替 TA 刮哦");
                    assertThat(e.status()).isEqualTo(403);
                });
        assertThat(card.scratchBy("bob")).isTrue();
        assertThat(card.scratched()).isTrue();
        assertThat(card.scratchedAt()).isNotNull();
        assertThat(card.scratchBy("bob")).isFalse();
    }

    @Test
    void onlyTheGiverCanRedeemAndOnlyAfterItIsScratched() {
        Scratch card = issued();
        // 收券人自己点核销：不归他
        assertThatThrownBy(() -> card.redeemBy("bob"))
                .isInstanceOfSatisfying(RuleViolation.class, e -> {
                    assertThat(e.getMessage()).isEqualTo("只有送券的人才能点核销哦");
                    assertThat(e.status()).isEqualTo(403);
                });
        // 送券人也不能跳过「先刮开」
        assertThatThrownBy(() -> card.redeemBy("alice"))
                .isInstanceOfSatisfying(RuleViolation.class, e -> {
                    assertThat(e.getMessage()).isEqualTo("TA 还没刮开这张券呢");
                    assertThat(e.status()).isZero();
                });

        card.scratchBy("bob");
        assertThat(card.redeemBy("alice")).isTrue();
        assertThat(card.redeemed()).isTrue();
        assertThat(card.redeemedAt()).isNotNull();
        // 重复核销：返回 false 让用例不再补第二笔分
        assertThat(card.redeemBy("alice")).isFalse();
    }

    @Test
    void prizeStaysHiddenFromTheReceiverUntilScratched() {
        Scratch card = issued();
        assertThat(card.visiblePrizeFor("bob")).isNull();
        assertThat(card.visiblePrizeFor("alice")).isEqualTo("一个不少于 10 秒的用力抱抱");
        card.scratchBy("bob");
        assertThat(card.visiblePrizeFor("bob")).isEqualTo("一个不少于 10 秒的用力抱抱");
        assertThat(card.visiblePrizeFor("alice")).isEqualTo("一个不少于 10 秒的用力抱抱");
    }

    @Test
    void restoreKeepsLegacyRowAsIs() {
        Scratch legacy = Scratch.restore("sc1", "s1", "2026-W39", "bob", "alice", "movie", "一场电影",
                true, 11L, null, null);
        assertThat(legacy.scratched()).isTrue();
        assertThat(legacy.redeemed()).isFalse();
        assertThat(legacy.created()).isZero();
        assertThat(legacy.redeemBy("bob")).isTrue();
    }
}
