package com.smart.chat.couple.domain.memory;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 一句话总结的守卫：这句话是 100 天彩蛋页的门面，规则是「只说数据里有的事」。
 * 所以零值必须整段消失，不能留下「答完了 0 道」这种笑话，也不能出现悬空的分号。
 */
class RelationSummaryTest {

    private RelationSummary.Facts facts(int days, int confirmed, int longest, int makeup, int answered,
                                        int wishes, String title, String latest) {
        return new RelationSummary.Facts(days, confirmed, longest, makeup, answered, wishes, title, latest);
    }

    @Test
    void everyNumberComesFromTheFacts() {
        String text = RelationSummary.compose(facts(128, 96, 41, 3, 63, 4, "心有灵犀", "专属贴纸包"));
        assertThat(text)
                .contains("在一起 128 天")
                .contains("打卡 96 天")
                .contains("最长连着 41 天")
                .contains("其中 3 天是补签回来")
                .contains("答完了 63 道每日一问")
                .contains("实现了 4 个愿望")
                .contains("「心有灵犀」")
                .contains("「专属贴纸包」");
    }

    @Test
    void emptyFactsDoNotLeaveHollowClausesOrStrayPunctuation() {
        String text = RelationSummary.compose(facts(100, 0, 0, 0, 0, 0, null, null));
        assertThat(text).doesNotContain("答完了").doesNotContain("偷偷帮对方").doesNotContain("现在的你们是");
        assertThat(text).doesNotContain("，。").doesNotContain("，，");
        assertThat(text).startsWith("在一起 100 天，你们一起打卡 0 天。");
    }

    @Test
    void missingTitleOrLatestUnlockOnlyDropsThatHalfSentence() {
        String text = RelationSummary.compose(facts(100, 88, 100, 0, 0, 0, null, "隐藏彩蛋页"));
        assertThat(text).contains("最近解锁了「隐藏彩蛋页」").doesNotContain("现在的你们是");

        String noUnlock = RelationSummary.compose(facts(100, 88, 100, 0, 0, 0, "相守一生", null));
        assertThat(noUnlock).contains("「相守一生」").doesNotContain("最近解锁了");
    }
}
