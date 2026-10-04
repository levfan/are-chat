package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.pin.Conversation;
import com.smart.chat.messaging.domain.pin.ConversationPin;
import com.smart.chat.messaging.domain.pin.ConversationPinRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@link ConversationPinRepository} 的 MyBatis-Plus 适配器。
 * <p>
 * {@link #replace} 把「一个会话最多一条置顶」这条不变式落在存储上：先按规范化后的
 * {@code user_a/user_b} 删掉原有的，再插入新的——与改造前 {@code PrivateMessageService.pin} 里
 * 手写的那两趟一致，只是现在只有一个地方能写这张表。
 */
@Component
public class ConversationPinRepositoryAdapter implements ConversationPinRepository {

    private final ConversationPinMapper pinMapper;

    public ConversationPinRepositoryAdapter(ConversationPinMapper pinMapper) {
        this.pinMapper = pinMapper;
    }

    @Override
    public Optional<ConversationPin> findFor(Conversation conversation) {
        return pinMapper.findForConversation(conversation.userA(), conversation.userB())
                .map(ConversationPinRepositoryAdapter::toDomain);
    }

    @Override
    public void replace(ConversationPin pin) {
        pinMapper.deleteForConversation(pin.userA(), pin.userB());
        ConversationPinPO po = new ConversationPinPO();
        po.setId(pin.id());
        po.setUserA(pin.userA());
        po.setUserB(pin.userB());
        po.setMsgId(pin.msgId());
        po.setCreatedBy(pin.createdBy());
        po.setCreated(pin.created());
        pinMapper.insert(po);
    }

    @Override
    public void clear(Conversation conversation) {
        pinMapper.deleteForConversation(conversation.userA(), conversation.userB());
    }

    private static ConversationPin toDomain(ConversationPinPO po) {
        return ConversationPin.restore(po.getId(), po.getUserA(), po.getUserB(), po.getMsgId(), po.getCreatedBy(),
                po.getCreated());
    }
}
