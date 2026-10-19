package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.reaction.MessageReaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 表情回应适配器：只做双向翻译与「空集合不发查询」这一件事。 */
@ExtendWith(MockitoExtension.class)
class MessageReactionRepositoryAdapterTest {

    @Mock
    private MessageReactionMapper reactionMapper;

    @InjectMocks
    private MessageReactionRepositoryAdapter reactionRepository;

    @Test
    void reactionIsInsertedReadsBackAndDeletesByToggle() {
        MessageReaction reaction = MessageReaction.add("m1", "alice", "👍", 3L);

        reactionRepository.save(reaction);

        ArgumentCaptor<MessageReactionPO> captor = ArgumentCaptor.forClass(MessageReactionPO.class);
        verify(reactionMapper).insert(captor.capture());
        assertThat(captor.getValue().getEmoji()).isEqualTo("👍");
        assertThat(captor.getValue().getMsgId()).isEqualTo("m1");
        assertThat(captor.getValue().getCreated()).isEqualTo(3L);

        MessageReactionPO po = new MessageReactionPO();
        po.setId("r1");
        po.setMsgId("m1");
        po.setUsername("alice");
        po.setEmoji("👍");
        when(reactionMapper.findByMsgIds(anyCollection())).thenReturn(List.of(po));
        assertThat(reactionRepository.listByMsgIds(List.of("m1")))
                .extracting(MessageReaction::id, MessageReaction::emoji).containsExactly(
                        org.assertj.core.groups.Tuple.tuple("r1", "👍"));

        when(reactionMapper.findUnique("m1", "alice", "👍")).thenReturn(null);
        assertThat(reactionRepository.findByMsgIdAndUserAndEmoji("m1", "alice", "👍")).isEmpty();

        reactionRepository.deleteById("r1");
        verify(reactionMapper).deleteById("r1");
    }

    @Test
    void reactionBatchReadSkipsEmptyIds() {
        assertThat(reactionRepository.listByMsgIds(List.of())).isEmpty();
        verify(reactionMapper, never()).findByMsgIds(any());
    }
}
