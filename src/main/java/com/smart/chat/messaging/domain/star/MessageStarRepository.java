package com.smart.chat.messaging.domain.star;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 收藏的仓储端口。 */
public interface MessageStarRepository {

    /** 我的收藏夹（按收藏时间倒序） */
    List<MessageStar> listByUsername(String username);

    /** 会话页一次性装配「我收藏了哪几条」 */
    List<MessageStar> listByUsernameAndMsgIds(String username, Collection<String> msgIds);

    Optional<MessageStar> findByUsernameAndMsgId(String username, String msgId);

    /** 收藏是流水表：只有追加，没有更新路径 */
    void save(MessageStar star);

    void deleteById(String id);
}
