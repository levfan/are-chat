package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.mood.MoodReaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 心情回应仓储适配器：验证「只回写聚合纳管的列」这条纪律。
 * 空间、心情日、回应人、创建时刻都不归聚合改，用聚合重建整行会把它们静默改写；
 * 另外把回应目录的 emoji/label 与 PO 那份逐字符对齐（推送文案在用）。
 */
@ExtendWith(MockitoExtension.class)
class MoodReactionRepositoryAdapterTest {

    @Mock
    private CoupleMoodReactionMapper moodReactionMapper;

    @InjectMocks
    private MoodReactionRepositoryAdapter adapter;

    @Test
    void firstReactionIsInsertedWithAllColumns() {
        when(moodReactionMapper.selectById(any())).thenReturn(null);
        MoodReaction reaction = MoodReaction.give("s1", "2026-10-04", "alice", "PAT");

        adapter.save(reaction);

        ArgumentCaptor<CoupleMoodReactionPO> captor = ArgumentCaptor.forClass(CoupleMoodReactionPO.class);
        verify(moodReactionMapper).insert(captor.capture());
        CoupleMoodReactionPO po = captor.getValue();
        assertThat(po.getId()).isEqualTo(reaction.id());
        assertThat(po.getSpaceId()).isEqualTo("s1");
        assertThat(po.getMoodDay()).isEqualTo("2026-10-04");
        assertThat(po.getFromUser()).isEqualTo("alice");
        assertThat(po.getReaction()).isEqualTo("PAT");
        assertThat(po.getCreated()).isEqualTo(reaction.created());
        assertThat(po.getUpdatedAt()).as("第一次贴回应不留修改时刻").isNull();
        verify(moodReactionMapper, never()).updateById(any(CoupleMoodReactionPO.class));
    }

    @Test
    void updateOnlyWritesReactionAndUpdatedAt() {
        CoupleMoodReactionPO stored = stored("r1", "2026-10-04", "alice", "HUG");
        when(moodReactionMapper.selectById("r1")).thenReturn(stored);
        MoodReaction reaction = MoodReaction.restore("r1", "s1", "2026-10-04", "alice", "HUG", 111L, 111L);
        reaction.revise("CHEER");

        adapter.save(reaction);

        ArgumentCaptor<CoupleMoodReactionPO> captor = ArgumentCaptor.forClass(CoupleMoodReactionPO.class);
        verify(moodReactionMapper).updateById(captor.capture());
        CoupleMoodReactionPO written = captor.getValue();
        assertThat(written.getReaction()).as("回应是聚合唯一真的改动的业务列").isEqualTo("CHEER");
        assertThat(written.getUpdatedAt()).isNotEqualTo(111L);
        assertThat(written.getMoodDay()).isEqualTo("2026-10-04");
        assertThat(written.getFromUser()).isEqualTo("alice");
        assertThat(written.getSpaceId()).isEqualTo("s1");
        assertThat(written.getCreated()).isEqualTo(111L);
        verify(moodReactionMapper, never()).insert(any(CoupleMoodReactionPO.class));
    }

    @Test
    void findMapsRowBackIntoReaction() {
        CoupleMoodReactionPO row = stored("r2", "2026-10-03", "bob", "KISS");
        when(moodReactionMapper.find("s1", "2026-10-03", "bob")).thenReturn(row);
        when(moodReactionMapper.find("s1", "2026-10-03", "ghost")).thenReturn(null);

        Optional<MoodReaction> found = adapter.findBySpaceAndUserOn("s1", "2026-10-03", "bob");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().reaction()).isEqualTo("KISS");
        assertThat(found.orElseThrow().updatedAt()).isEqualTo(111L);
        assertThat(adapter.findBySpaceAndUserOn("s1", "2026-10-03", "ghost")).isEmpty();
    }

    @Test
    void reactionDirectoryStaysByteIdenticalToThePoDirectory() {
        for (String key : new String[]{"HUG", "KISS", "CHEER", "PAT", "RETIRED"}) {
            MoodReaction reaction = MoodReaction.restore("r-" + key, "s1", "2026-10-03", "alice", key, 1L, null);
            assertThat(CoupleMoodReactionPO.labelOf(reaction.reaction())).isEqualTo(MoodReaction.labelOf(key));
            assertThat(CoupleMoodReactionPO.emojiOf(reaction.reaction())).isEqualTo(MoodReaction.emojiOf(key));
            assertThat(CoupleMoodReactionPO.isValidReaction(key)).isEqualTo(MoodReaction.isValidReaction(key));
        }
    }

    private CoupleMoodReactionPO stored(String id, String moodDay, String fromUser, String reaction) {
        CoupleMoodReactionPO po = new CoupleMoodReactionPO();
        po.setId(id);
        po.setSpaceId("s1");
        po.setMoodDay(moodDay);
        po.setFromUser(fromUser);
        po.setReaction(reaction);
        po.setCreated(111L);
        po.setUpdatedAt(111L);
        return po;
    }
}
