package com.smart.chat.messaging.domain.reaction;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 表情回应的仓储端口（会话页一次性批量装配，不许逐条查）。 */
public interface MessageReactionRepository {

    List<MessageReaction> listByMsgIds(Collection<String> msgIds);

    Optional<MessageReaction> findByMsgIdAndUserAndEmoji(String msgId, String username, String emoji);

    /** 回应是流水表：只有追加，没有更新路径 */
    void save(MessageReaction reaction);

    void deleteById(String id);
}
