package com.smart.chat.im;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MessageStarMapper extends BaseMapperCompat<MessageStar> {

    default List<MessageStar> findByUsername(String username) {
        return selectList(new LambdaQueryWrapper<MessageStar>()
                .eq(MessageStar::getUsername, username)
                .orderByDesc(MessageStar::getCreated));
    }

    default List<MessageStar> findByUsernameAndMsgIds(String username, Collection<String> msgIds) {
        if (msgIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<MessageStar>()
                .eq(MessageStar::getUsername, username)
                .in(MessageStar::getMsgId, msgIds));
    }

    default MessageStar findUnique(String username, String msgId) {
        return selectOne(new LambdaQueryWrapper<MessageStar>()
                .eq(MessageStar::getUsername, username)
                .eq(MessageStar::getMsgId, msgId)
                .last("LIMIT 1"));
    }
}
