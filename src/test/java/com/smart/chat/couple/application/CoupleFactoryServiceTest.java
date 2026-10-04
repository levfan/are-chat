package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.chore.SpinTask;
import com.smart.chat.couple.domain.chore.SpinTaskRepository;
import com.smart.chat.couple.domain.points.PointEntry;
import com.smart.chat.couple.domain.points.PointLedgerRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import com.smart.chat.sharedkernel.web.BusinessException;
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
 * <p>
 * 假表建在端口这一层（{@link SpinTaskRepository}／{@link PointLedgerRepository}），
 * PO 与 Mapper 不出现在用例里；期望值与改造前逐字相同。
 */
@ExtendWith(MockitoExtension.class)
class CoupleFactoryServiceTest {

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private SpinTaskRepository spinRepository;
    @Mock
    private PointLedgerRepository ledgerRepository;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleFactoryService service;

    private final List<SpinTask> tasks = new ArrayList<>();
    private final List<PointEntry> ledger = new ArrayList<>();
    private final String week = LocalDate.now().with(DayOfWeek.MONDAY).toString();

    @BeforeEach
    void setUp() {
        CoupleSpace space = CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 0L, null, null, null, null, null, null, null);
        lenient().when(spaceRepository.findActiveByMember("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceRepository.findActiveByMember("bob")).thenReturn(Optional.of(space));

        lenient().when(spinRepository.findByWeek(eq("s1"), any())).thenAnswer(inv -> tasks.stream()
                .filter(t -> t.week().equals(inv.getArgument(1))).toList());
        lenient().when(spinRepository.alreadySpun(eq("s1"), any())).thenAnswer(inv -> tasks.stream()
                .anyMatch(t -> t.week().equals(inv.getArgument(1))));
        lenient().when(spinRepository.findByIdIn(any(), any())).thenAnswer(inv -> tasks.stream()
                .filter(t -> t.id().equals(inv.getArgument(0)) && t.spaceId().equals(inv.getArgument(1)))
                .findFirst());
        lenient().doAnswer(inv -> {
            SpinTask saved = inv.getArgument(0);
            if (tasks.stream().noneMatch(t -> t.id().equals(saved.id()))) {
                tasks.add(saved);
            }
            return null;
        }).when(spinRepository).save(any(SpinTask.class));
        lenient().doAnswer(inv -> {
            ledger.add((PointEntry) inv.getArgument(0));
            return null;
        }).when(ledgerRepository).append(any(PointEntry.class));
    }

    private void spinTwo() {
        service.spin("alice", "洗碗,拖地");
    }

    @Test
    void spinAssignsAlternatelyAndBlocksSecondSpin() {
        spinTwo();
        assertThat(tasks).hasSize(2);
        assertThat(tasks.get(0).assignedUser()).isNotEqualTo(tasks.get(1).assignedUser());
        verify(push).pushCoupleEventBoth(eq("factory-spin-open"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(this::spinTwo)
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经转过盘");
    }

    @Test
    void ownConfirmationInvalidAndUnconfirmedDoneBlocked() {
        spinTwo();
        SpinTask mine = tasks.stream()
                .filter(t -> t.assignedUser().equals("alice")).findFirst().orElseThrow();
        // 自己的活自己认不算双签
        assertThatThrownBy(() -> service.confirmSpin("alice", mine.id()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己的活自己认");
        // 没认账之前，干了也白干
        assertThatThrownBy(() -> service.doneSpin("alice", mine.id()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("先等对方认账");
        assertThat(ledger).isEmpty();

        service.confirmSpin("bob", mine.id());
        assertThat(mine.confirmed()).isTrue();
        // 抢 TA 的活
        assertThatThrownBy(() -> service.doneSpin("bob", mine.id()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("这活不是你的");
    }

    @Test
    void doneEarnsPointsOnceAndClearRewardsBoth() {
        spinTwo();
        SpinTask a = tasks.stream().filter(t -> t.assignedUser().equals("alice")).findFirst().orElseThrow();
        SpinTask b = tasks.stream().filter(t -> t.assignedUser().equals("bob")).findFirst().orElseThrow();
        service.confirmSpin("bob", a.id());
        service.confirmSpin("alice", b.id());

        service.doneSpin("alice", a.id());
        // 干完那一格：只给干活的人记一笔 +3
        assertThat(ledger).hasSize(1);
        assertThat(ledger.get(0).fromUser()).isEqualTo("alice");
        assertThat(ledger.get(0).type()).isEqualTo(PointEntry.TYPE_EARN);
        assertThat(ledger.get(0).points()).isEqualTo(CoupleFactoryService.SPIN_DONE_POINTS);
        assertThat(ledger.get(0).item()).startsWith(CoupleFactoryService.SPIN_DONE_PREFIX);
        // 本周没全清，不该发清空奖
        assertThat(ledger).noneMatch(l -> CoupleFactoryService.SPIN_CLEAR_REASON.equals(l.item()));

        service.doneSpin("bob", b.id());
        // 第二格干完：bob 自己的 +3，加上双方各一笔清空 +2
        assertThat(ledger).hasSize(4);
        assertThat(ledger).filteredOn(l -> CoupleFactoryService.SPIN_CLEAR_REASON.equals(l.item()))
                .extracting(PointEntry::fromUser).containsExactlyInAnyOrder("alice", "bob");
        verify(push).pushCoupleEventBoth(eq("factory-spin-clear"), eq("bob"), eq("alice"), eq("bob"), any());

        // 重复打勾：早退，既不再改行也不再计分
        int before = ledger.size();
        service.doneSpin("bob", b.id());
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
        verify(spinRepository, never()).save(any(SpinTask.class));
    }

    @Test
    void noSpaceIs404() {
        when(spaceRepository.findActiveByMember("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("carol"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("情侣空间");
    }
}
