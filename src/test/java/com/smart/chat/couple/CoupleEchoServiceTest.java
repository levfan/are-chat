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
 * 回音壁单测（保留好事簿/鼓励语罐/能量补给/电量预报）：好事查重不重复记分、加星只归记录人且幂等、
 * 积分归「被记的那位」而不是写字的人、罐子槽位复用与容量、补给每人每天一次、电量钳制与当天改写落同一行、
 * 无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleEchoServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleEchoDeedMapper deedMapper;
    @Mock
    private CoupleEchoJuiceMapper juiceMapper;
    @Mock
    private CoupleEchoRefillLogMapper refillLogMapper;
    @Mock
    private CoupleEchoBatteryMapper batteryMapper;
    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleEchoService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<CoupleEchoDeed> deeds = new ArrayList<>();
    private final List<CoupleEchoJuice> juices = new ArrayList<>();
    private final List<CoupleEchoRefillLog> logs = new ArrayList<>();
    private final List<CoupleEchoBattery> batteries = new ArrayList<>();
    private final List<CouplePointLedger> ledger = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        lenient().when(deedMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> deeds.stream()
                .filter(d -> d.getFromUser().equals(inv.getArgument(1))).toList());
        lenient().when(deedMapper.findByDayContent(eq("s1"), any(), any(), any())).thenAnswer(inv -> deeds.stream()
                .filter(d -> d.getFromUser().equals(inv.getArgument(1)) && d.getDay().equals(inv.getArgument(2))
                        && d.getContent().equals(inv.getArgument(3)))
                .findFirst().orElse(null));
        lenient().when(deedMapper.insert(any(CoupleEchoDeed.class))).thenAnswer(inv -> {
            deeds.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(deedMapper.selectById(any())).thenAnswer(inv -> deeds.stream()
                .filter(d -> d.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        lenient().when(deedMapper.updateById(any(CoupleEchoDeed.class))).thenAnswer(inv -> 1);

        lenient().when(juiceMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(juices));
        lenient().when(juiceMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> juices.stream()
                .filter(j -> j.getFromUser().equals(inv.getArgument(1))).toList());
        lenient().when(juiceMapper.insert(any(CoupleEchoJuice.class))).thenAnswer(inv -> {
            juices.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(juiceMapper.selectById(any())).thenAnswer(inv -> juices.stream()
                .filter(j -> j.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        lenient().when(juiceMapper.deleteById(any(String.class))).thenAnswer(inv -> {
            juices.removeIf(j -> j.getId().equals(inv.getArgument(0)));
            return 1;
        });

        lenient().when(refillLogMapper.findByDayUser(eq("s1"), any(), any())).thenAnswer(inv -> logs.stream()
                .filter(l -> l.getFromUser().equals(inv.getArgument(1)) && l.getDay().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(refillLogMapper.insert(any(CoupleEchoRefillLog.class))).thenAnswer(inv -> {
            logs.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(batteryMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> batteries.stream()
                .filter(b -> b.getDay().equals(inv.getArgument(1))).toList());
        lenient().when(batteryMapper.findByDayUser(eq("s1"), any(), any())).thenAnswer(inv -> batteries.stream()
                .filter(b -> b.getDay().equals(inv.getArgument(1)) && b.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(batteryMapper.insert(any(CoupleEchoBattery.class))).thenAnswer(inv -> {
            batteries.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(batteryMapper.updateById(any(CoupleEchoBattery.class))).thenAnswer(inv -> 1);

        lenient().when(ledgerMapper.insert(any(CouplePointLedger.class))).thenAnswer(inv -> {
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
        assertThat(ledger.get(0).getType()).isEqualTo(CouplePointLedger.TYPE_EARN);
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
        CoupleEchoDeed row = deeds.get(0);
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
    void juiceReusesFreedSlotAndStopsAtCap() {
        for (int i = 0; i < 5; i++) {
            service.addJuice("alice", "纸条" + i);
        }
        assertThat(juices).hasSize(5);
        assertThat(juices).extracting(CoupleEchoJuice::getIdx).containsExactly(1, 2, 3, 4, 5);
        assertThatThrownBy(() -> service.addJuice("alice", "第六张"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("装不下");

        // 删掉第 3 张后，下一张要补进 3 号槽而不是排到 6
        CoupleEchoJuice third = juices.stream().filter(j -> j.getIdx() == 3).findFirst().orElseThrow();
        service.removeJuice("alice", third.getId());
        service.addJuice("alice", "补位那张");
        assertThat(juices).hasSize(5);
        // 补位的那张排在列表末尾，但占回的是 3 号槽
        assertThat(juices).extracting(CoupleEchoJuice::getIdx).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
        assertThat(juices).anyMatch(j -> j.getIdx() == 3 && j.getContent().equals("补位那张"));
    }

    @Test
    void juiceRemovalLimitedToOwner() {
        service.addJuice("alice", "我的纸条");
        CoupleEchoJuice row = juices.get(0);
        assertThatThrownBy(() -> service.removeJuice("bob", row.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只能清自己罐子");
        assertThat(juices).hasSize(1);
        assertThatThrownBy(() -> service.addJuice("alice", "一".repeat(61)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 60 字");
    }

    @Test
    void refillOnceADayCarriesOwnDeedsAndBothJars() {
        service.addDeed("alice", "接我", null);
        service.addDeed("alice", "做饭", null);
        service.addJuice("alice", "你可以的");
        service.addJuice("bob", "你已经很棒了");

        CoupleEchoService.EchoVO vo = service.refill("alice");
        assertThat(vo.refill().mineToday()).isTrue();
        assertThat(vo.refill().deeds()).hasSize(2);
        assertThat(vo.refill().juices()).hasSize(2);
        verify(push).pushCoupleEventBoth(eq("echo-refilled"), eq("alice"), eq("alice"), eq("bob"), any());

        assertThatThrownBy(() -> service.refill("alice"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经充过电");
        // 对方那一格独立
        assertThat(service.refill("bob").refill().partnerToday()).isTrue();
    }

    @Test
    void batteryClampsAndRewritesSameRow() {
        service.battery("alice", 99, "抱抱就好");
        assertThat(batteries).hasSize(1);
        assertThat(batteries.get(0).getLevel()).isEqualTo(5);

        service.battery("alice", -3, "别问");
        assertThat(batteries).hasSize(1);
        assertThat(batteries.get(0).getLevel()).isEqualTo(1);
        assertThat(batteries.get(0).getWant()).isEqualTo("别问");
        verify(batteryMapper, times(1)).insert(any(CoupleEchoBattery.class));
        verify(batteryMapper).updateById(any(CoupleEchoBattery.class));

        // 低电量只提醒对方，提醒自己时不给 hint
        service.battery("bob", 1, "");
        CoupleEchoService.EchoVO asAlice = service.vault("alice");
        assertThat(asAlice.battery()).filteredOn(b -> b.fromUser().equals("bob"))
                .allMatch(b -> !b.mine() && !b.hint().isEmpty());
        assertThat(service.vault("bob").battery()).filteredOn(CoupleEchoService.BatteryVO::mine)
                .allMatch(b -> b.hint().isEmpty());

        assertThatThrownBy(() -> service.battery("alice", 3, "太".repeat(41)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 40 字");
    }

    @Test
    void vaultListsOwnAndPartnerDeedsSeparately() {
        service.addDeed("alice", "TA 接我", null);
        service.addDeed("bob", "TA 做饭", null);
        CoupleEchoService.EchoVO vo = service.vault("alice");
        assertThat(vo.deeds()).extracting(CoupleEchoService.DeedVO::content).containsExactly("TA 接我");
        assertThat(vo.partnerDeeds()).extracting(CoupleEchoService.DeedVO::content).containsExactly("TA 做饭");
        assertThat(vo.day()).isEqualTo(DAY);
    }

    @Test
    void noSpaceIs404() {
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.vault("carol"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("情侣空间");
        verify(ledgerMapper, never()).insert(any(CouplePointLedger.class));
    }
}
