package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface MessageStarMapper extends BaseMapperCompat<MessageStarPO> {

    default List<MessageStarPO> findByUsername(String username) {
        return selectList(new LambdaQueryWrapper<MessageStarPO>()
                .eq(MessageStarPO::getUsername, username)
                .orderByDesc(MessageStarPO::getCreated));
    }

    default List<MessageStarPO> findByUsernameAndMsgIds(String username, Collection<String> msgIds) {
        if (msgIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapper<MessageStarPO>()
                .eq(MessageStarPO::getUsername, username)
                .in(MessageStarPO::getMsgId, msgIds));
    }

    default MessageStarPO findUnique(String username, String msgId) {
        return selectOne(new LambdaQueryWrapper<MessageStarPO>()
                .eq(MessageStarPO::getUsername, username)
                .eq(MessageStarPO::getMsgId, msgId)
                .last("LIMIT 1"));
    }
}
