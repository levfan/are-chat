package com.smart.chat.im;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PrivateMessageMapper extends BaseMapperCompat<PrivateMessage> {

    /** 双向会话过滤：(me→peer) OR (peer→me)。 */
    default LambdaQueryWrapper<PrivateMessage> conversationWrapper(String me, String peer) {
        return new LambdaQueryWrapper<PrivateMessage>()
                .and(w -> w
                        .and(w1 -> w1.eq(PrivateMessage::getFromUser, me).eq(PrivateMessage::getToUser, peer))
                        .or(w2 -> w2.eq(PrivateMessage::getFromUser, peer).eq(PrivateMessage::getToUser, me)));
    }

    /** 倒序取一页（before 为游标：早于该时间），调用方自行反转为正序。 */
    default List<PrivateMessage> findConversationPage(String me, String peer, Long before, int limit) {
        LambdaQueryWrapper<PrivateMessage> wrapper = conversationWrapper(me, peer)
                .lt(before != null && before > 0, PrivateMessage::getCreated, before)
                .orderByDesc(PrivateMessage::getCreated)
                .last("LIMIT " + limit);
        return selectList(wrapper);
    }

    default Optional<PrivateMessage> findLatestBetween(String me, String peer) {
        return Optional.ofNullable(selectOne(conversationWrapper(me, peer)
                .orderByDesc(PrivateMessage::getCreated)
                .last("LIMIT 1")));
    }

    default long countUnread(String me, String peer, Long lastReadAt) {
        return selectCount(conversationWrapper(me, peer)
                .eq(PrivateMessage::getFromUser, peer)
                .eq(PrivateMessage::getStatus, PrivateMessage.STATUS_SENT)
                .gt(PrivateMessage::getCreated, lastReadAt == null ? 0L : lastReadAt));
    }

    /** 会话内关键字搜索（只搜未撤回），倒序取最近 50 条，调用方反转为正序。 */
    default List<PrivateMessage> searchConversation(String me, String peer, String keyword) {
        return selectList(conversationWrapper(me, peer)
                .eq(PrivateMessage::getStatus, PrivateMessage.STATUS_SENT)
                .like(PrivateMessage::getContent, keyword)
                .orderByDesc(PrivateMessage::getCreated)
                .last("LIMIT 50"));
    }

    /** 56 导出：全量拉取双向会话消息（正序由调用方保证，上限 10000 条）。 */
    default List<PrivateMessage> findConversationAll(String me, String peer) {
        return selectList(conversationWrapper(me, peer)
                .orderByAsc(PrivateMessage::getCreated)
                .last("LIMIT 10000"));
    }

    /** 已读回执：把 from 发给 to 的未读消息全部标记已读。 */
    default int markIncomingRead(String from, String to) {
        return update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<PrivateMessage>()
                .eq(PrivateMessage::getFromUser, from)
                .eq(PrivateMessage::getToUser, to)
                .eq(PrivateMessage::getReadFlag, 0)
                .set(PrivateMessage::getReadFlag, 1));
    }
}
