package com.smart.chat.couple.domain.question;

import java.util.List;
import java.util.Optional;

/** 每日一问回答的仓储端口。 */
public interface QuestionAnswerRepository {

    /** 某人某天那一题的回答 */
    Optional<QuestionAnswer> find(String spaceId, String day, String username);

    List<QuestionAnswer> findBySpaceAndDay(String spaceId, String day);

    /** 某空间从 fromDay（含）起的回答，按日期倒序——回看与百日回顾都读这个 */
    List<QuestionAnswer> findBySpaceFrom(String spaceId, String fromDay);

    List<QuestionAnswer> findBySpace(String spaceId);

    /** 新答则插入、改答则只回写答案与改动时刻 */
    void save(QuestionAnswer answer);

    /** 全库累计回答条数（管理看板用） */
    long countAll();
}
