package com.smart.chat.im;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface ConversationPinMapper extends BaseMapperCompat<ConversationPin> {

    default Optional<ConversationPin> findForConversation(String userA, String userB) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<ConversationPin>()
                .eq(ConversationPin::getUserA, userA)
                .eq(ConversationPin::getUserB, userB)
                .last("LIMIT 1")));
    }

    default void deleteForConversation(String userA, String userB) {
        delete(new LambdaQueryWrapper<ConversationPin>()
                .eq(ConversationPin::getUserA, userA)
                .eq(ConversationPin::getUserB, userB));
    }
}
