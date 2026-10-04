package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.dine.DineTicket;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 饭票仓储适配器：验证「只回写聚合纳管的列」这条纪律。
 * 改票（同一 space/day/from_user 再投）只覆盖 dish/reason，id、归属、created 必须原样留着——
 * 用聚合整行重建会把 created 刷成新时刻、把 id 换成新 UUID，这类破坏编译和业务用例都照不出来。
 */
@ExtendWith(MockitoExtension.class)
class DineTicketRepositoryAdapterTest {

    @Mock
    private CoupleDineTicketMapper ticketMapper;

    @InjectMocks
    private DineTicketRepositoryAdapter adapter;

    @Test
    void newTicketIsInsertedWithAllColumns() {
        when(ticketMapper.find("s1", "2026-10-05", "alice")).thenReturn(null);
        DineTicket ticket = DineTicket.offer("s1", "2026-10-05", "alice", "番茄牛腩", "想你了");

        adapter.save(ticket);

        ArgumentCaptor<CoupleDineTicketPO> captor = ArgumentCaptor.forClass(CoupleDineTicketPO.class);
        verify(ticketMapper).insert(captor.capture());
        CoupleDineTicketPO po = captor.getValue();
        assertThat(po.getId()).isEqualTo(ticket.id());
        assertThat(po.getSpaceId()).isEqualTo("s1");
        assertThat(po.getDay()).isEqualTo("2026-10-05");
        assertThat(po.getFromUser()).isEqualTo("alice");
        assertThat(po.getDish()).isEqualTo("番茄牛腩");
        assertThat(po.getReason()).isEqualTo("想你了");
        verify(ticketMapper, never()).updateById(any(CoupleDineTicketPO.class));
    }

    @Test
    void rewriteOnlyTouchesDishAndReason() {
        CoupleDineTicketPO stored = new CoupleDineTicketPO();
        stored.setId("t1");
        stored.setSpaceId("s1");
        stored.setDay("2026-10-05");
        stored.setFromUser("alice");
        stored.setDish("旧菜");
        stored.setReason("旧理由");
        stored.setCreated(111L);
        when(ticketMapper.find("s1", "2026-10-05", "alice")).thenReturn(stored);

        // 覆盖同一张票：service 每次投的都是「新 id/新 created」的聚合，适配器按 uk 命中后只改 dish/reason
        adapter.save(DineTicket.offer("s1", "2026-10-05", "alice", "新菜", "新理由"));

        ArgumentCaptor<CoupleDineTicketPO> captor = ArgumentCaptor.forClass(CoupleDineTicketPO.class);
        verify(ticketMapper).updateById(captor.capture());
        CoupleDineTicketPO written = captor.getValue();
        assertThat(written.getDish()).as("菜名是改票真的覆盖的列").isEqualTo("新菜");
        assertThat(written.getReason()).isEqualTo("新理由");
        assertThat(written.getId()).isEqualTo("t1");
        assertThat(written.getSpaceId()).isEqualTo("s1");
        assertThat(written.getDay()).isEqualTo("2026-10-05");
        assertThat(written.getFromUser()).isEqualTo("alice");
        assertThat(written.getCreated()).isEqualTo(111L);
        verify(ticketMapper, never()).insert(any(CoupleDineTicketPO.class));
    }

    @Test
    void queriesMapRowsBackIntoTickets() {
        CoupleDineTicketPO row = new CoupleDineTicketPO();
        row.setId("t2");
        row.setSpaceId("s1");
        row.setDay("2026-10-05");
        row.setFromUser("bob");
        row.setDish("寿司");
        row.setReason("");
        row.setCreated(200L);
        when(ticketMapper.findByDay("s1", "2026-10-05")).thenReturn(List.of(row));
        when(ticketMapper.find("s1", "2026-10-05", "bob")).thenReturn(row);

        assertThat(adapter.listByDay("s1", "2026-10-05")).extracting(DineTicket::dish).containsExactly("寿司");
        Optional<DineTicket> found = adapter.findBySpaceAndUserAndDay("s1", "bob", "2026-10-05");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().id()).isEqualTo("t2");
        assertThat(found.orElseThrow().created()).isEqualTo(200L);
    }
}
