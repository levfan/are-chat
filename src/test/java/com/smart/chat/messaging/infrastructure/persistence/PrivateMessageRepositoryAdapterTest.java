package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.conversation.PrivateMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 消息适配器的写回口径：{@code read_flag} 由已读回执那趟批量 UPDATE 负责，聚合只是读它来显示
 * 「TA 读了没」；用聚合重建整行会把已读位清回未读，红点重新亮起来。
 */
@ExtendWith(MockitoExtension.class)
class PrivateMessageRepositoryAdapterTest {

    @Mock
    private PrivateMessageMapper messageMapper;

    @InjectMocks
    private PrivateMessageRepositoryAdapter repository;

    @Test
    void updateKeepsTheReadStampAndTheConversationEndpoints() {
        PrivateMessagePO existing = new PrivateMessagePO();
        existing.setId("m1");
        existing.setFromUser("alice");
        existing.setToUser("bob");
        existing.setContent("原始内容");
        existing.setMsgType(PrivateMessage.TYPE_TEXT);
        existing.setStatus(PrivateMessage.STATUS_SENT);
        existing.setReadFlag(1);
        existing.setCreated(111L);
        when(messageMapper.selectById("m1")).thenReturn(existing);

        // 聚合是「撤回」场景：只带 content/status，其余字段在内存里是 null
        PrivateMessage recalled = PrivateMessage.restore("m1", "alice", "bob", "原始内容",
                PrivateMessage.TYPE_TEXT, PrivateMessage.STATUS_RECALLED, null, null, null, null, null);
        repository.save(recalled);

        ArgumentCaptor<PrivateMessagePO> captor = ArgumentCaptor.forClass(PrivateMessagePO.class);
        verify(messageMapper).updateById(captor.capture());
        PrivateMessagePO written = captor.getValue();
        assertThat(written.getReadFlag()).as("已读位归批量回执管，聚合不许清").isEqualTo(1);
        assertThat(written.getCreated()).as("发送时间不属于改写范围").isEqualTo(111L);
        assertThat(written.getFromUser()).isEqualTo("alice");
        assertThat(written.getToUser()).isEqualTo("bob");
        assertThat(written.getStatus()).isEqualTo(PrivateMessage.STATUS_RECALLED);
        verify(messageMapper, never()).insert(any(PrivateMessagePO.class));
    }

    @Test
    void newMessageIsInsertedWithItsIdentityAndSentStatus() {
        when(messageMapper.selectById(any())).thenReturn(null);
        PrivateMessage message = PrivateMessage.offer("alice", "bob", "在吗", PrivateMessage.TYPE_TEXT, 900L);

        repository.save(message);

        ArgumentCaptor<PrivateMessagePO> captor = ArgumentCaptor.forClass(PrivateMessagePO.class);
        verify(messageMapper).insert(captor.capture());
        PrivateMessagePO po = captor.getValue();
        assertThat(po.getId()).isEqualTo(message.id());
        assertThat(po.getContent()).isEqualTo("在吗");
        assertThat(po.getMsgType()).isEqualTo(PrivateMessage.TYPE_TEXT);
        assertThat(po.getStatus()).isEqualTo(PrivateMessage.STATUS_SENT);
        assertThat(po.getCreated()).isEqualTo(900L);
        verify(messageMapper, never()).updateById(any(PrivateMessagePO.class));
    }

    @Test
    void batchFetchSkipsTheQueryForEmptyEntries() {
        // 空集合会被拼成 IN ()，直接语法错；端口必须自己拦下来
        assertThat(repository.listByIds(List.of())).isEmpty();
        verify(messageMapper, never()).selectBatchIds(any());
    }

    @Test
    void batchFetchMapsRowsBackIntoMessages() {
        PrivateMessagePO po = new PrivateMessagePO();
        po.setId("m2");
        po.setFromUser("bob");
        po.setToUser("alice");
        po.setContent("晚上吃什么");
        po.setMsgType(PrivateMessage.TYPE_TEXT);
        po.setStatus(PrivateMessage.STATUS_SENT);
        po.setReadFlag(1);
        po.setHeartAt(7L);
        when(messageMapper.selectBatchIds(anyCollection())).thenReturn(List.of(po));

        PrivateMessage message = repository.listByIds(List.of("m2")).get(0);

        assertThat(message.id()).isEqualTo("m2");
        assertThat(message.readFlag()).isTrue();
        assertThat(message.readFlagRaw()).isEqualTo(1);
        assertThat(message.heartAt()).isEqualTo(7L);
    }

    @Test
    void conversationReadsAndDeletesDelegateStraightToTheMapperQueries() {
        // 这几条是取数口径的直通：适配器不许加自己的过滤条件
        when(messageMapper.findLatestCreatedPerPeer("alice")).thenReturn(java.util.Map.of("bob", 5L));
        assertThat(repository.findLatestCreatedPerPeer("alice")).containsEntry("bob", 5L);

        repository.markIncomingRead("bob", "alice");
        verify(messageMapper).markIncomingRead("bob", "alice");

        when(messageMapper.deleteConversation("alice", "bob")).thenReturn(3);
        assertThat(repository.deleteConversation("alice", "bob")).isEqualTo(3);

        repository.findConversationPage("alice", "bob", null, 20);
        verify(messageMapper).findConversationPage("alice", "bob", null, 20);

        repository.findAttachments("alice", "bob", PrivateMessage.TYPE_IMAGE);
        verify(messageMapper).findAttachments("alice", "bob", PrivateMessage.TYPE_IMAGE);
    }

    @Test
    void missingIdReadsAsEmptyInsteadOfNull() {
        when(messageMapper.selectById("nope")).thenReturn(null);

        assertThat(repository.findById("nope")).isEqualTo(Optional.empty());
    }
}
