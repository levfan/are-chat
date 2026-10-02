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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 两家与朋友（F330-F339）单测：拜访攻略条数上限/一天一篇/双确认归属/战报交回；
 * 送礼池查重与接单买好归属；他观三题懒建与满三出卡；官宣一月一张；文案三候选与选稿权在求稿方；
 * 接待手册按城市 upsert；称呼册查重与只能被考、错题计数；社会信用到期日校验/见证与塌房归属/到期自动解除；
 * 群聊素材改写不重推与双双笑过；赔礼信审阅链与重写窗口；总览聚合；无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleWorldServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleWorldVisitMapper visitMapper;
    @Mock
    private CoupleWorldGiftMapper giftMapper;
    @Mock
    private CoupleWorldFriendViewMapper viewMapper;
    @Mock
    private CoupleWorldDeclareMapper declareMapper;
    @Mock
    private CoupleWorldCaptionMapper captionMapper;
    @Mock
    private CoupleWorldCityPlanMapper cityMapper;
    @Mock
    private CoupleWorldRelativesQMapper relativeMapper;
    @Mock
    private CoupleWorldVowMapper vowMapper;
    @Mock
    private CoupleWorldGroupReportMapper groupMapper;
    @Mock
    private CoupleWorldApologyMapper apologyMapper;
    @Mock
    private CoupleCodexEntryMapper codexMapper;
    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleWorldService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String MONTH = DAY.substring(0, 7);

    private final List<CoupleWorldVisit> visits = new ArrayList<>();
    private final List<CoupleWorldGift> gifts = new ArrayList<>();
    private final List<CoupleWorldFriendView> views = new ArrayList<>();
    private final List<CoupleWorldDeclare> declares = new ArrayList<>();
    private final List<CoupleWorldCaption> captions = new ArrayList<>();
    private final List<CoupleWorldCityPlan> cities = new ArrayList<>();
    private final List<CoupleWorldRelativesQ> relatives = new ArrayList<>();
    private final List<CoupleWorldVow> vows = new ArrayList<>();
    private final List<CoupleWorldGroupReport> groups = new ArrayList<>();
    private final List<CoupleWorldApology> apologies = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        lenient().when(visitMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> visits.stream()
                .filter(v -> v.getDay().equals(inv.getArgument(1))).toList());
        lenient().when(visitMapper.findOpen("s1")).thenAnswer(inv -> visits.stream()
                .filter(v -> CoupleWorldVisit.STATUS_OPEN.equals(v.getStatus())).toList());
        lenient().when(visitMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(visits));
        lenient().when(visitMapper.insert(any(CoupleWorldVisit.class))).thenAnswer(inv -> {
            visits.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(giftMapper.findByIdea(eq("s1"), any())).thenAnswer(inv -> gifts.stream()
                .filter(g -> g.getIdea().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(giftMapper.findOpen("s1")).thenAnswer(inv -> gifts.stream()
                .filter(g -> CoupleWorldGift.STATUS_OPEN.equals(g.getStatus())).toList());
        lenient().when(giftMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(gifts));
        lenient().when(giftMapper.insert(any(CoupleWorldGift.class))).thenAnswer(inv -> {
            gifts.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(viewMapper.findBySlot(eq("s1"), anyInt())).thenAnswer(inv -> views.stream()
                .filter(v -> v.getSlot().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(viewMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(views));
        lenient().when(viewMapper.insert(any(CoupleWorldFriendView.class))).thenAnswer(inv -> {
            views.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(declareMapper.findByMonth(eq("s1"), any())).thenAnswer(inv -> declares.stream()
                .filter(d -> d.getMonth().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(declareMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(declares));
        lenient().when(declareMapper.insert(any(CoupleWorldDeclare.class))).thenAnswer(inv -> {
            declares.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(captionMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> captions.stream()
                .filter(c -> c.getDay().equals(inv.getArgument(1))).toList());
        lenient().when(captionMapper.findByDayUserSlot(eq("s1"), any(), any(), anyInt())).thenAnswer(inv ->
                captions.stream().filter(c -> c.getDay().equals(inv.getArgument(1))
                        && c.getFromUser().equals(inv.getArgument(2)) && c.getSlot() == inv.getArgument(3))
                        .findFirst().orElse(null));
        lenient().when(captionMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(captions));
        lenient().when(captionMapper.insert(any(CoupleWorldCaption.class))).thenAnswer(inv -> {
            captions.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(cityMapper.findByCity(eq("s1"), any())).thenAnswer(inv -> cities.stream()
                .filter(c -> c.getCity().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(cityMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(cities));
        lenient().when(cityMapper.insert(any(CoupleWorldCityPlan.class))).thenAnswer(inv -> {
            cities.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(relativeMapper.findByTerm(eq("s1"), any())).thenAnswer(inv -> relatives.stream()
                .filter(r -> r.getTerm().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(relativeMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(relatives));
        lenient().when(relativeMapper.insert(any(CoupleWorldRelativesQ.class))).thenAnswer(inv -> {
            relatives.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(vowMapper.findDue(eq("s1"), any())).thenAnswer(inv -> vows.stream()
                .filter(v -> CoupleWorldVow.STATUS_OPEN.equals(v.getStatus())
                        && v.getDueDay().compareTo(inv.getArgument(1)) <= 0).toList());
        lenient().when(vowMapper.findByOwner(eq("s1"), any())).thenAnswer(inv -> vows.stream()
                .filter(v -> v.getOwnerUser().equals(inv.getArgument(1))).toList());
        lenient().when(vowMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(vows));
        lenient().when(vowMapper.insert(any(CoupleWorldVow.class))).thenAnswer(inv -> {
            vows.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(groupMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> groups.stream()
                .filter(g -> g.getDay().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(groupMapper.findRecent(eq("s1"), any())).thenAnswer(inv -> List.copyOf(groups));
        lenient().when(groupMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(groups));
        lenient().when(groupMapper.insert(any(CoupleWorldGroupReport.class))).thenAnswer(inv -> {
            groups.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(apologyMapper.findPending("s1")).thenAnswer(inv -> apologies.stream()
                .filter(a -> CoupleWorldApology.STATUS_OPEN.equals(a.getStatus())).toList());
        lenient().when(apologyMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> apologies.stream()
                .filter(a -> a.getFromUser().equals(inv.getArgument(1))).toList());
        lenient().when(apologyMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(apologies));
        lenient().when(apologyMapper.insert(any(CoupleWorldApology.class))).thenAnswer(inv -> {
            apologies.add(inv.getArgument(0));
            return 1;
        });
    }

    // ========== F330 拜访攻略 ==========

    @Test
    void visitPrepsCappedAndConfirmBelongsToPartner() {
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            many.append(i > 0 ? "," : "").append("带东西").append(i);
        }
        assertThatThrownBy(() -> service.visitPlan("alice", DAY, "MINE", many.toString()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 8 条");
        assertThatThrownBy(() -> service.visitPlan("alice", DAY, "HIS", "带水果"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("我家」和「你家");

        service.visitPlan("alice", DAY, "MINE", "带两盒茶,聊聊 TA 小时候,雷区：别接婚期那句");
        verify(push).pushCoupleEvent(eq("world-visit"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.visitPlan("alice", DAY, "YOURS", "再写一篇"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("写过了");

        String id = visits.get(0).getId();
        assertThatThrownBy(() -> service.visitConfirm("alice", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("双确认");
        service.visitConfirm("bob", id);
        verify(push).pushCoupleEventBoth(eq("world-visit-confirmed"), eq("bob"), eq("alice"), eq("bob"), any());
        service.visitConfirm("bob", id);
        verify(push, times(1)).pushCoupleEventBoth(eq("world-visit-confirmed"), any(), any(), any(), any());

        assertThatThrownBy(() -> service.visitReport("bob", id, "你替你交战报"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("写攻略的人");
        service.visitReport("alice", id, "茶送到了，婚期那句被 TA 挡了回去");
        assertThat(visits.get(0).getStatus()).isEqualTo(CoupleWorldVisit.STATUS_DONE);
        verify(push).pushCoupleEventBoth(eq("world-visit-report"), eq("alice"), eq("alice"), eq("bob"), any());

        CoupleWorldService.VisitVO vo = service.world("bob").visits().get(0);
        assertThat(vo.preps()).hasSize(3);
        assertThat(vo.preps().get(2).kind()).isEqualTo("MINE");
        assertThat(vo.canConfirm()).isFalse();
        assertThat(vo.hostLabel()).isEqualTo("我家");
    }

    // ========== F331 送礼互助池 ==========

    @Test
    void giftPoolTakeAndBoughtOwnership() {
        service.giftAdd("alice", "我妈", "颈椎按摩枕", "300 以内", "别买花香的");
        verify(push).pushCoupleEvent(eq("world-gift"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.giftAdd("bob", "你妈", "颈椎按摩枕", "", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("池子里");
        assertThatThrownBy(() -> service.giftAdd("bob", "", "护手霜", "", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("送谁要写");

        String id = gifts.get(0).getId();
        assertThatThrownBy(() -> service.giftTake("alice", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不用自己接单");
        service.giftTake("bob", id);
        assertThat(gifts.get(0).getStatus()).isEqualTo(CoupleWorldGift.STATUS_TAKEN);
        assertThatThrownBy(() -> service.giftBought("alice", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("接单的人");
        service.giftBought("bob", id);
        verify(push).pushCoupleEventBoth(eq("world-gift-bought"), eq("bob"), eq("alice"), eq("bob"), any());
        service.giftBought("bob", id);
        verify(push, times(1)).pushCoupleEventBoth(eq("world-gift-bought"), any(), any(), any(), any());
        assertThat(service.world("alice").gifts().get(0).canTake()).isFalse();
    }

    // ========== F332 朋友视角问卷 ==========

    @Test
    void friendViewSlotsAreLazyStableAndCardNeedsAllThree() {
        assertThat(service.world("alice").views()).hasSize(3);
        assertThat(service.world("bob").views().get(0).question())
                .isEqualTo(service.world("alice").views().get(0).question());
        assertThatThrownBy(() -> service.friendViewFill("alice", 4, "老王", "跑题了"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有 1-3 题");

        service.friendViewFill("alice", 1, "大学室友", "明显是你更让着 TA");
        verify(push).pushCoupleEvent(eq("world-view"), eq("alice"), eq("bob"), any());
        assertThat(service.world("alice").friendViewLine()).isEmpty();
        service.friendViewFill("bob", 2, "闺蜜", "你们像欢喜冤家");
        service.friendViewFill("alice", 3, "发小", "你变爱说话了");
        assertThat(service.world("bob").friendViewLine()).isNotBlank();
        assertThat(service.world("bob").views().get(2).answer()).contains("爱说话");
    }

    // ========== F333 官宣日 ==========

    @Test
    void declareIsOneCardPerMonth() {
        service.declare("alice", "这个月我们把猫接回家了");
        verify(push).pushCoupleEventBoth(eq("world-declare"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThat(service.world("alice").declares().get(0).month()).isEqualTo(MONTH);
        assertThatThrownBy(() -> service.declare("bob", "我也要说一句"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("一个月一张");
        assertThatThrownBy(() -> service.declare("bob", "字".repeat(201)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 200 字");
    }

    // ========== F334 文案代写 ==========

    @Test
    void captionSlotsAndPickAuthority() {
        service.captionSubmit("alice", 1, "把日子过成了我们想要的样子");
        service.captionSubmit("alice", 1, "改写：日子被我们过顺了");
        verify(push, times(1)).pushCoupleEvent(eq("world-caption"), eq("alice"), eq("bob"), any());
        assertThat(captions).hasSize(1);
        assertThatThrownBy(() -> service.captionSubmit("alice", 4, "第四条"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多交三条");

        service.captionSubmit("bob", 1, "TA 负责可爱，我负责配合");
        assertThat(captions).hasSize(2);
        String bobId = captions.stream().filter(c -> c.getFromUser().equals("bob")).findFirst().orElseThrow().getId();
        assertThatThrownBy(() -> service.captionPick("bob", bobId))
                .isInstanceOf(BusinessException.class).hasMessageContaining("互评选稿");
        service.captionPick("alice", bobId);
        verify(push).pushCoupleEventBoth(eq("world-caption-pick"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThat(captions.stream().filter(CoupleWorldCaption::isWon)).hasSize(1);
        // F334 定稿进百科
        List<CoupleCodexEntry> entries = new ArrayList<>();
        lenient().when(codexMapper.findTerm(eq("s1"), any())).thenAnswer(inv -> entries.stream()
                .filter(e -> e.getTerm().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(codexMapper.insert(any(CoupleCodexEntry.class))).thenAnswer(inv -> {
            entries.add(inv.getArgument(0));
            return 1;
        });
        service.captionPick("alice", bobId);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getTerm()).startsWith("定稿文案 · ").contains(DAY);
        assertThat(entries.get(0).getDefinition()).contains("我负责配合");
        assertThat(service.world("bob").captions().stream().filter(CoupleWorldService.CaptionVO::won)
                .findFirst().orElseThrow().mine()).isTrue();
    }

    // ========== F335 进城接待手册 ==========

    @Test
    void cityPlanUpsertsAndCarriesPackTemplate() {
        service.citySave("alice", "成都", LocalDate.now().plusDays(10).toString(),
                "上午熊猫基地,下午喝盖碗茶,晚上九眼桥", "高铁到南站我去接", "充电线,常用药");
        verify(push).pushCoupleEvent(eq("world-city"), eq("alice"), eq("bob"), any());
        service.citySave("alice", "成都", "", "只留两项：熊猫基地,喝盖碗茶", "自驾", "");
        assertThat(cities).hasSize(1);
        CoupleWorldService.CityVO vo = service.world("bob").cities().get(0);
        assertThat(vo.itinerary()).hasSize(2);
        assertThat(vo.daysLeft()).isEqualTo(-1);
        assertThat(service.world("alice").packTemplate()).hasSize(6);
        assertThatThrownBy(() -> service.citySave("bob", "重庆", "2026-13-45", "一条行程", "", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy-MM-dd");
        service.citySave("bob", "重庆", "", "一条行程", "", "");
        assertThat(cities).hasSize(2);
    }

    // ========== F336 亲戚称呼册 ==========

    @Test
    void relativesDedupeAndOnlyTesteeCanAnswer() {
        service.relativeAdd("alice", "舅舅", "你妈妈的弟弟怎么称呼", "舅舅");
        assertThatThrownBy(() -> service.relativeAdd("bob", "舅舅", "重复出题", "舅"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("在册上");
        String id = relatives.get(0).getId();
        assertThatThrownBy(() -> service.relativeTry("alice", id, "舅舅"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("考不了自己");

        service.relativeTry("bob", id, "姑父");
        assertThat(relatives.get(0).getWrongCount()).isEqualTo(1);
        assertThat(relatives.get(0).getLastWrongDay()).isEqualTo(DAY);
        verify(push).pushCoupleEvent(eq("world-relative-wrong"), eq("bob"), eq("alice"), any());
        service.relativeTry("bob", id, " 舅舅 ");
        assertThat(relatives.get(0).getWrongCount()).isEqualTo(1);
        verify(push).pushCoupleEvent(eq("world-relative-right"), eq("bob"), eq("alice"), any());
        assertThat(service.world("alice").relatives().get(0).canTry()).isFalse();
    }

    // ========== F337 社会信用 ==========

    @Test
    void vowWitnessBreakAndAutoReleaseOnDue() {
        assertThatThrownBy(() -> service.vowAdd("alice", "不再熬夜打游戏", DAY))
                .isInstanceOf(BusinessException.class).hasMessageContaining("要在以后");
        String future = LocalDate.now().plusDays(7).toString();
        service.vowAdd("alice", "不再熬夜打游戏", future);
        verify(push).pushCoupleEvent(eq("world-vow"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.vowAdd("alice", "不再熬夜打游戏", future))
                .isInstanceOf(BusinessException.class).hasMessageContaining("立过了");

        String id = vows.get(0).getId();
        assertThatThrownBy(() -> service.vowWitness("alice", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("见证人得是对方");
        service.vowWitness("bob", id);
        verify(push).pushCoupleEvent(eq("world-vow-witness"), eq("bob"), eq("alice"), any());

        assertThatThrownBy(() -> service.vowBreak("alice", id, "我自己塌"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己不能给自己记");
        assertThatThrownBy(() -> service.vowBreak("bob", id, " "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("写一句事实");

        // 到期自动解除（已见证）
        List<CouplePointLedger> earned = new ArrayList<>();
        lenient().when(ledgerMapper.insert(any(CouplePointLedger.class))).thenAnswer(inv -> {
            earned.add(inv.getArgument(0));
            return 1;
        });
        vows.get(0).setDueDay(LocalDate.now().minusDays(1).toString());
        service.world("alice");
        assertThat(vows.get(0).getStatus()).isEqualTo(CoupleWorldVow.STATUS_KEPT);
        // 到期解除自动记一笔心动（立保证的人拿分）
        assertThat(earned).hasSize(1);
        assertThat(earned.get(0).getItem()).startsWith("说到做到").contains("熬夜打游戏");
        assertThat(earned.get(0).getPoints()).isEqualTo(10);
        assertThat(earned.get(0).getFromUser()).isEqualTo("alice");
        verify(push).pushCoupleEventBoth(eq("world-vow-kept"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.vowBreak("bob", id, "迟来的举报"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("收尾了");

        // 未见证的到期不自动解除，改为可被举报塌房
        service.vowAdd("bob", "不删你们的合照", future);
        String second = vows.get(1).getId();
        vows.get(1).setDueDay(LocalDate.now().minusDays(2).toString());
        service.world("bob");
        assertThat(vows.get(1).getStatus()).isEqualTo(CoupleWorldVow.STATUS_OPEN);
        service.vowBreak("alice", second, "昨天你清了相册");
        assertThat(vows.get(1).getStatus()).isEqualTo(CoupleWorldVow.STATUS_BROKEN);
        verify(push).pushCoupleEventBoth(eq("world-vow-broken"), eq("alice"), eq("alice"), eq("bob"), any());
    }

    // ========== F338 群聊记者 ==========

    @Test
    void groupLineRewritableAndBothLaughClosesTheDay() {
        service.groupLine("alice", "今天群里最好笑的是我俩的合照被做成了表情包");
        verify(push).pushCoupleEvent(eq("world-group"), eq("alice"), eq("bob"), any());
        service.groupLine("alice", "改写：表情包那条");
        verify(push, times(1)).pushCoupleEvent(eq("world-group"), eq("alice"), eq("bob"), any());
        assertThat(groups).hasSize(1);

        CoupleWorldService.GroupVO waiting = service.world("bob").group();
        assertThat(waiting.partnerLine()).contains("表情包");
        assertThat(waiting.canWrite()).isTrue();

        service.groupLine("bob", "他们说我们是本周最佳笑料供应商");
        service.groupLaugh("alice");
        verify(push).pushCoupleEvent(eq("world-group-laugh"), eq("alice"), eq("bob"), any());
        assertThat(service.world("bob").group().bothLaughed()).isFalse();
        // 笑过的那一方（alice）自己那格置真、不再给二次笑的机会；没笑的 bob 才还能笑
        assertThat(service.world("alice").group().iLaughed()).isTrue();
        assertThat(service.world("alice").group().canLaugh()).isFalse();
        assertThat(service.world("bob").group().iLaughed()).isFalse();
        assertThat(service.world("bob").group().partnerLaughed()).isTrue();
        assertThat(service.world("bob").group().canLaugh()).isTrue();
        service.groupLaugh("bob");
        verify(push).pushCoupleEventBoth(eq("world-group-both"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(service.world("alice").group().bothLaughed()).isTrue();
        assertThatThrownBy(() -> service.groupLine("bob", " "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("素材要写一句");
    }

    // ========== F339 代 TA 赔礼 ==========

    @Test
    void apologyReviewRewriteAndSendChain() {
        service.apologyWrite("alice", "你妈", "饭桌上我顶了一句", "那天是我太急，您别往心里去。");
        verify(push).pushCoupleEvent(eq("world-apology"), eq("alice"), eq("bob"), any());
        String id = apologies.get(0).getId();

        assertThatThrownBy(() -> service.apologyReview("alice", id, true, ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己审自己");
        assertThatThrownBy(() -> service.apologyReview("bob", id, false, ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("改哪儿");
        service.apologyReview("bob", id, false, "结尾再软一点，别提对错");
        assertThat(apologies.get(0).getStatus()).isEqualTo(CoupleWorldApology.STATUS_BACK);

        assertThatThrownBy(() -> service.apologyRewrite("bob", id, "别人代写"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("信主本人");
        service.apologyRewrite("alice", id, "那天是我太急，回去想了很久，您别往心里去。");
        assertThat(apologies.get(0).getStatus()).isEqualTo(CoupleWorldApology.STATUS_OPEN);
        assertThat(apologies.get(0).getReviewedBy()).isEmpty();

        service.apologyReview("bob", id, true, "这版能送");
        assertThat(apologies.get(0).getStatus()).isEqualTo(CoupleWorldApology.STATUS_SENT);
        verify(push).pushCoupleEventBoth(eq("world-apology-sent"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.apologyRewrite("alice", id, "还想改"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("被打回的信");
        assertThat(service.world("alice").apologies().get(0).template()).isNotBlank();
        assertThat(service.world("bob").apologies().get(0).template()).isEmpty();
    }

    @Test
    void worldListsAreCappedOnRead() {
        // 35 条历史候选文案只回最近 30 条；30 条灵感未触及 40 上限则全给
        for (int i = 0; i < 35; i++) {
            captions.add(CoupleWorldCaption.of("s1", LocalDate.now().minusDays(i).toString(),
                    i % 2 == 0 ? "alice" : "bob", 1, "文案" + i));
        }
        for (int i = 0; i < 30; i++) {
            gifts.add(CoupleWorldGift.of("s1", "长辈" + i, "灵感" + i, "", "", "alice"));
        }
        CoupleWorldService.WorldVO vo = service.world("alice");
        assertThat(vo.captions()).hasSize(30);
        assertThat(vo.gifts()).hasSize(30);
    }

    // ========== 总览与空间 ==========

    @Test
    void overviewAggregatesAllPools() {
        service.visitPlan("alice", DAY, "MINE", "带两盒茶");
        service.giftAdd("bob", "你弟", "机械键盘", "500 内", "");
        service.declare("alice", "本月把领养手续办完了");
        CoupleWorldService.WorldVO vo = service.world("alice");
        assertThat(vo.day()).isEqualTo(DAY);
        assertThat(vo.month()).isEqualTo(MONTH);
        assertThat(vo.visits()).hasSize(1);
        assertThat(vo.gifts()).hasSize(1);
        assertThat(vo.declares()).hasSize(1);
        assertThat(vo.views()).hasSize(3);
    }

    @Test
    void requiresActiveSpace() {
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.world("alice"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("先邀请一位好友");
    }
}
