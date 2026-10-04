package com.smart.chat.messaging.domain.pin;

import java.util.Optional;

/** 会话置顶的仓储端口。 */
public interface ConversationPinRepository {

    /** 当前会话的置顶（没有就是空，由 application 投影成 null） */
    Optional<ConversationPin> findFor(Conversation conversation);

    /** 落一条置顶：先清掉本会话原有的那条，保证「一个会话最多一条」 */
    void replace(ConversationPin pin);

    /** 取消置顶 */
    void clear(Conversation conversation);
}
