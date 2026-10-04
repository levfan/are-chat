package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MessageReactionMapper extends BaseMapperCompat<MessageReactionPO> {

    /** 批量取一组消息的回应（会话页一次性装配）。 */
    default List<MessageReactionPO> findByMsgIds(Collection<String> msgIds) {
        if (msgIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<MessageReactionPO>()
                .in(MessageReactionPO::getMsgId, msgIds));
    }

    default MessageReactionPO findUnique(String msgId, String username, String emoji) {
        return selectOne(new LambdaQueryWrapper<MessageReactionPO>()
                .eq(MessageReactionPO::getMsgId, msgId)
                .eq(MessageReactionPO::getUsername, username)
                .eq(MessageReactionPO::getEmoji, emoji)
                .last("LIMIT 1"));
    }
}
