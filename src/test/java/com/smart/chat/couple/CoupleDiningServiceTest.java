package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 今晚饭桌（保留卡 `couple-dine-today`）单测：饭票撞菜推送与改票覆盖、裁决稳定与票池。
 * 星评/踩雷库/菜单/拿手菜/点单机/搭伙车/干饭账 随功能下线，对应用例一并删除。
 */
@ExtendWith(MockitoExtension.class)
class CoupleDiningServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleDineTicketMapper ticketMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleDiningService service;

    private static final String DAY = LocalDate.now().toString();

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        return space;
    }

    private void stubSpace(String me) {
        lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(space()));
    }

    private CoupleDineTicket ticket(String user, String dish) {
        return CoupleDineTicket.of("s1", DAY, user, dish, "");
    }

    // ========== F210 饭票 ==========

    @Test
    void firstTicketOfTodayIsInsertedAndNotifiesPartner() {
        stubSpace("alice");
        when(ticketMapper.find("s1", DAY, "alice")).thenReturn(null);
        when(ticketMapper.find("s1", DAY, "bob")).thenReturn(null);

        service.throwTicket("alice", " 番茄牛腩 ", "想你了的味道");

        ArgumentCaptor<CoupleDineTicket> cap = ArgumentCaptor.forClass(CoupleDineTicket.class);
        verify(ticketMapper).insert(cap.capture());
        assertThat(cap.getValue().getDish()).isEqualTo("番茄牛腩");
        assertThat(cap.getValue().getFromUser()).isEqualTo("alice");
        verify(push).pushCoupleEvent(eq("dine-ticket"), any(), any(), any());
    }

    @Test
    void matchingDishesPushesBothDineHit() {
        stubSpace("alice");
        when(ticketMapper.find("s1", DAY, "alice")).thenReturn(null, ticket("alice", "火锅"));
        when(ticketMapper.find("s1", DAY, "bob")).thenReturn(ticket("bob", "火锅"));
        when(ticketMapper.findByDay("s1", DAY)).thenReturn(List.of(ticket("alice", "火锅"), ticket("bob", "火锅")));

        CoupleDiningService.TodayVO vo = service.throwTicket("alice", "火锅", "撞上了！");

        verify(ticketMapper).insert(any(CoupleDineTicket.class));
        verify(push).pushCoupleEventBoth(eq("dine-hit"), any(), any(), any(), any());
        assertThat(vo.hit()).isTrue();
        assertThat(vo.verdict()).isEqualTo("火锅");
    }

    @Test
    void secondTicketOfSameDayOverwritesViaUpdate() {
        stubSpace("alice");
        CoupleDineTicket existing = ticket("alice", "旧菜");
        when(ticketMapper.find("s1", DAY, "alice")).thenReturn(existing);
        when(ticketMapper.find("s1", DAY, "bob")).thenReturn(null);

        service.throwTicket("alice", "新菜", "");

        verify(ticketMapper, never()).insert(any(CoupleDineTicket.class));
        verify(ticketMapper).updateById(any(CoupleDineTicket.class));
        assertThat(existing.getDish()).isEqualTo("新菜");
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
        when(ticketMapper.findByDay("s1", DAY))
                .thenReturn(List.of(ticket("alice", "火锅"), ticket("bob", "寿司")));
        String first = service.verdictOf(space(), DAY);
        assertThat(first).isIn("火锅", "寿司");
        assertThat(service.verdictOf(space(), DAY)).isEqualTo(first);
    }

    @Test
    void verdictEmptyPoolReturnsNull() {
        when(ticketMapper.findByDay("s1", DAY)).thenReturn(List.of());
        assertThat(service.verdictOf(space(), DAY)).isNull();
    }

    // ========== 无空间 ==========

    @Test
    void noSpaceThrows404() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.today("solo"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("情侣空间");
    }
}
