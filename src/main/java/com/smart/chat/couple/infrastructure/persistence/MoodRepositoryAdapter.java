package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.mood.Mood;
import com.smart.chat.couple.domain.mood.MoodRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link MoodRepository} 的 MyBatis-Plus 适配器：PO ↔ 薄实体的翻译只发生在这里。
 * <p>
 * 查询复用 {@link CoupleMoodMapper} 已有的 default 方法（Mapper 不感知领域类型），
 * 取数口径因此与改造前逐字相同。本切片没有写入用例，适配器不碰任何一列。
 */
@Component
public class MoodRepositoryAdapter implements MoodRepository {

    private final CoupleMoodMapper moodMapper;

    public MoodRepositoryAdapter(CoupleMoodMapper moodMapper) {
        this.moodMapper = moodMapper;
    }

    @Override
    public Optional<Mood> findBySpaceAndUserOn(String spaceId, String username, String day) {
        return moodMapper.find(spaceId, username, day).map(MoodRepositoryAdapter::toDomain);
    }

    @Override
    public List<Mood> listBySpace(String spaceId) {
        return moodMapper.findBySpace(spaceId).stream().map(MoodRepositoryAdapter::toDomain).toList();
    }

    private static Mood toDomain(CoupleMoodPO po) {
        return Mood.restore(po.getId(), po.getSpaceId(), po.getUsername(), po.getMoodDay(), po.getMood(),
                po.getNote(), po.getCreated(), po.getUpdatedAt());
    }
}
