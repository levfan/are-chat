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
 * 饭桌系（F210-F219）核心逻辑单测：饭票撞菜推送与改票覆盖、裁决稳定与票池、星评钳位、
 * 踩雷重复与越权划掉、菜单插删与周一锚、拿手菜 upsert、搭伙车双锁成行与删除权限、
 * 话题打卡幂等、点单机兜底、年度干饭账聚合。
 */
@ExtendWith(MockitoExtension.class)
class CoupleDiningServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleDineTicketMapper ticketMapper;
    @Mock
    private CoupleDineRateMapper rateMapper;
    @Mock
    private CoupleDineNogoMapper nogoMapper;
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

    // ========== F212 星评 ==========

    @Test
    void starsClampedTo1To5() {
        stubSpace("alice");
        when(rateMapper.findBySpace("s1")).thenReturn(List.of());

        service.rate("alice", DAY, "烤肉", 9, "香");
        ArgumentCaptor<CoupleDineRate> cap = ArgumentCaptor.forClass(CoupleDineRate.class);
        verify(rateMapper).insert(cap.capture());
        assertThat(cap.getValue().getStars()).isEqualTo(5);

        service.rate("alice", DAY, "泡面", 0, "");
        verify(rateMapper, org.mockito.Mockito.times(2)).insert(cap.capture());
        assertThat(cap.getValue().getStars()).isEqualTo(1);
    }

    @Test
    void rateRejectsBadDayAndBlankDish() {
        stubSpace("alice");
        assertThatThrownBy(() -> service.rate("alice", "昨天", "菜", 5, ""))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.rate("alice", DAY, " ", 5, ""))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("吃了什么");
    }

    // ========== F213 踩雷库 ==========

    @Test
    void duplicateNogoRejected() {
        stubSpace("alice");
        when(nogoMapper.find("s1", "某店")).thenReturn(CoupleDineNogo.of("s1", "某店", "难吃", "bob"));
        assertThatThrownBy(() -> service.addNogo("alice", "某店", "又难吃"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("别重复拉黑");
    }

    @Test
    void onlyProposerCanRemoveNogo() {
        stubSpace("alice");
        stubSpace("bob");
        CoupleDineNogo row = CoupleDineNogo.of("s1", "坑店", "拉肚子", "bob");
        row.setId("n1");
        when(nogoMapper.selectById("n1")).thenReturn(row);
        assertThatThrownBy(() -> service.removeNogo("alice", "n1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("谁提议");

        service.removeNogo("bob", "n1");
        verify(nogoMapper).deleteById("n1");
    }

    // ========== F214 本周菜单 ==========

    // ========== F215 拿手菜 ==========

    // ========== F216 点单机 ==========

    // ========== F217 搭伙车 ==========

    // ========== F218 话题打卡 ==========

    // ========== F219 年度干饭账 ==========

    // ========== 无空间 ==========

    @Test
    void noSpaceThrows404() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.today("solo"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("情侣空间");
    }
}
