package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.bond.BondAction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 贴贴流水适配器：流水只追加，所以这里锁两件事——插入写满五列、
 * 以及<b>永不回写</b>（不 update 也不 delete，改一次贴贴等于篡改流水）。
 * 顺带把动作目录的 emoji/label 与 PO 那份逐字符对齐（前端在按这套目录渲染）。
 */
@ExtendWith(MockitoExtension.class)
class ActionRepositoryAdapterTest {

    @Mock
    private CoupleActionMapper actionMapper;

    @InjectMocks
    private ActionRepositoryAdapter adapter;

    @Test
    void appendInsertsTheWholeRowAndNothingElse() {
        BondAction action = BondAction.sent("s1", "alice", "NUZZLE");

        adapter.append(action);

        ArgumentCaptor<CoupleActionPO> captor = ArgumentCaptor.forClass(CoupleActionPO.class);
        verify(actionMapper).insert(captor.capture());
        CoupleActionPO po = captor.getValue();
        assertThat(po.getId()).isEqualTo(action.id());
        assertThat(po.getSpaceId()).isEqualTo("s1");
        assertThat(po.getUsername()).isEqualTo("alice");
        assertThat(po.getKind()).isEqualTo("NUZZLE");
        assertThat(po.getCreated()).isEqualTo(action.created());
        verify(actionMapper, never()).updateById(any(CoupleActionPO.class));
    }

    @Test
    void readsReuseTheMapperDirectoryAndCounters() {
        when(actionMapper.findBySpace("s1")).thenReturn(List.of(row("a1", "bob", "HUG", 200L)));
        when(actionMapper.countByKind("s1", "HUG")).thenReturn(9L);
        when(actionMapper.countByKindAndUser("s1", "HUG", "bob")).thenReturn(5L);
        when(actionMapper.lastCreatedAt("s1", "HUG")).thenReturn(200L);

        assertThat(adapter.listBySpace("s1")).extracting(BondAction::kind).containsExactly("HUG");
        assertThat(adapter.listBySpace("s1").get(0).id()).isEqualTo("a1");
        assertThat(adapter.countByKind("s1", "HUG")).isEqualTo(9L);
        assertThat(adapter.countSentBy("s1", "HUG", "bob")).isEqualTo(5L);
        assertThat(adapter.lastSentAt("s1", "HUG")).isEqualTo(200L);
        assertThat(adapter.listBySpace("s1").get(0).sentBy("bob")).isTrue();
    }

    @Test
    void kindDirectoryStaysByteIdenticalToThePoDirectory() {
        for (String kind : new String[]{"POKE", "HUG", "KISS", "PAT", "NUZZLE", "TICKLE", "MISS", "RETIRED"}) {
            assertThat(BondAction.emojiOf(kind)).isEqualTo(CoupleActionPO.emojiOf(kind));
            assertThat(BondAction.labelOf(kind)).isEqualTo(CoupleActionPO.labelOf(kind));
            assertThat(BondAction.isValidKind(kind)).isEqualTo(CoupleActionPO.isValidKind(kind));
        }
    }

    private CoupleActionPO row(String id, String username, String kind, Long created) {
        CoupleActionPO po = new CoupleActionPO();
        po.setId(id);
        po.setSpaceId("s1");
        po.setUsername(username);
        po.setKind(kind);
        po.setCreated(created);
        return po;
    }
}
