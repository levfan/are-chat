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
    void nicksFollowTheReorderedMembers() {
        // 传入顺序被换过来了，爱称也必须跟着人走，不能贴错格子
        CoupleSpace space = CoupleSpace.restore("id1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 1L,
                null, "宝宝", "猪猪", null, "classic", null, null);
        assertThat(space.nickOf("alice")).isEqualTo("宝宝");
        assertThat(space.nickOf("bob")).isEqualTo("猪猪");
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
        assertThatThrownBy(() -> space.decorate("宣言", "classic", null)).isInstanceOf(RuleViolation.class);
    }

    @Test
    void decorateGuardsThemeWhitelistAndStickerCap() {
        CoupleSpace space = CoupleSpace.open("alice", "bob", 1L);
        space.decorate("只有彼此懂的一句话", "cherry", "a,b,c");
        assertThat(space.theme()).isEqualTo("cherry");
        assertThat(space.slogan()).isEqualTo("只有彼此懂的一句话");
        assertThat(space.stickers()).isEqualTo("a,b,c");

        assertThatThrownBy(() -> space.decorate(null, "neon", null))
                .isInstanceOf(RuleViolation.class)
                .hasMessageContaining("主题");
        assertThatThrownBy(() -> space.decorate(null, null, "1,2,3,4,5,6,7"))
                .isInstanceOf(RuleViolation.class)
                .hasMessageContaining("贴纸");
        // 上限本身要放过
        space.decorate(null, null, "1,2,3,4,5,6");
        assertThat(space.stickers()).isEqualTo("1,2,3,4,5,6");
    }

    @Test
    void renamingPartnerIsTheOtherHalfsPrivilege() {
        CoupleSpace space = CoupleSpace.open("alice", "bob", 1L);
        space.renamePartner("bob", "宝宝");
        assertThat(space.nickOf("alice")).isEqualTo("宝宝");
        assertThat(space.nickOf("bob")).isNull();
        // 留空 = 清除
        space.renamePartner("bob", "   ");
        assertThat(space.nickOf("alice")).isNull();
    }
}
