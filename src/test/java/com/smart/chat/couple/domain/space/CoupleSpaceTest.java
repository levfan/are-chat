package com.smart.chat.couple.domain.space;

import com.smart.chat.couple.domain.RuleViolation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 空间聚合的不变式——纯领域测试，不起 Spring。 */
class CoupleSpaceTest {

    @Test
    void membersAreAlwaysOrderedByNameRegardlessOfWhoInvitedFirst() {
        CoupleSpace byAlice = CoupleSpace.open("alice", "bob", 1L);
        CoupleSpace byBob = CoupleSpace.open("bob", "alice", 1L);
        assertThat(byAlice.userA()).isEqualTo("alice");
        assertThat(byBob.userA()).isEqualTo("alice");
        assertThat(byBob.userB()).isEqualTo("bob");
    }

    @Test
    void partnerOfRejectsOutsiders() {
        CoupleSpace space = CoupleSpace.open("alice", "bob", 1L);
        assertThat(space.partnerOf("alice")).isEqualTo("bob");
        assertThat(space.partnerOf("bob")).isEqualTo("alice");
        assertThatThrownBy(() -> space.partnerOf("carol")).isInstanceOf(RuleViolation.class);
    }

    @Test
    void cannotBindYourself() {
        assertThatThrownBy(() -> CoupleSpace.open("alice", "alice", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("自己");
    }

    @Test
    void dissolveIsOneWayAndStamped() {
        CoupleSpace space = CoupleSpace.open("alice", "bob", 1L);
        assertThat(space.isActive()).isTrue();
        space.dissolve(999L);
        assertThat(space.status()).isEqualTo(CoupleSpace.STATUS_DISSOLVED);
        assertThat(space.dissolvedAt()).isEqualTo(999L);
        assertThat(space.isActive()).isFalse();
        assertThatThrownBy(() -> space.dissolve(1000L)).isInstanceOf(RuleViolation.class);
        assertThatThrownBy(() -> space.bindAnniversary("2026-10-01")).isInstanceOf(RuleViolation.class);
    }

    @Test
    void anniversaryIsBlankTreatedAsUnset() {
        CoupleSpace space = CoupleSpace.open("alice", "bob", 1L);
        space.bindAnniversary("2026-06-01");
        assertThat(space.anniversary()).isEqualTo("2026-06-01");
        space.bindAnniversary("   ");
        assertThat(space.anniversary()).isNull();
    }
}
