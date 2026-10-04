package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.conversation.PrivateMessage;
import com.smart.chat.messaging.domain.conversation.PrivateMessageRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link PrivateMessageRepository} 的 MyBatis-Plus 适配器。
 * <p>
 * PO ↔ 领域的双向翻译只发生在这里；原先写在 {@link PrivateMessageMapper} 的 default 查询
 * （含「双向谓词吃不到索引，取数走 {@code findLatestCreatedPerPeer}」这条口径）原样留在 Mapper，
 * 这里只调它，Mapper 不感知领域类型。
 * <p>
 * 更新<b>只回写聚合持有的列</b>：{@code read_flag} 由 {@link #markIncomingRead} 批量刷
 * （已读回执一次改一整会话），聚合读它只为显示「TA 读了没」。用聚合重建整行会把已读位清回未读。
 */
@Component
public class PrivateMessageRepositoryAdapter implements PrivateMessageRepository {

    private final PrivateMessageMapper messageMapper;

    public PrivateMessageRepositoryAdapter(PrivateMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public Optional<PrivateMessage> findById(String id) {
        return Optional.ofNullable(messageMapper.selectById(id)).map(PrivateMessageRepositoryAdapter::toDomain);
    }

    @Override
    public List<PrivateMessage> listByIds(Collection<String> ids) {
        if (ids.isEmpty()) {
            // 空集合会被拼成 IN ()，直接语法错（与 MessageStarMapper 的批量读法同口径）
            return List.of();
        }
        return messageMapper.selectBatchIds(ids).stream().map(PrivateMessageRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(PrivateMessage message) {
        PrivateMessagePO existing = messageMapper.selectById(message.id());
        if (existing == null) {
            PrivateMessagePO po = new PrivateMessagePO();
            po.setId(message.id());
            po.setFromUser(message.fromUser());
            po.setToUser(message.toUser());
            po.setContent(message.content());
            po.setMsgType(message.msgType());
            po.setStatus(message.status());
            po.setReplyToId(message.replyToId());
            po.setReadFlag(message.readFlagRaw());
            po.setEdited(message.editedRaw());
            po.setHeartAt(message.heartAt());
            po.setCreated(message.created());
            messageMapper.insert(po);
            return;
        }
        applyOwnedFields(existing, message);
        messageMapper.updateById(existing);
    }

    /** 聚合负责维护的列；收发双方、类型与发送时间是消息的身份，read_flag 归已读回执，这里都不碰 */
    private static void applyOwnedFields(PrivateMessagePO po, PrivateMessage message) {
        po.setContent(message.content());
        po.setStatus(message.status());
        po.setReplyToId(message.replyToId());
        po.setEdited(message.editedRaw());
        po.setHeartAt(message.heartAt());
    }

    @Override
    public Map<String, Long> findLatestCreatedPerPeer(String me) {
        return messageMapper.findLatestCreatedPerPeer(me);
    }

    @Override
    public List<PrivateMessage> findMessagesAtCreated(String me, Collection<String> peers,
                                                      Collection<Long> timestamps) {
        return messageMapper.findMessagesAtCreated(me, peers, timestamps).stream()
                .map(PrivateMessageRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<PrivateMessage> findConversationPage(String me, String peer, Long before, int limit) {
        return toDomainList(messageMapper.findConversationPage(me, peer, before, limit));
    }

    @Override
    public List<PrivateMessage> searchConversation(String me, String peer, String keyword) {
        return toDomainList(messageMapper.searchConversation(me, peer, keyword));
    }

    @Override
    public List<PrivateMessage> findConversationAll(String me, String peer) {
        return toDomainList(messageMapper.findConversationAll(me, peer));
    }

    @Override
    public List<PrivateMessage> findHeartMoments(String me, String peer) {
        return toDomainList(messageMapper.findHeartMoments(me, peer));
    }

    @Override
    public List<PrivateMessage> searchGlobal(String me, String keyword) {
        return toDomainList(messageMapper.searchGlobal(me, keyword));
    }

    @Override
    public List<PrivateMessage> findAttachments(String me, String peer, String msgType) {
        return toDomainList(messageMapper.findAttachments(me, peer, msgType));
    }

    @Override
    public void markIncomingRead(String from, String to) {
        messageMapper.markIncomingRead(from, to);
    }

    @Override
    public int deleteConversation(String me, String peer) {
        return messageMapper.deleteConversation(me, peer);
    }

    private static List<PrivateMessage> toDomainList(List<PrivateMessagePO> rows) {
        return rows.stream().map(PrivateMessageRepositoryAdapter::toDomain).toList();
    }

    private static PrivateMessage toDomain(PrivateMessagePO po) {
        return PrivateMessage.restore(po.getId(), po.getFromUser(), po.getToUser(), po.getContent(), po.getMsgType(),
                po.getStatus(), po.getReplyToId(), po.getReadFlag(), po.getEdited(), po.getHeartAt(), po.getCreated());
    }
}
