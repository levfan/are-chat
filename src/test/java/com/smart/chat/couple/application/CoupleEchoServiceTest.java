package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.persistence.CoupleEchoDeedPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleEchoDeedMapper;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerPO;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpacePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
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
 * 重点锁「积分台账真的插了一行、分值与归属人对」——只看 VO 回显的话，
 * 漏插台账的坏代码在 mock 下也能过。
 */
@ExtendWith(MockitoExtension.class)
class CoupleEchoServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleEchoDeedMapper deedMapper;
    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleEchoService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<CoupleEchoDeedPO> deeds = new ArrayList<>();
    private final List<CouplePointLedgerPO> ledger = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpacePO space = new CoupleSpacePO();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpacePO.STATUS_ACTIVE);
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        lenient().when(deedMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> deeds.stream()
                .filter(d -> d.getFromUser().equals(inv.getArgument(1))).toList());
        lenient().when(deedMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(deeds));
        lenient().when(deedMapper.findByDayContent(eq("s1"), any(), any(), any())).thenAnswer(inv -> deeds.stream()
                .filter(d -> d.getFromUser().equals(inv.getArgument(1)) && d.getDay().equals(inv.getArgument(2))
                        && d.getContent().equals(inv.getArgument(3)))
                .findFirst().orElse(null));
        lenient().when(deedMapper.insert(any(CoupleEchoDeedPO.class))).thenAnswer(inv -> {
            deeds.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(deedMapper.selectById(any())).thenAnswer(inv -> deeds.stream()
                .filter(d -> d.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        lenient().when(deedMapper.updateById(any(CoupleEchoDeedPO.class))).thenAnswer(inv -> 1);

        lenient().when(ledgerMapper.insert(any(CouplePointLedgerPO.class))).thenAnswer(inv -> {
            ledger.add(inv.getArgument(0));
            return 1;
        });
    }

    @Test
    void deedPointsGoToThePersonWhoDidIt() {
        service.addDeed("alice", "下雨天绕路来接我", null);

        assertThat(deeds).hasSize(1);
        verify(push).pushCoupleEventBoth(eq("echo-deed-added"), eq("alice"), eq("alice"), eq("bob"), any());
        // 写字的是「被照顾的那个」，分要给做事的 bob
        assertThat(ledger).hasSize(1);
        assertThat(ledger.get(0).getFromUser()).isEqualTo("bob");
        assertThat(ledger.get(0).getPoints()).isEqualTo(CoupleEchoService.DEED_POINTS);
        assertThat(ledger.get(0).getType()).isEqualTo(CouplePointLedgerPO.TYPE_EARN);
        assertThat(ledger.get(0).getItem()).isEqualTo("好事簿：下雨天绕路来接我");
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
        CoupleEchoDeedPO row = deeds.get(0);
        // TA 的记录只能 TA 自己点
        assertThatThrownBy(() -> service.starDeed("bob", row.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有记下这条的人");

        int before = ledger.size();
        service.starDeed("alice", row.getId());
        assertThat(row.starredFlag()).isTrue();
        verify(deedMapper).updateById(row);
        assertThat(ledger).hasSize(before + 1);
        assertThat(ledger.get(before).getFromUser()).isEqualTo("bob");
        assertThat(ledger.get(before).getPoints()).isEqualTo(CoupleEchoService.DEED_STAR_POINTS);

        // 重复加星：幂等早退，既不再落库也不再记分
        service.starDeed("alice", row.getId());
        assertThat(ledger).hasSize(before + 1);
        verify(deedMapper, times(1)).updateById(row);
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
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.vault("carol"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("情侣空间");
        verify(ledgerMapper, never()).insert(any(CouplePointLedgerPO.class));
    }
}
