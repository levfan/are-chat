package com.smart.chat.im;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MessageReactionMapper extends BaseMapperCompat<MessageReaction> {

    /** 批量取一组消息的回应（会话页一次性装配）。 */
    default List<MessageReaction> findByMsgIds(Collection<String> msgIds) {
        if (msgIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<MessageReaction>()
                .in(MessageReaction::getMsgId, msgIds));
    }

    default MessageReaction findUnique(String msgId, String username, String emoji) {
        return selectOne(new LambdaQueryWrapper<MessageReaction>()
                .eq(MessageReaction::getMsgId, msgId)
                .eq(MessageReaction::getUsername, username)
                .eq(MessageReaction::getEmoji, emoji)
                .last("LIMIT 1"));
    }
}
