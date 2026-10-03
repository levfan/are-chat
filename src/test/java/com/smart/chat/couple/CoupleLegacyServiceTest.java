package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 周年抽奖箱单测（系统裁剪后传世系统唯一保留项）：每人一年一次且第二格留给 TA、抽中必须真落库、
 * 奖池只吃本年 EARN 的台账条目（SPEND 与去年都不算）、一条没攒过才回落静态奖位、
 * 周年提醒走读时惰性结算且不重复推、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleLegacyServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleLegacyDrawMapper drawMapper;
    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleLegacyService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String YEAR = String.valueOf(LocalDate.now().getYear());

    private final List<CoupleLegacyDraw> draws = new ArrayList<>();
    private final List<CouplePointLedger> ledger = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        space.setAnniversary(DAY);
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        lenient().when(drawMapper.findByYear(eq("s1"), any())).thenAnswer(inv -> draws.stream()
                .filter(d -> d.getYear().equals(inv.getArgument(1)))
                .findFirst().orElse(null));
        lenient().when(drawMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(draws));
        lenient().when(drawMapper.insert(any(CoupleLegacyDraw.class))).thenAnswer(inv -> {
            draws.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(ledgerMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(ledger));
    }

    @Test
    void drawOnceEachAndAnniversaryReminderSettlesOnRead() {
        assertThat(draws).isEmpty();
        service.draw("alice");
        assertThat(draws).hasSize(1);
        assertThat(draws.get(0).getPrizeA()).isNotBlank();
        verify(push).pushCoupleEvent(eq("legacy-draw"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.draw("alice"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("抽过了");
        service.draw("bob");
        assertThat(draws.get(0).drawnBFlag()).isTrue();

        // 周年当天读时提醒（不建定时任务）
        draws.get(0).setNotified(0);
        service.legacy("alice");
        verify(push).pushCoupleEventBoth(eq("legacy-draw-remind"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThat(draws.get(0).notifiedFlag()).isTrue();
        service.legacy("alice");
        verify(push, times(1)).pushCoupleEventBoth(eq("legacy-draw-remind"), any(), any(), any(), any());

        CoupleLegacyService.DrawVO vo = service.legacy("alice").draw();
        assertThat(vo.year()).isEqualTo(YEAR);
        assertThat(vo.drawnMine()).isTrue();
        assertThat(vo.prizeMine()).isNotBlank();
        // 今年一条愿望都没攒过，才回落 Bank 的固定迷你愿望位
        assertThat(vo.prizeMine()).isIn(CoupleLegacyBank.PRIZES.toArray());
    }

    @Test
    void drawPoolEatsOnlyThisYearsEarnedWishes() {
        ledger.add(CouplePointLedger.of("s1", "alice", "EARN", "陪看一部老片", 5));
        ledger.add(CouplePointLedger.of("s1", "bob", "EARN", "一次不挑餐厅", 5));
        // SPEND 是花掉的花销、去年 EARN 是去年的愿望，都不该进今年的箱子
        ledger.add(CouplePointLedger.of("s1", "alice", "SPEND", "已经换掉的花销", 3));
        CouplePointLedger lastYear = CouplePointLedger.of("s1", "alice", "EARN", "去年的愿望", 5);
        lastYear.setCreated(LocalDate.of(LocalDate.now().getYear() - 1, 6, 1)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        ledger.add(lastYear);

        service.draw("alice");
        service.draw("bob");

        assertThat(draws.get(0).getPrizeA()).isIn("陪看一部老片", "一次不挑餐厅");
        assertThat(draws.get(0).getPrizeB()).isIn("陪看一部老片", "一次不挑餐厅");
        assertThat(draws.get(0).getPrizeA()).isNotEqualTo("去年的愿望");
    }

    @Test
    void secondSideKeepsItsOwnDrawAndNoSpaceIs404() {
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.draw("carol"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("情侣空间");
        assertThat(draws).isEmpty();
        verify(drawMapper, never()).updateById(any(CoupleLegacyDraw.class));
    }
}
