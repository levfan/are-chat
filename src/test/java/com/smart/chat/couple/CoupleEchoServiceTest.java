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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 回音壁（F350-F359）单测：好事簿同日同人同内容重复与加星归属（单记录人模型）、总览近 30 条截断；
 * 鼓励语罐 5 条上限与空槽复用；补给每人每天一次与证据不足降级、顺带开读在途信；
 * 慢递在途 3 封上限与到日读时惰性结算推双方；高光 12 条上限与本人整理；
 * 回执幂等与非本空间 400；电量 1-5 钳制与对方低电量提示；给自己的信单封约束；
 * 日历按天聚合与年报真数字；无空间 404。
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
    private CoupleEchoSlowMapper slowMapper;
    @Mock
    private CoupleEchoHighlightMapper highlightMapper;
    @Mock
    private CoupleEchoReceiptMapper receiptMapper;
    @Mock
    private CoupleEchoBatteryMapper batteryMapper;
    @Mock
    private CoupleEchoSelfLetterMapper selfLetterMapper;
    @Mock
    private CouplePraiseMapper praiseMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleEchoService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String YEAR = String.valueOf(LocalDate.now().getYear());
    private static final long BASE = LocalDate.of(LocalDate.now().getYear(), 1, 10)
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    /** 递增时间戳：让排序断言确定、同时 yearOf 落在当年。 */
    private long seq = 0;

    private final List<CoupleEchoDeed> deeds = new ArrayList<>();
    private final List<CoupleEchoJuice> juices = new ArrayList<>();
    private final List<CoupleEchoRefillLog> refills = new ArrayList<>();
    private final List<CoupleEchoSlow> slows = new ArrayList<>();
    private final List<CoupleEchoHighlight> highlights = new ArrayList<>();
    private final List<CoupleEchoReceipt> receipts = new ArrayList<>();
    private final List<CoupleEchoBattery> batteries = new ArrayList<>();
    private final List<CoupleEchoSelfLetter> selfs = new ArrayList<>();
    private final List<CouplePraise> praises = new ArrayList<>();

    private long tick() {
        return BASE + (++seq) * 1000;
    }

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

        lenient().when(deedMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> deeds.stream()
                .filter(d -> d.getFromUser().equals(inv.getArgument(1)))
                .sorted(Comparator.comparingLong(CoupleEchoDeed::getCreated).reversed()).toList());
        lenient().when(deedMapper.findBySpace("s1")).thenAnswer(inv -> deeds.stream()
                .sorted(Comparator.comparingLong(CoupleEchoDeed::getCreated).reversed()).toList());
        lenient().when(deedMapper.findByDayContent(eq("s1"), any(), any(), any())).thenAnswer(inv -> deeds.stream()
                .filter(d -> d.getFromUser().equals(inv.getArgument(1)) && d.getDay().equals(inv.getArgument(2))
                        && d.getContent().equals(inv.getArgument(3)))
                .findFirst().orElse(null));
        lenient().when(deedMapper.selectById(any())).thenAnswer(inv -> deeds.stream()
                .filter(d -> d.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        lenient().when(deedMapper.insert(any(CoupleEchoDeed.class))).thenAnswer(inv -> {
            CoupleEchoDeed row = inv.getArgument(0);
            row.setCreated(tick());
            deeds.add(row);
            return 1;
        });

        lenient().when(juiceMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> juices.stream()
                .filter(j -> j.getFromUser().equals(inv.getArgument(1)))
                .sorted(Comparator.comparingInt(CoupleEchoJuice::getIdx)).toList());
        lenient().when(juiceMapper.findBySpace("s1")).thenAnswer(inv -> juices.stream()
                .sorted(Comparator.comparing(CoupleEchoJuice::getFromUser)
                        .thenComparingInt(CoupleEchoJuice::getIdx)).toList());
        lenient().when(juiceMapper.selectById(any())).thenAnswer(inv -> juices.stream()
                .filter(j -> j.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        lenient().when(juiceMapper.insert(any(CoupleEchoJuice.class))).thenAnswer(inv -> {
            CoupleEchoJuice row = inv.getArgument(0);
            row.setCreated(tick());
            juices.add(row);
            return 1;
        });
        lenient().when(juiceMapper.deleteById(any(java.io.Serializable.class))).thenAnswer(inv -> {
            Object raw = inv.getArgument(0);
            String id = String.valueOf(raw);
            juices.removeIf(j -> j.getId().equals(id));
            return 1;
        });

        lenient().when(refillLogMapper.findByDayUser(eq("s1"), any(), any())).thenAnswer(inv -> refills.stream()
                .filter(r -> r.getFromUser().equals(inv.getArgument(1)) && r.getDay().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(refillLogMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(refills));
        lenient().when(refillLogMapper.insert(any(CoupleEchoRefillLog.class))).thenAnswer(inv -> {
            CoupleEchoRefillLog row = inv.getArgument(0);
            row.setCreated(tick());
            refills.add(row);
            return 1;
        });

        lenient().when(slowMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> slows.stream()
                .filter(s -> s.getFromUser().equals(inv.getArgument(1)))
                .sorted(Comparator.comparingLong(CoupleEchoSlow::getCreated).reversed()).toList());
        lenient().when(slowMapper.findDue(eq("s1"), any())).thenAnswer(inv -> slows.stream()
                .filter(s -> !s.deliveredFlag() && s.getOpenDay().compareTo((String) inv.getArgument(1)) <= 0)
                .sorted(Comparator.comparing(CoupleEchoSlow::getOpenDay)).toList());
        lenient().when(slowMapper.findBySpace("s1")).thenAnswer(inv -> slows.stream()
                .sorted(Comparator.comparingLong(CoupleEchoSlow::getCreated).reversed()).toList());
        lenient().when(slowMapper.insert(any(CoupleEchoSlow.class))).thenAnswer(inv -> {
            CoupleEchoSlow row = inv.getArgument(0);
            row.setCreated(tick());
            slows.add(row);
            return 1;
        });

        lenient().when(highlightMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> highlights.stream()
                .filter(h -> h.getFromUser().equals(inv.getArgument(1)))
                .sorted(Comparator.comparingLong(CoupleEchoHighlight::getCreated).reversed()).toList());
        lenient().when(highlightMapper.findBySpace("s1")).thenAnswer(inv -> highlights.stream()
                .sorted(Comparator.comparingLong(CoupleEchoHighlight::getCreated).reversed()).toList());
        lenient().when(highlightMapper.selectById(any())).thenAnswer(inv -> highlights.stream()
                .filter(h -> h.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        lenient().when(highlightMapper.insert(any(CoupleEchoHighlight.class))).thenAnswer(inv -> {
            CoupleEchoHighlight row = inv.getArgument(0);
            row.setCreated(tick());
            highlights.add(row);
            return 1;
        });
        lenient().when(highlightMapper.deleteById(any(java.io.Serializable.class))).thenAnswer(inv -> {
            Object raw = inv.getArgument(0);
            String id = String.valueOf(raw);
            highlights.removeIf(h -> h.getId().equals(id));
            return 1;
        });

        lenient().when(receiptMapper.findByQuoteUser(eq("s1"), any(), any())).thenAnswer(inv -> receipts.stream()
                .filter(r -> r.getQuoteId().equals(inv.getArgument(1))
                        && r.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(receiptMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> receipts.stream()
                .filter(r -> r.getFromUser().equals(inv.getArgument(1)))
                .sorted(Comparator.comparingLong(CoupleEchoReceipt::getCreated).reversed()).toList());
        lenient().when(receiptMapper.findBySpace("s1")).thenAnswer(inv -> receipts.stream()
                .sorted(Comparator.comparingLong(CoupleEchoReceipt::getCreated).reversed()).toList());
        lenient().when(receiptMapper.insert(any(CoupleEchoReceipt.class))).thenAnswer(inv -> {
            CoupleEchoReceipt row = inv.getArgument(0);
            row.setCreated(tick());
            receipts.add(row);
            return 1;
        });

        lenient().when(batteryMapper.findByDayUser(eq("s1"), any(), any())).thenAnswer(inv -> batteries.stream()
                .filter(b -> b.getDay().equals(inv.getArgument(1)) && b.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(batteryMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> batteries.stream()
                .filter(b -> b.getDay().equals(inv.getArgument(1)))
                .sorted(Comparator.comparing(CoupleEchoBattery::getFromUser)).toList());
        lenient().when(batteryMapper.insert(any(CoupleEchoBattery.class))).thenAnswer(inv -> {
            CoupleEchoBattery row = inv.getArgument(0);
            row.setCreated(tick());
            batteries.add(row);
            return 1;
        });

        lenient().when(selfLetterMapper.findSealedByUser(eq("s1"), any())).thenAnswer(inv -> selfs.stream()
                .filter(s -> s.getFromUser().equals(inv.getArgument(1)) && s.isSealed())
                .findFirst().orElse(null));
        lenient().when(selfLetterMapper.insert(any(CoupleEchoSelfLetter.class))).thenAnswer(inv -> {
            CoupleEchoSelfLetter row = inv.getArgument(0);
            row.setCreated(tick());
            selfs.add(row);
            return 1;
        });

        lenient().when(praiseMapper.selectById(any())).thenAnswer(inv -> praises.stream()
                .filter(p -> p.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
    }

    // ========== F350 好事簿 ==========

    @Test
    void deedDuplicateRejectedAndStarBelongsToRecorder() {
        assertThatThrownBy(() -> service.addDeed("alice", "", null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("写一句");
        assertThatThrownBy(() -> service.addDeed("alice", "字".repeat(81), null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 80 字");
        assertThatThrownBy(() -> service.addDeed("alice", "把感冒药放在我包里", "26-01-01"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy-MM-dd");

        service.addDeed("alice", "把感冒药放在我包里", null);
        verify(push).pushCoupleEventBoth(eq("echo-deed-added"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.addDeed("alice", " 把感冒药放在我包里 ", DAY))
                .isInstanceOf(BusinessException.class).hasMessageContaining("这条已经记过了");
        // 双记录人对称：bob 记同样的事不冲突
        service.addDeed("bob", "把感冒药放在我包里", null);
        assertThat(deeds).hasSize(2);

        String deedId = deeds.get(0).getId();
        assertThatThrownBy(() -> service.starDeed("bob", deedId))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有记下这条的人能加星");
        assertThatThrownBy(() -> service.starDeed("alice", "nope"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不在好事簿里");
        service.starDeed("alice", deedId);
        verify(push).pushCoupleEventBoth(eq("echo-deed-starred"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThat(deeds.stream().filter(d -> d.getId().equals(deedId)).findFirst().orElseThrow().starredFlag())
                .isTrue();
        service.starDeed("alice", deedId);
        verify(push, times(1)).pushCoupleEventBoth(eq("echo-deed-starred"), any(), any(), any(), any());

        // 双方对称可见：bob 的 partnerDeeds 里能看到 alice 记的「TA 为我做的事」
        CoupleEchoService.EchoVO bobView = service.vault("bob");
        assertThat(bobView.partnerDeeds()).extracting(CoupleEchoService.DeedVO::content)
                .contains("把感冒药放在我包里");
        assertThat(bobView.partnerDeeds().get(0).mine()).isFalse();
        assertThat(bobView.deeds()).extracting(CoupleEchoService.DeedVO::content)
                .containsExactly("把感冒药放在我包里");
    }

    @Test
    void vaultShowsOnlyLatestThirtyDeeds() {
        for (int i = 1; i <= 35; i++) {
            service.addDeed("alice", "好事第" + i + "件", null);
        }
        CoupleEchoService.EchoVO vo = service.vault("alice");
        assertThat(vo.deeds()).hasSize(30);
        assertThat(vo.deeds().get(0).content()).isEqualTo("好事第35件");
        assertThat(vo.deeds().get(29).content()).isEqualTo("好事第6件");
        assertThat(vo.partnerDeeds()).isEmpty();
        assertThat(vo.refill().mineToday()).isFalse();
    }

    // ========== F352 鼓励语罐 ==========

    @Test
    void juiceCapsFiveAndReusesFreedSlot() {
        assertThatThrownBy(() -> service.addJuice("alice", "  "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("写一句");
        assertThatThrownBy(() -> service.addJuice("alice", "字".repeat(61)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 60 字");

        for (int i = 1; i <= 5; i++) {
            service.addJuice("alice", "罐语" + i);
        }
        assertThat(juices).extracting(CoupleEchoJuice::getIdx).containsExactly(1, 2, 3, 4, 5);
        assertThatThrownBy(() -> service.addJuice("alice", "第六张"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("罐子装不下了");

        // bob 独立 5 格，互不影响
        service.addJuice("bob", "bob 的纸条");
        assertThat(juices).hasSize(6);

        // 删中间格后新纸条复用空槽
        String midId = juices.stream().filter(j -> j.getFromUser().equals("alice") && j.getIdx() == 3)
                .findFirst().orElseThrow().getId();
        assertThatThrownBy(() -> service.removeJuice("bob", midId))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只能清自己罐子里的");
        service.removeJuice("alice", midId);
        service.addJuice("alice", "补一张");
        assertThat(juices.stream().filter(j -> j.getFromUser().equals("alice"))
                .map(CoupleEchoJuice::getIdx).sorted()).containsExactly(1, 2, 3, 4, 5);
        assertThat(service.vault("alice").juices()).hasSize(6);
    }

    // ========== F351 能量补给 ==========

    @Test
    void refillOncePerDayAndDegradesWithFewDeeds() {
        service.addDeed("alice", "雨天把伞塞给我", null);
        service.addDeed("alice", "排队替我占位", null);
        service.addJuice("bob", "你已经做得很好了");
        service.addHighlight("alice", "上周三发烧", "整晚给我换毛巾", "又虚又安心");

        CoupleEchoService.EchoVO vo = service.refill("alice");
        verify(push).pushCoupleEventBoth(eq("echo-refilled"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThat(vo.refill().mineToday()).isTrue();
        assertThat(vo.refill().partnerToday()).isFalse();
        assertThat(vo.refill().deeds()).hasSize(2);
        assertThat(vo.refill().deeds()).extracting(CoupleEchoService.DeedVO::fromUser)
                .containsOnly("alice");
        assertThat(vo.refill().juices()).hasSize(1);
        assertThat(vo.refill().highlights()).hasSize(1);
        assertThat(vo.refill().line()).isNotBlank();

        assertThatThrownBy(() -> service.refill("alice"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("今天已经充过电了");
        CoupleEchoService.EchoVO bobVo = service.refill("bob");
        verify(push, times(2)).pushCoupleEventBoth(eq("echo-refilled"), any(), any(), any(), any());
        assertThat(service.vault("alice").refill().partnerToday()).isTrue();

        // 证据不足降级：0 条也照发，只是 deeds 为空
        assertThat(bobVo.refill().deeds()).isEmpty();
        assertThat(bobVo.refill().mineToday()).isTrue();
        assertThatThrownBy(() -> service.refill("bob"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("今天已经充过电了");
    }

    @Test
    void refillOpensSealedSelfLetter() {
        service.writeSelf("alice", "低落不是你的错，先吃饭");
        assertThat(service.vault("alice").selfLetter().status())
                .isEqualTo(CoupleEchoSelfLetter.STATUS_SEALED);

        CoupleEchoService.EchoVO vo = service.refill("alice");
        assertThat(vo.refill().selfLetter()).isEqualTo("低落不是你的错，先吃饭");
        assertThat(selfs.get(0).getStatus()).isEqualTo(CoupleEchoSelfLetter.STATUS_READ);
        assertThat(service.vault("alice").selfLetter()).isNull();
        // 开读后没有推送（写给自己的信不需要观众）
        verify(push, times(1)).pushCoupleEventBoth(any(), any(), any(), any(), any());

        assertThatThrownBy(() -> service.readSelf("alice"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("没有在途的信");
    }

    // ========== F354 感谢慢递 ==========

    @Test
    void slowCapsThreeInFlightAndSettlesLazilyOnRead() {
        assertThatThrownBy(() -> service.writeSlow("alice", "  "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("写一句");
        assertThatThrownBy(() -> service.writeSlow("alice", "字".repeat(101)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 100 字");

        for (int i = 1; i <= 3; i++) {
            service.writeSlow("alice", "谢谢" + i);
        }
        assertThatThrownBy(() -> service.writeSlow("alice", "第四封"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("路上还有 3 封");
        // 每人独立在途配额
        service.writeSlow("bob", "谢谢你的耳机");
        assertThat(slows).hasSize(4).allMatch(s -> !s.deliveredFlag());

        CoupleEchoService.EchoVO vo = service.vault("alice");
        assertThat(vo.slowInFlight()).hasSize(4);
        assertThat(vo.slowArrived()).isEmpty();

        // 到日惰性结算：把一封的送达日改到今天，读时置 1 并推双方（不建 Job）
        CoupleEchoSlow due = slows.stream().filter(s -> s.getFromUser().equals("alice")).findFirst().orElseThrow();
        due.setOpenDay(DAY);
        CoupleEchoService.EchoVO settled = service.vault("bob");
        assertThat(due.deliveredFlag()).isTrue();
        verify(push).pushCoupleEventBoth(eq("echo-thanks-arrived"), eq(due.getFromUser()),
                eq("alice"), eq("bob"), any());
        assertThat(settled.slowArrived()).extracting(CoupleEchoService.SlowVO::content).contains(due.getContent());
        service.vault("alice");
        verify(push, times(1)).pushCoupleEventBoth(eq("echo-thanks-arrived"), any(), any(), any(), any());
    }

    // ========== F355 高光重放 ==========

    @Test
    void highlightCapsTwelveAndOnlyOwnerTidies() {
        assertThatThrownBy(() -> service.addHighlight("alice", "  ", "做了什么", "感觉"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("什么时候");
        assertThatThrownBy(() -> service.addHighlight("alice", "哪天", "  ", "感觉"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("做了什么");
        assertThatThrownBy(() -> service.addHighlight("alice", "哪天", "做了什么", "  "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("什么感觉");
        assertThatThrownBy(() -> service.addHighlight("alice", "字".repeat(41), "做了", "感觉"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 40 字");

        for (int i = 1; i <= 12; i++) {
            service.addHighlight("alice", "时刻" + i, "做了" + i, "感觉" + i);
        }
        assertThatThrownBy(() -> service.addHighlight("alice", "第13刻", "做了13", "感觉13"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("精选夹满了");

        String id = highlights.get(0).getId();
        assertThatThrownBy(() -> service.removeHighlight("bob", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只能整理自己的精选夹");
        service.removeHighlight("alice", id);
        assertThat(highlights).hasSize(11);
        service.addHighlight("alice", "第13刻", "做了13", "感觉13");
        assertThat(service.vault("bob").highlights()).hasSize(12);
        assertThat(service.vault("bob").highlights().get(0).mine()).isFalse();
    }

    // ========== F356 夸夸回执 ==========

    @Test
    void receiptIdempotentAndRejectsForeignQuote() {
        CouplePraise q1 = CouplePraise.of("s1", "bob", "你昨天把碗全洗了");
        q1.setId("q1");
        praises.add(q1);
        CouplePraise foreign = CouplePraise.of("s2", "carol", "别家的夸夸");
        foreign.setId("q2");
        praises.add(foreign);

        assertThatThrownBy(() -> service.receipt("alice", "q2"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不在你们的夸夸墙上");
        assertThatThrownBy(() -> service.receipt("alice", "nope"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不在你们的夸夸墙上");
        assertThatThrownBy(() -> service.receipt("alice", " "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不在你们的夸夸墙上");

        CoupleEchoService.EchoVO vo = service.receipt("alice", "q1");
        verify(push).pushCoupleEvent(eq("echo-receipt-given"), eq("alice"), eq("bob"), any());
        assertThat(vo.receipts()).hasSize(1);
        assertThat(vo.receipts().get(0).quoteContent()).isEqualTo("你昨天把碗全洗了");
        assertThat(vo.receipts().get(0).quoteFrom()).isEqualTo("bob");

        // 幂等：重复点不重复记、不重复推
        service.receipt("alice", "q1");
        assertThat(receipts).hasSize(1);
        verify(push, times(1)).pushCoupleEvent(eq("echo-receipt-given"), any(), any(), any());

        service.receipt("bob", "q1");
        assertThat(receipts).hasSize(2);
        assertThat(service.vault("bob").receipts()).hasSize(1);
    }

    // ========== F357 电量预报 ==========

    @Test
    void batteryClampsRewritesAndHintsLowPartner() {
        assertThatThrownBy(() -> service.battery("alice", 3, "字".repeat(41)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 40 字");

        service.battery("alice", 9, "多抱抱");
        assertThat(batteries.get(0).getLevel()).isEqualTo(5);
        service.battery("alice", null, "");
        assertThat(batteries.get(0).getLevel()).isEqualTo(CoupleEchoBattery.LEVEL_DEFAULT);
        assertThat(batteries).hasSize(1);
        service.battery("alice", 2, "今晚想被哄睡");
        assertThat(batteries).hasSize(1);
        assertThat(batteries.get(0).getLevel()).isEqualTo(2);

        CoupleEchoService.EchoVO bobView = service.vault("bob");
        assertThat(bobView.battery()).hasSize(1);
        assertThat(bobView.battery().get(0).mine()).isFalse();
        assertThat(bobView.battery().get(0).hint()).contains("今晚轻轻的");

        service.battery("bob", 4, "正常就好");
        CoupleEchoService.EchoVO aliceView = service.vault("alice");
        assertThat(aliceView.battery()).hasSize(2);
        assertThat(aliceView.battery().stream().filter(b -> b.mine()).findFirst().orElseThrow().hint())
                .isEmpty();
        assertThat(aliceView.battery().stream().filter(b -> !b.mine()).findFirst().orElseThrow().level())
                .isEqualTo(4);
    }

    // ========== F358 写给低落的自己 ==========

    @Test
    void selfLetterAllowsOnlyOneInFlight() {
        assertThatThrownBy(() -> service.writeSelf("alice", "  "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("写给低落的自己");
        assertThatThrownBy(() -> service.writeSelf("alice", "字".repeat(301)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 300 字");

        service.writeSelf("alice", "难过的时候先把自己喂饱");
        assertThatThrownBy(() -> service.writeSelf("alice", "第二封"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还有一封在等你");

        CoupleEchoService.SelfLetterVO letter = service.vault("alice").selfLetter();
        assertThat(letter.status()).isEqualTo(CoupleEchoSelfLetter.STATUS_SEALED);
        assertThat(letter.content()).isEqualTo("难过的时候先把自己喂饱");

        CoupleEchoService.EchoVO read = service.readSelf("alice");
        assertThat(read.selfLetter().status()).isEqualTo(CoupleEchoSelfLetter.STATUS_READ);
        assertThat(selfs.get(0).getStatus()).isEqualTo(CoupleEchoSelfLetter.STATUS_READ);
        assertThat(service.vault("alice").selfLetter()).isNull();

        // 读完后可以写下一封
        service.writeSelf("alice", "新的一封");
        assertThat(selfs).hasSize(2);
        assertThatThrownBy(() -> service.readSelf("bob"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("没有在途的信");
    }

    // ========== F353 被爱日历 + F359 年报 ==========

    @Test
    void calendarAndYearAggregateRealNumbers() {
        String yesterday = LocalDate.now().minusDays(1).toString();
        service.addDeed("alice", "早安问候", null);
        service.addDeed("alice", "顺路带早饭", null);
        service.addDeed("bob", "帮我修好台灯", null);
        service.starDeed("alice", deeds.get(0).getId());
        service.addDeed("alice", "前天的一杯热牛奶", yesterday);
        service.refill("alice");

        List<CoupleEchoService.CalendarDayVO> days = service.calendar("alice", null);
        assertThat(days).hasSize(2);
        CoupleEchoService.CalendarDayVO today = days.get(1);
        assertThat(today.day()).isEqualTo(DAY);
        assertThat(today.deeds()).isEqualTo(3);
        assertThat(today.starred()).isEqualTo(1);
        assertThat(today.refilled()).isTrue();
        CoupleEchoService.CalendarDayVO yd = days.get(0);
        assertThat(yd.day()).isEqualTo(yesterday);
        assertThat(yd.deeds()).isEqualTo(1);
        assertThat(yd.refilled()).isFalse();

        assertThat(service.calendar("alice", "1999")).isEmpty();
        assertThatThrownBy(() -> service.calendar("alice", "99"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy");

        CoupleEchoService.YearlyVO year = service.yearReport("alice", null);
        assertThat(year.year()).isEqualTo(Integer.parseInt(YEAR));
        assertThat(year.deeds()).isEqualTo(4);
        assertThat(year.starred()).isEqualTo(1);
        assertThat(year.refills()).isEqualTo(1);
        assertThat(year.slowArrived()).isEqualTo(0);
        assertThat(year.receipts()).isEqualTo(0);
        assertThat(year.summary()).contains(YEAR).contains("4 件").contains("1 条救过人");

        assertThatThrownBy(() -> service.yearReport("alice", "abcd"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy");
        assertThat(service.vault("alice").yearly().deeds()).isEqualTo(4);
    }

    // ========== 空间校验 ==========

    @Test
    void requiresActiveSpace() {
        when(spaceMapper.findActiveByUser("charlie")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.vault("charlie"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("先邀请一位好友");
        assertThatThrownBy(() -> service.addDeed("charlie", "好事", null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("先邀请一位好友");
        assertThatThrownBy(() -> service.refill("charlie"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("先邀请一位好友");
    }
}
