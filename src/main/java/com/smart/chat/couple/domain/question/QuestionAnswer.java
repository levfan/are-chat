package com.smart.chat.couple.domain.question;

import java.util.UUID;

/**
 * 每日一问的一行回答：一个空间、一天、一个人一条。
 * <p>
 * 薄实体——「双方都答完才能互看」这条真规则在 {@link DailyQuestion}（一天一题的读模型）里，
 * 这一层只负责「谁在什么时候答了哪道题」，改写答案不产生新行为。
 */
public final class QuestionAnswer {

    private final String id;
    private final String spaceId;
    private final String day;
    private final int questionIndex;
    private final String question;
    private final String username;
    private String answer;
    private final long created;
    private Long updatedAt;

    private QuestionAnswer(String id, String spaceId, String day, int questionIndex, String question,
                           String username, String answer, long created, Long updatedAt) {
        this.id = id;
        this.spaceId = spaceId;
        this.day = day;
        this.questionIndex = questionIndex;
        this.question = question;
        this.username = username;
        this.answer = answer;
        this.created = created;
        this.updatedAt = updatedAt;
    }

    /** 今天第一次作答 */
    public static QuestionAnswer answer(String spaceId, String day, int questionIndex, String question,
                                        String username, String text) {
        return new QuestionAnswer(UUID.randomUUID().toString(), spaceId, day, questionIndex, question, username,
                text, System.currentTimeMillis(), null);
    }

    public static QuestionAnswer restore(String id, String spaceId, String day, Integer questionIndex,
                                         String question, String username, String answer, Long created,
                                         Long updatedAt) {
        return new QuestionAnswer(id, spaceId, day, questionIndex == null ? 0 : questionIndex, question, username,
                answer, created == null ? 0L : created, updatedAt);
    }

    /** 当天允许改写答案（过了今天就由读模型按已答处理） */
    public void rewrite(String text) {
        this.answer = text;
        this.updatedAt = System.currentTimeMillis();
    }

    public String id() {
        return id;
    }

    public String spaceId() {
        return spaceId;
    }

    public String day() {
        return day;
    }

    public int questionIndex() {
        return questionIndex;
    }

    public String question() {
        return question;
    }

    public String username() {
        return username;
    }

    public String answerText() {
        return answer;
    }

    public long created() {
        return created;
    }

    public Long updatedAt() {
        return updatedAt;
    }
}
