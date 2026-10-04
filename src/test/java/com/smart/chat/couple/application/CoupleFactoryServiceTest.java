package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerPO;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpacePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpinTaskPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpinTaskMapper;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
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
 * 家务轮盘单测（系统裁剪后二人制造厂唯一保留项）：一周一转、交替分配、自认不算双签、
 * 未认账不许打勾、抢别人的活 400、重复打勾不重复计分，以及本模块的积分口径——
 * 干完 +3 归干的人、本周全清双方各 +2，且必须真插进台账而不是只改返回值。
 */
@ExtendWith(MockitoExtension.class)
class CoupleFactoryServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleSpinTaskMapper spinMapper;
    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleFactoryService service;

    private final List<CoupleSpinTaskPO> tasks = new ArrayList<>();
    private final List<CouplePointLedgerPO> ledger = new ArrayList<>();
    private final String week = LocalDate.now().with(DayOfWeek.MONDAY).toString();

    @BeforeEach
    void setUp() {
        CoupleSpacePO space = new CoupleSpacePO();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpacePO.STATUS_ACTIVE);
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        lenient().when(spinMapper.findByWeek(eq("s1"), any())).thenAnswer(inv -> tasks.stream()
                .filter(t -> t.getWeek().equals(inv.getArgument(1))).toList());
        lenient().when(spinMapper.insert(any(CoupleSpinTaskPO.class))).thenAnswer(inv -> {
            tasks.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(spinMapper.selectById(any())).thenAnswer(inv -> tasks.stream()
                .filter(t -> t.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        lenient().when(spinMapper.updateById(any(CoupleSpinTaskPO.class))).thenAnswer(inv -> 1);
        lenient().when(ledgerMapper.insert(any(CouplePointLedgerPO.class))).thenAnswer(inv -> {
            ledger.add(inv.getArgument(0));
            return 1;
        });
    }

    private void spinTwo() {
        service.spin("alice", "洗碗,拖地");
    }

    @Test
    void spinAssignsAlternatelyAndBlocksSecondSpin() {
        spinTwo();
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).getAssignedUser()).isNotEqualTo(tasks.get(1).getAssignedUser());
        verify(push).pushCoupleEventBoth(eq("factory-spin-open"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(this::spinTwo)
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经转过盘");
    }

    @Test
    void ownConfirmationInvalidAndUnconfirmedDoneBlocked() {
        spinTwo();
        CoupleSpinTaskPO mine = tasks.stream()
                .filter(t -> t.getAssignedUser().equals("alice")).findFirst().orElseThrow();
        // 自己的活自己认不算双签
        assertThatThrownBy(() -> service.confirmSpin("alice", mine.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己的活自己认");
        // 没认账之前，干了也白干
        assertThatThrownBy(() -> service.doneSpin("alice", mine.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("先等对方认账");
        assertThat(ledger).isEmpty();

        service.confirmSpin("bob", mine.getId());
        assertThat(mine.confirmedFlag()).isTrue();
        // 抢 TA 的活
        assertThatThrownBy(() -> service.doneSpin("bob", mine.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("这活不是你的");
    }

    @Test
    void doneEarnsPointsOnceAndClearRewardsBoth() {
        spinTwo();
        CoupleSpinTaskPO a = tasks.stream().filter(t -> t.getAssignedUser().equals("alice")).findFirst().orElseThrow();
        CoupleSpinTaskPO b = tasks.stream().filter(t -> t.getAssignedUser().equals("bob")).findFirst().orElseThrow();
        service.confirmSpin("bob", a.getId());
        service.confirmSpin("alice", b.getId());

        service.doneSpin("alice", a.getId());
        // 干完那一格：只给干活的人记一笔 +3
        assertThat(ledger).hasSize(1);
        assertThat(ledger.get(0).getFromUser()).isEqualTo("alice");
        assertThat(ledger.get(0).getType()).isEqualTo(CouplePointLedgerPO.TYPE_EARN);
        assertThat(ledger.get(0).getPoints()).isEqualTo(CoupleFactoryService.SPIN_DONE_POINTS);
        assertThat(ledger.get(0).getItem()).startsWith(CoupleFactoryService.SPIN_DONE_PREFIX);
        // 本周没全清，不该发清空奖
        assertThat(ledger).noneMatch(l -> CoupleFactoryService.SPIN_CLEAR_REASON.equals(l.getItem()));

        service.doneSpin("bob", b.getId());
        // 第二格干完：bob 自己的 +3，加上双方各一笔清空 +2
        assertThat(ledger).hasSize(4);
        assertThat(ledger).filteredOn(l -> CoupleFactoryService.SPIN_CLEAR_REASON.equals(l.getItem()))
                .extracting(CouplePointLedgerPO::getFromUser).containsExactlyInAnyOrder("alice", "bob");
        verify(push).pushCoupleEventBoth(eq("factory-spin-clear"), eq("bob"), eq("alice"), eq("bob"), any());

        // 重复打勾：早退，既不再改行也不再计分
        int before = ledger.size();
        service.doneSpin("bob", b.getId());
        assertThat(ledger).hasSize(before);
        verify(push, times(1)).pushCoupleEventBoth(eq("factory-spin-clear"), any(), any(), any(), any());
    }

    @Test
    void itemGuardsBeforeAnyInsert() {
        assertThatThrownBy(() -> service.spin("alice", "只做一件事"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("至少写两件事");
        assertThatThrownBy(() -> service.spin("alice", "洗碗,洗碗"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("有重复项");
        assertThatThrownBy(() -> service.spin("alice", "一".repeat(41) + ",拖地"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("每条最多 40 字");
        assertThat(tasks).isEmpty();
        verify(spinMapper, never()).insert(any(CoupleSpinTaskPO.class));
    }

    @Test
    void noSpaceIs404() {
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("carol"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("情侣空间");
    }
}
