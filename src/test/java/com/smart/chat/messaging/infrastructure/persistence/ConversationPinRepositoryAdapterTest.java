package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.pin.Conversation;
import com.smart.chat.messaging.domain.pin.ConversationPin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 置顶适配器的写入口径：「一个会话最多一条」这件事在改造前写在 Service 里（先 deleteForConversation
 * 再 insert），收口后由 {@code replace} 承担——所以删与插的顺序与参数必须原样锁住。
 */
@ExtendWith(MockitoExtension.class)
class ConversationPinRepositoryAdapterTest {

    @Mock
    private ConversationPinMapper pinMapper;

    @InjectMocks
    private ConversationPinRepositoryAdapter repository;

    @Test
    void replaceClearsThePreviousPinBeforeInsertingTheNewOne() {
        ConversationPin pin = ConversationPin.by("bob", "alice", "m2", 500L);

        repository.replace(pin);

        InOrder order = Mockito.inOrder(pinMapper);
        order.verify(pinMapper).deleteForConversation("alice", "bob");
        ArgumentCaptor<ConversationPinPO> captor = ArgumentCaptor.forClass(ConversationPinPO.class);
        order.verify(pinMapper).insert(captor.capture());
        ConversationPinPO written = captor.getValue();
        assertThat(written.getUserA()).isEqualTo("alice");
        assertThat(written.getUserB()).isEqualTo("bob");
        assertThat(written.getMsgId()).isEqualTo("m2");
        assertThat(written.getCreatedBy()).isEqualTo("bob");
        assertThat(written.getCreated()).isEqualTo(500L);
        assertThat(written.getId()).isEqualTo(pin.id());
    }

    @Test
    void unpinDeletesByCanonicalPairOnly() {
        Conversation conversation = Conversation.between("bob", "alice");

        repository.clear(conversation);

        verify(pinMapper).deleteForConversation("alice", "bob");
    }

    @Test
    void storedRowComesBackAsADomainPin() {
        ConversationPinPO po = new ConversationPinPO();
        po.setId("p1");
        po.setUserA("alice");
        po.setUserB("bob");
        po.setMsgId("m9");
        po.setCreatedBy("bob");
        po.setCreated(1L);
        when(pinMapper.findForConversation("alice", "bob")).thenReturn(Optional.of(po));

        ConversationPin pin = repository.findFor(Conversation.between("bob", "alice")).orElseThrow();

        assertThat(pin.id()).isEqualTo("p1");
        assertThat(pin.msgId()).isEqualTo("m9");
        assertThat(pin.createdBy()).isEqualTo("bob");
    }

}
