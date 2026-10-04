package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.deed.Deed;
import com.smart.chat.couple.domain.deed.DeedRepository;
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
 * 好事簿（保留卡 `couple-echo-deed`）单测。
 * 重点锁「积分台账真的追加了一行、分值与归属人对」——只看 VO 回显的话，
 * 漏记台账的坏代码在 mock 下也能过。假表建在端口这一层，PO 与 Mapper 不出现在用例里。
 */
@ExtendWith(MockitoExtension.class)
class CoupleEchoServiceTest {

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private DeedRepository deedRepository;
    @Mock
    private PointLedgerRepository ledgerRepository;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleEchoService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<Deed> deeds = new ArrayList<>();
    private final List<PointEntry> ledger = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpace space = CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 0L, null,
                null, null, null, null, null, null);
        lenient().when(spaceRepository.findActiveByMember("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceRepository.findActiveByMember("bob")).thenReturn(Optional.of(space));

        lenient().when(deedRepository.listByRecorder(eq("s1"), any())).thenAnswer(inv -> deeds.stream()
                .filter(d -> d.fromUser().equals(inv.getArgument(1))).toList());
        lenient().when(deedRepository.alreadyRecorded(eq("s1"), any(), any(), any())).thenAnswer(inv -> deeds.stream()
                .anyMatch(d -> d.fromUser().equals(inv.getArgument(1)) && d.day().equals(inv.getArgument(2))
                        && d.content().equals(inv.getArgument(3))));
        lenient().when(deedRepository.findById(any())).thenAnswer(inv -> deeds.stream()
                .filter(d -> d.id().equals(inv.getArgument(0))).findFirst());
        lenient().doAnswer(inv -> {
            Deed saved = inv.getArgument(0);
            if (deeds.stream().noneMatch(d -> d.id().equals(saved.id()))) {
                deeds.add(saved);
            }
            return null;
        }).when(deedRepository).save(any(Deed.class));

        lenient().doAnswer(inv -> {
            ledger.add((PointEntry) inv.getArgument(0));
            return null;
        }).when(ledgerRepository).append(any(PointEntry.class));
    }

    @Test
    void deedPointsGoToThePersonWhoDidIt() {
        service.addDeed("alice", "下雨天绕路来接我", null);

        assertThat(deeds).hasSize(1);
        verify(push).pushCoupleEventBoth(eq("echo-deed-added"), eq("alice"), eq("alice"), eq("bob"), any());
        // 写字的是「被照顾的那个」，分要给做事的 bob
        assertThat(ledger).hasSize(1);
        assertThat(ledger.get(0).fromUser()).isEqualTo("bob");
        assertThat(ledger.get(0).points()).isEqualTo(CoupleEchoService.DEED_POINTS);
        assertThat(ledger.get(0).type()).isEqualTo(PointEntry.TYPE_EARN);
        assertThat(ledger.get(0).item()).isEqualTo("好事簿：下雨天绕路来接我");
    }

    @Test
    void duplicateDeedRejectedAndNoExtraPoints() {
        service.addDeed("alice", "接我下班", null);
        int before = ledger.size();
        assertThatThrownBy(() -> service.addDeed("alice", "接我下班", null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经记过");
        assertThat(ledger).hasSize(before);
        assertThat(deeds).hasSize(1);

        assertThatThrownBy(() -> service.addDeed("alice", "接我下班", "2026/01/01"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy-MM-dd");
        assertThatThrownBy(() -> service.addDeed("alice", "一".repeat(81), null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 80 字");
    }

    @Test
    void starOnlyByRecorderIdempotentAndGoesToSubject() {
        service.addDeed("alice", "记得我不吃香菜", null);
        Deed row = deeds.get(0);
        // TA 的记录只能 TA 自己点
        assertThatThrownBy(() -> service.starDeed("bob", row.id()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有记下这条的人");

        int before = ledger.size();
        service.starDeed("alice", row.id());
        assertThat(row.starred()).isTrue();
        // 端口只有一个 save：记下这条时 1 次 + 加星回写 1 次
        verify(deedRepository, times(2)).save(row);
        assertThat(ledger).hasSize(before + 1);
        assertThat(ledger.get(before).fromUser()).isEqualTo("bob");
        assertThat(ledger.get(before).points()).isEqualTo(CoupleEchoService.DEED_STAR_POINTS);

        // 重复加星：幂等早退，既不再回写也不再记分
        service.starDeed("alice", row.id());
        assertThat(ledger).hasSize(before + 1);
        verify(deedRepository, times(2)).save(row);
        assertThatThrownBy(() -> service.starDeed("alice", "missing"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不在好事簿里");
    }

    @Test
    void vaultListsOwnAndPartnerDeedsSeparately() {
        service.addDeed("alice", "TA 接我", null);
        service.addDeed("bob", "TA 做饭", null);
        CoupleEchoService.EchoVO vo = service.vault("alice");
        assertThat(vo.deeds()).extracting(CoupleEchoService.DeedVO::content).containsExactly("TA 接我");
        assertThat(vo.partnerDeeds()).extracting(CoupleEchoService.DeedVO::content).containsExactly("TA 做饭");
        assertThat(vo.mineCount()).isEqualTo(1);
        assertThat(vo.partnerCount()).isEqualTo(1);
        assertThat(vo.day()).isEqualTo(DAY);
    }

    @Test
    void noSpaceIs404() {
        when(spaceRepository.findActiveByMember("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.vault("carol"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("情侣空间");
        verify(ledgerRepository, never()).append(any(PointEntry.class));
    }
}
