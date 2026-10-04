package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.mood.MoodReaction;
import com.smart.chat.couple.domain.mood.MoodReactionRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link MoodReactionRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 取行复用 {@link CoupleMoodReactionMapper#find} 已有的 default 方法（Mapper 不感知领域类型），
 * 「每人每空间每天一条」的定位口径因此与改造前逐字相同。
 * <p>
 * 写回沿用 {@code CoupleSpaceRepositoryAdapter} 确立的纪律：<b>只回写聚合纳管的那两列</b>
 * （{@code reaction} 与 {@code updated_at}）。空间、心情日、回应人、创建时刻发布即冻结，
 * 所以更新分支先 {@code selectById} 取回原行、改完再写回，绝不拿聚合重建整行。
 */
@Component
public class MoodReactionRepositoryAdapter implements MoodReactionRepository {

    private final CoupleMoodReactionMapper moodReactionMapper;

    public MoodReactionRepositoryAdapter(CoupleMoodReactionMapper moodReactionMapper) {
        this.moodReactionMapper = moodReactionMapper;
    }

    @Override
    public Optional<MoodReaction> findBySpaceAndUserOn(String spaceId, String moodDay, String fromUser) {
        return Optional.ofNullable(moodReactionMapper.find(spaceId, moodDay, fromUser))
                .map(MoodReactionRepositoryAdapter::toDomain);
    }

    @Override
    public void save(MoodReaction reaction) {
        CoupleMoodReactionPO existing = moodReactionMapper.selectById(reaction.id());
        if (existing == null) {
            moodReactionMapper.insert(toPo(reaction));
            return;
        }
        applyOwnedFields(existing, reaction);
        moodReactionMapper.updateById(existing);
    }

    /** 聚合负责维护的列：只有回应与修改时刻。 */
    private static void applyOwnedFields(CoupleMoodReactionPO po, MoodReaction reaction) {
        po.setReaction(reaction.reaction());
        po.setUpdatedAt(reaction.updatedAt());
    }

    private static CoupleMoodReactionPO toPo(MoodReaction reaction) {
        CoupleMoodReactionPO po = new CoupleMoodReactionPO();
        po.setId(reaction.id());
        po.setSpaceId(reaction.spaceId());
        po.setMoodDay(reaction.moodDay());
        po.setFromUser(reaction.fromUser());
        po.setReaction(reaction.reaction());
        po.setCreated(reaction.created());
        po.setUpdatedAt(reaction.updatedAt());
        return po;
    }

    private static MoodReaction toDomain(CoupleMoodReactionPO po) {
        return MoodReaction.restore(po.getId(), po.getSpaceId(), po.getMoodDay(), po.getFromUser(),
                po.getReaction(), po.getCreated(), po.getUpdatedAt());
    }
}
