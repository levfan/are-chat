package com.smart.chat.couple.domain.wish;

import com.smart.chat.couple.domain.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 愿望清单的守卫，重点是那条「偷偷」：**被许愿的人永远看不到「已准备」**，
 * 哪怕只是状态字段泄露一点点，惊喜就没了。所以这里连 visibleStatusFor 与
 * keepsPreparationSecretFrom 都要单独钉住。
 */
class WishTest {

    private Wish aWish() {
        return Wish.add("alice", "alice", "想要一副耳机", "入耳式的");
    }

    @Test
    void titleIsRequiredAndCapped() {
        assertThatThrownBy(() -> Wish.add("alice", "alice", "  ", null))
                .isInstanceOf(RuleViolation.class).hasMessage("想要什么总得写一句呀");
        assertThatThrownBy(() -> Wish.add("alice", "alice", "一".repeat(Wish.TITLE_MAX + 1), null))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("80");
        assertThatThrownBy(() -> Wish.add("alice", "alice", "耳机", "说".repeat(Wish.NOTE_MAX + 1)))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("补充说明");
    }

    @Test
    void blankOwnerDefaultsToTheCreator() {
        Wish wish = Wish.add(null, "alice", "想一起看海", "  ");
        assertThat(wish.ownerUser()).isEqualTo("alice");
        assertThat(wish.note()).isNull();
    }

    @Test
    void onlyThePartnerCanMarkItPrepared() {
        Wish wish = aWish();
        assertThatThrownBy(() -> wish.prepareBy("alice", 1L))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("给 TA 留的");
        wish.prepareBy("bob", 100L);
        assertThat(wish.preparedFlag()).isTrue();
        assertThat(wish.status()).isEqualTo(Wish.STATUS_PREPARED);
        assertThatThrownBy(() -> wish.prepareBy("bob", 200L))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("别再点一次");
    }

    @Test
    void ownerRejectedMessagesMustNotRevealThatSomethingWasPrepared() {
        // 真后端探针实测到的缺陷：许愿人重复点一次，旧顺序会回她「已经标过「已准备」了」，
        // 这句话本身就把惊喜说出去了。归属闸门必须排在状态闸门前面。
        Wish prepared = aWish();
        prepared.prepareBy("bob", 100L);

        assertThatThrownBy(() -> prepared.prepareBy("alice", 200L))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("这条愿望是你自己许的，「已准备」那一格是给 TA 留的");
        assertThatThrownBy(() -> prepared.unprepareBy("alice"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("这条愿望是你自己许的，「已准备」那一格是给 TA 留的");
        // 两条话术对许愿人来说与「没被标记过」的开放愿望完全同形，看不出状态差别
        Wish open = aWish();
        assertThatThrownBy(() -> open.prepareBy("alice", 200L))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("这条愿望是你自己许的，「已准备」那一格是给 TA 留的");
        assertThatThrownBy(() -> open.unprepareBy("alice"))
                .isInstanceOf(RuleViolation.class)
                .hasMessage("这条愿望是你自己许的，「已准备」那一格是给 TA 留的");
    }

    @Test
    void preparationIsInvisibleToTheOwnerButNotToTheMarker() {
        Wish wish = aWish();
        wish.prepareBy("bob", 100L);
        assertThat(wish.keepsPreparationSecretFrom("alice")).isTrue();
        assertThat(wish.keepsPreparationSecretFrom("bob")).isFalse();
        assertThat(wish.visibleStatusFor("alice")).isEqualTo(Wish.STATUS_OPEN);
        assertThat(wish.visibleStatusFor("bob")).isEqualTo(Wish.STATUS_PREPARED);
    }

    @Test
    void onlyTheMarkerCanTakeThePreparationBack() {
        Wish wish = aWish();
        wish.prepareBy("bob", 100L);
        // 许愿人先被归属闸门挡下（见上一条用例），所以「谁来撤」这条规则要用第三人来验：
        // 两人空间里正常路径走不到它，但它是这条状态机的最后一道归属保护，不能当死代码。
        Wish threeWay = Wish.restore("w9", "carol", "alice", "第三人视角", null,
                Wish.STATUS_PREPARED, "bob", 100L, null);
        assertThatThrownBy(() -> threeWay.unprepareBy("dave"))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("就只能由谁撤掉");

        assertThatThrownBy(() -> wish.unprepareBy("nobody-but-marker"))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("就只能由谁撤掉");
        wish.unprepareBy("bob");
        assertThat(wish.status()).isEqualTo(Wish.STATUS_OPEN);
        assertThat(wish.preparedBy()).isNull();
        assertThat(wish.preparedAt()).isNull();
    }

    @Test
    void onlyTheOwnerCanConfirmItHappened() {
        Wish wish = aWish();
        assertThatThrownBy(() -> wish.fulfillBy("bob", 1L))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("只有许愿的人");
        wish.fulfillBy("alice", 1L);
        assertThat(wish.fulfilledFlag()).isTrue();
        // 实现之后公开了，但状态是单向的：不能再改回准备或未准备
        assertThatThrownBy(() -> wish.prepareBy("bob", 2L))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("已经实现");
        assertThatThrownBy(() -> wish.fulfillBy("alice", 3L))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("不用再点一次");
        assertThat(wish.visibleStatusFor("bob")).isEqualTo(Wish.STATUS_FULFILLED);
    }

    @Test
    void deletionBelongsToTheRecorderAndIsBlockedOnceFulfilled() {
        Wish wish = Wish.add("alice", "bob", "想喝那家的奶茶", null);
        assertThatThrownBy(() -> wish.requireDeletableBy("alice"))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("只有记这条愿望的人");
        wish.requireDeletableBy("bob");
        wish.fulfillBy("alice", 1L);
        assertThatThrownBy(() -> wish.requireDeletableBy("bob"))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("删不掉");
    }

    @Test
    void onlyTheRecorderCanEditTheNote() {
        Wish wish = Wish.add("alice", "alice", "耳机", "黑色的");
        assertThatThrownBy(() -> wish.editNote("bob", "改一下"))
                .isInstanceOf(RuleViolation.class).hasMessageContaining("只有记这条愿望的人能改");
        wish.editNote("alice", "白色的也行");
        assertThat(wish.note()).isEqualTo("白色的也行");
    }
}
