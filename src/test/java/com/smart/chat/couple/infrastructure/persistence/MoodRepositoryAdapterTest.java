package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.mood.Mood;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 心情日记仓储适配器：本片只有读法（写入用例还在 CoupleService.setMood），
 * 所以这里锁两件事——每列都翻译到位（含 note 与 updatedAt 这两个可空列）、
 * 以及适配器<b>一次都不写库</b>。
 */
@ExtendWith(MockitoExtension.class)
class MoodRepositoryAdapterTest {

    @Mock
    private CoupleMoodMapper moodMapper;

    @InjectMocks
    private MoodRepositoryAdapter adapter;

    @Test
    void findBySpaceAndUserOnTranslatesEveryColumn() {
        CoupleMoodPO po = new CoupleMoodPO();
        po.setId("m1");
        po.setSpaceId("s1");
        po.setUsername("alice");
        po.setMoodDay("2026-10-05");
        po.setMood("SAD");
        po.setNote("今天很难");
        po.setCreated(8L);
        po.setUpdatedAt(9L);
        when(moodMapper.find("s1", "alice", "2026-10-05")).thenReturn(Optional.of(po));

        Optional<Mood> found = adapter.findBySpaceAndUserOn("s1", "alice", "2026-10-05");

        assertThat(found).isPresent();
        Mood mood = found.orElseThrow();
        assertThat(mood.id()).isEqualTo("m1");
        assertThat(mood.username()).isEqualTo("alice");
        assertThat(mood.moodDay()).isEqualTo("2026-10-05");
        assertThat(mood.note()).isEqualTo("今天很难");
        assertThat(mood.created()).isEqualTo(8L);
        assertThat(mood.updatedAt()).isEqualTo(9L);
        assertThat(mood.isDowncast()).isTrue();
    }

    @Test
    void listBySpaceKeepsRowsAndToleratesBlankColumns() {
        CoupleMoodPO po = new CoupleMoodPO();
        po.setId("m2");
        po.setSpaceId("s1");
        po.setUsername("bob");
        po.setMoodDay("2026-10-04");
        po.setMood("HAPPY");
        when(moodMapper.findBySpace("s1")).thenReturn(List.of(po));

        List<Mood> moods = adapter.listBySpace("s1");

        assertThat(moods).hasSize(1);
        assertThat(moods.get(0).recordedBy("bob")).isTrue();
        assertThat(moods.get(0).note()).isNull();
        assertThat(moods.get(0).created()).isNull();
    }

    @Test
    void missingRowReadsAsEmpty() {
        when(moodMapper.find("s1", "ghost", "2026-10-05")).thenReturn(Optional.empty());

        assertThat(adapter.findBySpaceAndUserOn("s1", "ghost", "2026-10-05")).isEmpty();
    }

    @Test
    void adapterNeverWrites() {
        CoupleMoodPO po = new CoupleMoodPO();
        po.setId("m3");
        po.setSpaceId("s1");
        po.setUsername("alice");
        po.setMoodDay("2026-10-05");
        po.setMood("CALM");
        when(moodMapper.findBySpace("s1")).thenReturn(List.of(po));

        adapter.listBySpace("s1");
        adapter.findBySpaceAndUserOn("s1", "alice", "2026-10-05");

        verify(moodMapper, never()).insert(any(CoupleMoodPO.class));
        verify(moodMapper, never()).updateById(any(CoupleMoodPO.class));
    }
}
