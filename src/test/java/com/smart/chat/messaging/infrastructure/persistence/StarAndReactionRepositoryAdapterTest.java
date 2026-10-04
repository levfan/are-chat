package com.smart.chat.messaging.infrastructure.persistence;

import com.smart.chat.messaging.domain.reaction.MessageReaction;
import com.smart.chat.messaging.domain.star.MessageStar;
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

/** 两条流水的适配器：只做双向翻译与「空集合不发查询」这一件事，没有更新路径可写坏。 */
@ExtendWith(MockitoExtension.class)
class StarAndReactionRepositoryAdapterTest {

    @Mock
    private MessageStarMapper starMapper;

    @Mock
    private MessageReactionMapper reactionMapper;

    @InjectMocks
    private MessageStarRepositoryAdapter starRepository;

    @InjectMocks
    private MessageReactionRepositoryAdapter reactionRepository;

    @Test
    void starIsInsertedWithItsOwnIdAndReadsBack() {
        MessageStar star = MessageStar.add("alice", "m1", 7L);

        starRepository.save(star);

        ArgumentCaptor<MessageStarPO> captor = ArgumentCaptor.forClass(MessageStarPO.class);
        verify(starMapper).insert(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("alice");
        assertThat(captor.getValue().getMsgId()).isEqualTo("m1");
        assertThat(captor.getValue().getCreated()).isEqualTo(7L);
        assertThat(captor.getValue().getId()).isEqualTo(star.id());

        MessageStarPO po = new MessageStarPO();
        po.setId("s1");
        po.setUsername("alice");
        po.setMsgId("m1");
        po.setCreated(7L);
        when(starMapper.findByUsername("alice")).thenReturn(List.of(po));
        assertThat(starRepository.listByUsername("alice")).extracting(MessageStar::id).containsExactly("s1");
    }

    @Test
    void starBatchReadSkipsEmptyIdsAndDelegationCoversTheToggle() {
        assertThat(starRepository.listByUsernameAndMsgIds("alice", List.of())).isEmpty();
        verify(starMapper, never()).findByUsernameAndMsgIds(any(), anyCollection());

        MessageStarPO po = new MessageStarPO();
        po.setId("s2");
        po.setUsername("alice");
        po.setMsgId("m1");
        when(starMapper.findUnique("alice", "m1")).thenReturn(po);
        assertThat(starRepository.findByUsernameAndMsgId("alice", "m1").orElseThrow().id()).isEqualTo("s2");

        starRepository.deleteById("s2");
        verify(starMapper).deleteById("s2");
    }

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
