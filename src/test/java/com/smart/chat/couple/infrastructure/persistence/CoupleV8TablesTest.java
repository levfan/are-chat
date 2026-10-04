package com.smart.chat.couple.infrastructure.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * V52 三张新表的**真库**测试（H2 MODE=MySQL + Flyway 全量脚本）。
 * mock 单测只会把 stub 改成新签名然后一路绿灯，建表脚本、列宽、唯一键这三件事
 * 只有打到真表才暴露——本轮三张表都靠唯一键兜住「一天一行 / 同人同日一答 / 同名愿望」，
 * 唯一键没生效就是重复打卡与重复愿望的源头。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.MethodName.class)
class CoupleV8TablesTest {

    private static final String SPACE = "v8_test_space";
    private static final String ALICE = "v8_alice";
    private static final String BOB = "v8_bob";
    private static final String DAY = "2026-10-05";

    @Autowired
    private CoupleBondDayMapper bondDayMapper;
    @Autowired
    private CoupleQuestionAnswerMapper answerMapper;
    @Autowired
    private CoupleWishMapper wishMapper;

    @AfterEach
    void cleanUp() {
        bondDayMapper.findBySpace(SPACE).forEach(row -> bondDayMapper.deleteById(row.getId()));
        answerMapper.findBySpace(SPACE).forEach(row -> answerMapper.deleteById(row.getId()));
        wishMapper.findBySpace(SPACE).forEach(row -> wishMapper.deleteById(row.getId()));
    }

    @Test
    void bondDayRowsRoundTripAndCarryTheSourceColumn() {
        bondDayMapper.insert(CoupleBondDayPO.of(SPACE, DAY, CoupleBondDayPO.SOURCE_AUTO, null));
        bondDayMapper.insert(CoupleBondDayPO.of(SPACE, "2026-10-04", CoupleBondDayPO.SOURCE_MAKEUP, ALICE));

        List<CoupleBondDayPO> rows = bondDayMapper.findBySpace(SPACE);
        assertThat(rows).hasSize(2);
        // 默认查询按 day 升序，连续段算法依赖这个顺序
        assertThat(rows.get(0).getDay()).isEqualTo("2026-10-04");
        assertThat(rows.get(0).makeupFlag()).isTrue();
        assertThat(rows.get(1).getSource()).isEqualTo(CoupleBondDayPO.SOURCE_AUTO);
        assertThat(bondDayMapper.countMakeupBetween(SPACE, "2026-10-01", "2026-10-31")).isEqualTo(1);
    }

    @Test
    void bondDayUniqueKeyBlocksADuplicateDay() {
        bondDayMapper.insert(CoupleBondDayPO.of(SPACE, DAY, CoupleBondDayPO.SOURCE_AUTO, null));

        assertThatThrownBy(() -> bondDayMapper.insert(
                CoupleBondDayPO.of(SPACE, DAY, CoupleBondDayPO.SOURCE_MAKEUP, ALICE)))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(bondDayMapper.find(SPACE, DAY).getSource()).isEqualTo(CoupleBondDayPO.SOURCE_AUTO);
    }

    @Test
    void questionAnswerIsUniquePerSpaceDayUser() {
        answerMapper.insert(CoupleQuestionAnswerPO.of(SPACE, DAY, 7, "今天最开心的一件事是什么？", ALICE, "见到你"));
        answerMapper.insert(CoupleQuestionAnswerPO.of(SPACE, DAY, 7, "今天最开心的一件事是什么？", BOB, "吃到面了"));

        assertThat(answerMapper.findBySpaceAndDay(SPACE, DAY)).hasSize(2);
        assertThat(answerMapper.find(SPACE, DAY, ALICE).orElseThrow().getAnswer()).isEqualTo("见到你");

        assertThatThrownBy(() -> answerMapper.insert(
                CoupleQuestionAnswerPO.of(SPACE, DAY, 7, "今天最开心的一件事是什么？", ALICE, "改写一遍")))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void wishTitleIsUniquePerOwnerAndStatusCountsAcrossBothSides() {
        CoupleWishPO mine = CoupleWishPO.of(SPACE, ALICE, ALICE, "想要一副耳机", "入耳式");
        mine.setId("v8-wish-1");
        wishMapper.insert(mine);
        CoupleWishPO theirs = CoupleWishPO.of(SPACE, BOB, ALICE, "帮 TA 记下：一台相机", null);
        theirs.setId("v8-wish-2");
        theirs.setStatus(CoupleWishPO.STATUS_FULFILLED);
        wishMapper.insert(theirs);

        assertThat(wishMapper.findBySpace(SPACE)).hasSize(2);
        assertThat(wishMapper.existsSameTitle(SPACE, ALICE, "想要一副耳机")).isTrue();
        assertThat(wishMapper.existsSameTitle(SPACE, BOB, "想要一副耳机")).isFalse();
        assertThat(wishMapper.countFulfilled(SPACE)).isEqualTo(1);
        assertThat(wishMapper.countFulfilledBy(SPACE, ALICE)).isZero();

        assertThatThrownBy(() -> wishMapper.insert(CoupleWishPO.of(SPACE, ALICE, BOB, "想要一副耳机", null)))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void wishColumnsHoldTheDeclaredWidths() {
        // title 80 / note 200 是列宽合同，超了必须被数据库拒绝而不是静默截断
        CoupleWishPO longTitle = CoupleWishPO.of(SPACE, ALICE, ALICE, "一".repeat(81), null);
        assertThatThrownBy(() -> wishMapper.insert(longTitle)).isInstanceOf(Exception.class);

        CoupleQuestionAnswerPO answerTooLong = CoupleQuestionAnswerPO.of(SPACE, DAY, 0, "题", ALICE, "一".repeat(301));
        assertThatThrownBy(() -> answerMapper.insert(answerTooLong)).isInstanceOf(Exception.class);
    }
}
