package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.dine.DineTicket;
import com.smart.chat.couple.domain.dine.DineTicketRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 今晚饭桌（保留卡 `couple-dine-today`）单测：饭票撞菜推送与改票覆盖、裁决稳定与票池。
 * <p>
 * Service 改走 {@link DineTicketRepository} 端口后，这里用内存领域列表复刻 upsert/查询语义；
 * 期望值（菜名、命中、裁决、事件名）与改造前一字未改。insert-vs-update 的落库细节移到 {@code DineTicketRepositoryAdapterTest}。
 */
@ExtendWith(MockitoExtension.class)
class CoupleDiningServiceTest {

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private DineTicketRepository ticketRepository;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleDiningService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<DineTicket> tickets = new ArrayList<>();

    @BeforeEach
    void setUp() {
        lenient().when(ticketRepository.findBySpaceAndUserAndDay(eq("s1"), any(), any())).thenAnswer(inv -> {
            String user = inv.getArgument(1);
            String day = inv.getArgument(2);
            return tickets.stream().filter(t -> t.fromUser().equals(user) && t.day().equals(day)).findFirst();
        });
        lenient().when(ticketRepository.listByDay(eq("s1"), any())).thenAnswer(inv -> {
            String day = inv.getArgument(1);
            return tickets.stream().filter(t -> t.day().equals(day)).toList();
        });
        lenient().doAnswer(inv -> {
            DineTicket ticket = inv.getArgument(0);
            int idx = -1;
            for (int i = 0; i < tickets.size(); i++) {
                if (tickets.get(i).fromUser().equals(ticket.fromUser()) && tickets.get(i).day().equals(ticket.day())) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                DineTicket old = tickets.get(idx);
                tickets.set(idx, DineTicket.restore(old.id(), ticket.spaceId(), ticket.day(), ticket.fromUser(),
                        ticket.dish(), ticket.reason(), old.created()));
            } else {
                tickets.add(ticket);
            }
            return null;
        }).when(ticketRepository).save(any());
    }

    private CoupleSpace space() {
        return CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 0L, null, null, null, null, null, null, null);
    }

    private void stubSpace(String me) {
        lenient().when(spaceRepository.findActiveByMember(me)).thenReturn(Optional.of(space()));
    }

    private DineTicket ticket(String user, String dish) {
        return DineTicket.restore("t-" + user, "s1", DAY, user, dish, "", 0L);
    }

    // ========== F210 饭票 ==========

    @Test
    void firstTicketOfTodayIsInsertedAndNotifiesPartner() {
        stubSpace("alice");

        service.throwTicket("alice", " 番茄牛腩 ", "想你了的味道");

        ArgumentCaptor<DineTicket> cap = ArgumentCaptor.forClass(DineTicket.class);
        verify(ticketRepository).save(cap.capture());
        assertThat(cap.getValue().dish()).isEqualTo("番茄牛腩");
        assertThat(cap.getValue().fromUser()).isEqualTo("alice");
        verify(push).pushCoupleEvent(eq("dine-ticket"), any(), any(), any());
    }

    @Test
    void matchingDishesPushesBothDineHit() {
        stubSpace("alice");
        tickets.add(ticket("bob", "火锅"));

        CoupleDiningService.TodayVO vo = service.throwTicket("alice", "火锅", "撞上了！");

        verify(ticketRepository).save(any());
        verify(push).pushCoupleEventBoth(eq("dine-hit"), any(), any(), any(), any());
        assertThat(vo.hit()).isTrue();
        assertThat(vo.verdict()).isEqualTo("火锅");
    }

    @Test
    void secondTicketOfSameDayOverwritesViaUpdate() {
        stubSpace("alice");
        tickets.add(ticket("alice", "旧菜"));

        service.throwTicket("alice", "新菜", "");

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).id()).as("改票覆盖同一行，不新增").isEqualTo("t-alice");
        assertThat(tickets.get(0).dish()).isEqualTo("新菜");
    }

    @Test
    void blankTicketDishRejected() {
        stubSpace("alice");
        assertThatThrownBy(() -> service.throwTicket("alice", "   ", "x"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("想吃什么");
    }

    // ========== F211 裁决 ==========

    @Test
    void verdictIsStableAndFromTicketPool() {
        tickets.add(ticket("alice", "火锅"));
        tickets.add(ticket("bob", "寿司"));
        String first = service.verdictOf(space(), DAY);
        assertThat(first).isIn("火锅", "寿司");
        assertThat(service.verdictOf(space(), DAY)).isEqualTo(first);
    }

    @Test
    void verdictEmptyPoolReturnsNull() {
        assertThat(service.verdictOf(space(), DAY)).isNull();
    }

    // ========== 无空间 ==========

    @Test
    void noSpaceThrows404() {
        when(spaceRepository.findActiveByMember("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.today("solo"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("情侣空间");
    }
}
