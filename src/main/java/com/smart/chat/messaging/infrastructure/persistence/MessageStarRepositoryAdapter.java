package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.star.MessageStar;
import com.smart.chat.messaging.domain.star.MessageStarRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** {@link MessageStarRepository} 的 MyBatis-Plus 适配器（收藏是流水表，只有追加与删除，没有更新路径）。 */
@Component
public class MessageStarRepositoryAdapter implements MessageStarRepository {

    private final MessageStarMapper starMapper;

    public MessageStarRepositoryAdapter(MessageStarMapper starMapper) {
        this.starMapper = starMapper;
    }

    @Override
    public List<MessageStar> listByUsername(String username) {
        return starMapper.findByUsername(username).stream().map(MessageStarRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<MessageStar> listByUsernameAndMsgIds(String username, Collection<String> msgIds) {
        if (msgIds.isEmpty()) {
            return List.of();
        }
        return starMapper.findByUsernameAndMsgIds(username, msgIds).stream()
                .map(MessageStarRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<MessageStar> findByUsernameAndMsgId(String username, String msgId) {
        return Optional.ofNullable(starMapper.findUnique(username, msgId))
                .map(MessageStarRepositoryAdapter::toDomain);
    }

    @Override
    public void save(MessageStar star) {
        MessageStarPO po = new MessageStarPO();
        po.setId(star.id());
        po.setUsername(star.username());
        po.setMsgId(star.msgId());
        po.setCreated(star.created());
        starMapper.insert(po);
    }

    @Override
    public void deleteById(String id) {
        starMapper.deleteById(id);
    }

    private static MessageStar toDomain(MessageStarPO po) {
        return MessageStar.restore(po.getId(), po.getUsername(), po.getMsgId(), po.getCreated());
    }
}
