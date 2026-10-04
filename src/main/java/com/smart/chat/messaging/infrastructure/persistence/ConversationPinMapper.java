package com.smart.chat.messaging.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface ConversationPinMapper extends BaseMapperCompat<ConversationPinPO> {

    default Optional<ConversationPinPO> findForConversation(String userA, String userB) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<ConversationPinPO>()
                .eq(ConversationPinPO::getUserA, userA)
                .eq(ConversationPinPO::getUserB, userB)
                .last("LIMIT 1")));
    }

    default void deleteForConversation(String userA, String userB) {
        delete(new LambdaQueryWrapper<ConversationPinPO>()
                .eq(ConversationPinPO::getUserA, userA)
                .eq(ConversationPinPO::getUserB, userB));
    }
}
