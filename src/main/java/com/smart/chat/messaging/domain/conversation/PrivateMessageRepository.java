package com.smart.chat.messaging.domain.conversation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 私信流水的仓储端口。
 * <p>
 * 取数口径全部沿用改造前 Mapper 的那几条：<b>双向会话谓词吃不到索引</b>，所以联系人列表的
 * 「最后一条」必须走 {@link #findLatestCreatedPerPeer} + {@link #findMessagesAtCreated} 两趟批量，
 * 不许退回 {@code findLatestBetween} 的逐条查；方法名与 Mapper 保持一致就是为了这条口径可被 grep 复核。
 */
public interface PrivateMessageRepository {

    Optional<PrivateMessage> findById(String id);

    /** 收藏夹/会话页的一次批量装配（原 selectBatchIds 口径，逐条查会退化成 N+1） */
    List<PrivateMessage> listByIds(Collection<String> ids);

    /** 新消息插入；已有消息只回写聚合持有的列（read_flag 由 {@link #markIncomingRead} 负责） */
    void save(PrivateMessage message);

    /** 我与每个对端之间「最后一条消息的时间点」，两趟 GROUP BY，与好友数无关 */
    Map<String, Long> findLatestCreatedPerPeer(String me);

    /** 按上一步的时间点把那些「最后一条」整行捞回来（两个方向各一趟，都是索引前缀等值 + IN） */
    List<PrivateMessage> findMessagesAtCreated(String me, Collection<String> peers, Collection<Long> timestamps);

    /** 倒序取一页（before 为游标：早于该时间），调用方自行反转为正序 */
    List<PrivateMessage> findConversationPage(String me, String peer, Long before, int limit);

    /** 会话内关键字搜索（只搜未撤回），倒序取最近 50 条 */
    List<PrivateMessage> searchConversation(String me, String peer, String keyword);

    /** 56 导出：全量拉取双向会话消息 */
    List<PrivateMessage> findConversationAll(String me, String peer);

    /** F36 心动时刻：我参与的、被标记的消息（可限定 peer） */
    List<PrivateMessage> findHeartMoments(String me, String peer);

    /** 81 全局消息搜索：我参与的全部会话里的未撤回文本 */
    List<PrivateMessage> searchGlobal(String me, String keyword);

    /** 95 会话附件：双向会话中指定类型（image/file）的消息 */
    List<PrivateMessage> findAttachments(String me, String peer, String msgType);

    /** 已读回执：把 from 发给 to 的未读消息全部标记已读（批量 UPDATE，不走 save） */
    void markIncomingRead(String from, String to);

    /** 85 清空聊天记录：删除双向会话全部消息，返回删除条数 */
    int deleteConversation(String me, String peer);
}
