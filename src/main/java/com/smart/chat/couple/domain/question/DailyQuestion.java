package com.smart.chat.couple.domain.question;

import com.smart.chat.couple.domain.RuleViolation;

/**
 * 每日一问的当日视图：一个空间一天一题，每人各答一次，当天可以改写。
 * <p>
 * 「双方回答后互看」是本题唯一有趣的产品规则：<b>谁没答，就看不到 TA 的答案</b>——
 * 所以 {@link #partnerAnswerText()} 必须同时满足「我已答」与「TA 已答」，
 * 这条判断留在领域里，前端拿到的 null 就是答不了的样子。
 */
public final class DailyQuestion {

    public static final int ANSWER_MAX = 300;
    public static final int QUESTION_MAX = 200;

    private final String day;
    private final int index;
    private final String question;
    private final String myAnswer;
    private final String partnerAnswer;

    private DailyQuestion(String day, int index, String question, String myAnswer, String partnerAnswer) {
        this.day = day;
        this.index = index;
        this.question = question;
        this.myAnswer = myAnswer;
        this.partnerAnswer = partnerAnswer;
    }

    public static DailyQuestion of(String day, int index, String question, String myAnswer, String partnerAnswer) {
        return new DailyQuestion(day, index, question, blankToNull(myAnswer), blankToNull(partnerAnswer));
    }

    /** 提交前的文字闸门：必填、不超长。 */
    public static String requireAnswerText(String answer) {
        String text = answer == null ? "" : answer.trim();
        if (text.isEmpty()) {
            throw new RuleViolation("写一句再交卷呀 📝");
        }
        if (text.length() > ANSWER_MAX) {
            throw new RuleViolation("回答最多 " + ANSWER_MAX + " 个字，短一点更像人话");
        }
        return text;
    }

    /** 我答过了没有。 */
    public boolean answeredByMe() {
        return myAnswer != null;
    }

    public boolean answeredByPartner() {
        return partnerAnswer != null;
    }

    /** 双方都答完了——今天这道题才算翻篇。 */
    public boolean bothAnswered() {
        return answeredByMe() && answeredByPartner();
    }

    /** 我能看到的 TA 的答案：我没答时不给看（否则谁都不肯先写）。 */
    public String partnerAnswerText() {
        return bothAnswered() ? partnerAnswer : null;
    }

    public String day() {
        return day;
    }

    public int index() {
        return index;
    }

    public String question() {
        return question;
    }

    public String myAnswerText() {
        return myAnswer;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
