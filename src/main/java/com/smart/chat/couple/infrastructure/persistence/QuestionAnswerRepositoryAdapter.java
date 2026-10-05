package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.question.QuestionAnswer;
import com.smart.chat.couple.domain.question.QuestionAnswerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link QuestionAnswerRepository} 的 MyBatis-Plus 适配器。
 * <p>
 * 更新只碰两列（答案与改动时刻）：题目、谁答的、哪天答的、哪天建的，都不该被改写答案顺带改掉。
 */
@Component
public class QuestionAnswerRepositoryAdapter implements QuestionAnswerRepository {

    private final CoupleQuestionAnswerMapper answerMapper;

    public QuestionAnswerRepositoryAdapter(CoupleQuestionAnswerMapper answerMapper) {
        this.answerMapper = answerMapper;
    }

    @Override
    public Optional<QuestionAnswer> find(String spaceId, String day, String username) {
        return answerMapper.find(spaceId, day, username).map(QuestionAnswerRepositoryAdapter::toDomain);
    }

    @Override
    public List<QuestionAnswer> findBySpaceAndDay(String spaceId, String day) {
        return answerMapper.findBySpaceAndDay(spaceId, day).stream()
                .map(QuestionAnswerRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<QuestionAnswer> findBySpaceFrom(String spaceId, String fromDay) {
        return answerMapper.findBySpaceFrom(spaceId, fromDay).stream()
                .map(QuestionAnswerRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<QuestionAnswer> findBySpace(String spaceId) {
        return answerMapper.findBySpace(spaceId).stream().map(QuestionAnswerRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(QuestionAnswer answer) {
        CoupleQuestionAnswerPO existing = answerMapper.selectById(answer.id());
        if (existing == null) {
            CoupleQuestionAnswerPO po = new CoupleQuestionAnswerPO();
            po.setId(answer.id());
            po.setSpaceId(answer.spaceId());
            po.setDay(answer.day());
            po.setQuestionIndex(answer.questionIndex());
            po.setQuestion(answer.question());
            po.setUsername(answer.username());
            po.setAnswer(answer.answerText());
            po.setCreated(answer.created());
            answerMapper.insert(po);
            return;
        }
        existing.setAnswer(answer.answerText());
        existing.setUpdatedAt(answer.updatedAt());
        answerMapper.updateById(existing);
    }

    @Override
    public long countAll() {
        return answerMapper.countAll();
    }

    private static QuestionAnswer toDomain(CoupleQuestionAnswerPO po) {
        return QuestionAnswer.restore(po.getId(), po.getSpaceId(), po.getDay(), po.getQuestionIndex(),
                po.getQuestion(), po.getUsername(), po.getAnswer(), po.getCreated(), po.getUpdatedAt());
    }
}
