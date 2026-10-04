package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.reaction.MessageReaction;
import com.smart.chat.messaging.domain.reaction.MessageReactionRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** {@link MessageReactionRepository} 的 MyBatis-Plus 适配器（回应是流水表，只有追加与删除，没有更新路径）。 */
@Component
public class MessageReactionRepositoryAdapter implements MessageReactionRepository {

    private final MessageReactionMapper reactionMapper;

    public MessageReactionRepositoryAdapter(MessageReactionMapper reactionMapper) {
        this.reactionMapper = reactionMapper;
    }

    @Override
    public List<MessageReaction> listByMsgIds(Collection<String> msgIds) {
        if (msgIds.isEmpty()) {
            return List.of();
        }
        return reactionMapper.findByMsgIds(msgIds).stream().map(MessageReactionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Optional<MessageReaction> findByMsgIdAndUserAndEmoji(String msgId, String username, String emoji) {
        return Optional.ofNullable(reactionMapper.findUnique(msgId, username, emoji))
                .map(MessageReactionRepositoryAdapter::toDomain);
    }

    @Override
    public void save(MessageReaction reaction) {
        MessageReactionPO po = new MessageReactionPO();
        po.setId(reaction.id());
        po.setMsgId(reaction.msgId());
        po.setUsername(reaction.username());
        po.setEmoji(reaction.emoji());
        po.setCreated(reaction.created());
        reactionMapper.insert(po);
    }

    @Override
    public void deleteById(String id) {
        reactionMapper.deleteById(id);
    }

    private static MessageReaction toDomain(MessageReactionPO po) {
        return MessageReaction.restore(po.getId(), po.getMsgId(), po.getUsername(), po.getEmoji(), po.getCreated());
    }
}
