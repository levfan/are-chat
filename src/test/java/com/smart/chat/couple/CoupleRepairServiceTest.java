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
 * 修复车间（F320-F329）单测：冷冻时长与在冻唯一、到点才能签、三问归属与双签掉礼盒；
 * 道歉六要素下限与验货归属与打回必填批注、只有 BACK 可重写；重来卡每季一张与打分唯一；
 * 重建计划任务条数/天数档位/双签计数/满档达成/归属中止；冷战倒计时时长档位、暂停权在对方、
 * 到点自动递台阶、和好掉盒；底线 3 格与踩线报备归属；认错一天一次与感动奖归被认错方；
 * 礼盒只能本人做完；纪念碑一天一句；冲突年报聚合；无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleRepairServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleRepairFreezeMapper freezeMapper;
    @Mock
    private CoupleSorryReviewMapper sorryMapper;
    @Mock
    private CoupleRepairRedoMapper redoMapper;
    @Mock
    private CoupleRebuildPlanMapper rebuildMapper;
    @Mock
    private CoupleRepairMakeupMapper makeupMapper;
    @Mock
    private CoupleBottomLineMapper bottomMapper;
    @Mock
    private CoupleAdmitLogMapper admitMapper;
    @Mock
    private CoupleRepairBoxMapper boxMapper;
    @Mock
    private CouplePeaceLineMapper peaceMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleRepairService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<CoupleRepairFreeze> freezes = new ArrayList<>();
    private final List<CoupleSorryReview> sorries = new ArrayList<>();
    private final List<CoupleRepairRedo> redos = new ArrayList<>();
    private final List<CoupleRebuildPlan> plans = new ArrayList<>();
    private final List<CoupleRepairMakeup> makeups = new ArrayList<>();
    private final List<CoupleBottomLine> bottoms = new ArrayList<>();
    private final List<CoupleAdmitLog> admits = new ArrayList<>();
    private final List<CoupleRepairBox> boxes = new ArrayList<>();
    private final List<CouplePeaceLine> peaces = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        lenient().when(freezeMapper.findCurrent("s1")).thenAnswer(inv -> freezes.stream()
                .filter(f -> CoupleRepairFreeze.STATUS_FROZEN.equals(f.getStatus())).findFirst().orElse(null));
        lenient().when(freezeMapper.find(eq("s1"), any())).thenAnswer(inv -> freezes.stream()
                .filter(f -> f.getStartDay().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(freezeMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(freezes));
        lenient().when(freezeMapper.insert(any(CoupleRepairFreeze.class))).thenAnswer(inv -> {
            freezes.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(sorryMapper.findPending("s1")).thenAnswer(inv -> sorries.stream()
                .filter(s -> CoupleSorryReview.STATUS_VERIFY.equals(s.getStatus())).toList());
        lenient().when(sorryMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(sorries));
        lenient().when(sorryMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> sorries.stream()
                .filter(s -> s.getFromUser().equals(inv.getArgument(1))).toList());
        lenient().when(sorryMapper.insert(any(CoupleSorryReview.class))).thenAnswer(inv -> {
            sorries.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(redoMapper.findByQuarter(eq("s1"), any())).thenAnswer(inv -> redos.stream()
                .filter(r -> r.getQuarter().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(redoMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(redos));
        lenient().when(redoMapper.insert(any(CoupleRepairRedo.class))).thenAnswer(inv -> {
            redos.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(rebuildMapper.findName(eq("s1"), any())).thenAnswer(inv -> plans.stream()
                .filter(p -> p.getName().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(rebuildMapper.findOpen("s1")).thenAnswer(inv -> plans.stream()
                .filter(p -> CoupleRebuildPlan.STATUS_OPEN.equals(p.getStatus())).toList());
        lenient().when(rebuildMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(plans));
        lenient().when(rebuildMapper.insert(any(CoupleRebuildPlan.class))).thenAnswer(inv -> {
            plans.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(makeupMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> makeups.stream()
                .filter(m -> m.getDay().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(makeupMapper.findRunning("s1")).thenAnswer(inv -> makeups.stream()
                .filter(m -> CoupleRepairMakeup.STATUS_RUNNING.equals(m.getStatus())).toList());
        lenient().when(makeupMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(makeups));
        lenient().when(makeupMapper.insert(any(CoupleRepairMakeup.class))).thenAnswer(inv -> {
            makeups.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(bottomMapper.findBySlot(eq("s1"), any(), anyInt())).thenAnswer(inv -> bottoms.stream()
                .filter(b -> b.getFromUser().equals(inv.getArgument(1))
                        && b.getSlot().equals(inv.getArgument(2))).findFirst().orElse(null));
        lenient().when(bottomMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> bottoms.stream()
                .filter(b -> b.getFromUser().equals(inv.getArgument(1))).toList());
        lenient().when(bottomMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(bottoms));
        lenient().when(bottomMapper.insert(any(CoupleBottomLine.class))).thenAnswer(inv -> {
            bottoms.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(admitMapper.findByDayUser(eq("s1"), any(), any())).thenAnswer(inv -> admits.stream()
                .filter(a -> a.getDay().equals(inv.getArgument(1)) && a.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(admitMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(admits));
        lenient().when(admitMapper.insert(any(CoupleAdmitLog.class))).thenAnswer(inv -> {
            admits.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(boxMapper.findByDayUser(eq("s1"), any(), any())).thenAnswer(inv -> boxes.stream()
                .filter(b -> b.getDay().equals(inv.getArgument(1)) && b.getOwnerUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(boxMapper.findOpen("s1")).thenAnswer(inv -> boxes.stream()
                .filter(b -> CoupleRepairBox.STATUS_OPEN.equals(b.getStatus())).toList());
        lenient().when(boxMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(boxes));
        lenient().when(boxMapper.insert(any(CoupleRepairBox.class))).thenAnswer(inv -> {
            boxes.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(peaceMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(peaces));
        lenient().when(peaceMapper.findRecent(eq("s1"), any())).thenAnswer(inv -> List.copyOf(peaces));
        lenient().when(peaceMapper.insert(any(CouplePeaceLine.class))).thenAnswer(inv -> {
            peaces.add(inv.getArgument(0));
            return 1;
        });
    }

    // ========== F320 冷冻解冻 ==========

    @Test
    void freezeHoursGuardedAndThawNeedsBothSignAndThreeQuestions() {
        assertThatThrownBy(() -> service.freeze("alice", 48, "太长了"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("别一冻一天");
        service.freeze("alice", 3, "刚才那句话我不想再说第二遍");
        verify(push).pushCoupleEventBoth(eq("repair-freeze"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.freeze("bob", 3, "我也挂一个"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还冻着呢");

        String id = freezes.get(0).getId();
        // 未到点不能签
        assertThatThrownBy(() -> service.freezeSign("bob", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还在冷冻里");

        // 三问只能由挂冷冻的人答
        assertThatThrownBy(() -> service.freezeAsk("bob", id, 1, "我怕你走"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("挂冷冻的人先答");
        service.freezeAsk("alice", id, 1, "我怕你嫌我烦");
        service.freezeAsk("alice", id, 2, "我要你先把话说完");
        assertThatThrownBy(() -> service.freezeAsk("alice", id, 4, "越界了"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("三问只有一、二、三");
        service.freezeAsk("alice", id, 3, "我先给你倒杯水");

        // 三问答完但只有一人签：不解冻
        freezes.get(0).setUntilAt(System.currentTimeMillis() - 1000);
        service.freezeSign("alice", id);
        verify(push).pushCoupleEvent(eq("repair-freeze-sign"), eq("alice"), eq("bob"), any());
        assertThat(freezes.get(0).getStatus()).isEqualTo(CoupleRepairFreeze.STATUS_FROZEN);

        // 本人重复签不重推
        service.freezeSign("alice", id);
        verify(push, times(1)).pushCoupleEvent(eq("repair-freeze-sign"), eq("alice"), eq("bob"), any());

        service.freezeSign("bob", id);
        assertThat(freezes.get(0).getStatus()).isEqualTo(CoupleRepairFreeze.STATUS_THAWED);
        verify(push).pushCoupleEventBoth(eq("repair-thawed"), eq("bob"), eq("alice"), eq("bob"), any());
        // 解冻掉两盒（每人一盒）
        assertThat(boxes).hasSize(2);
        assertThat(boxes.get(0).getTask()).isNotBlank();
        assertThat(service.workshop("alice").freezes().get(0).bothSigned()).isTrue();
        assertThatThrownBy(() -> service.freezeSign("bob", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("复温过了");
    }

    // ========== F321 道歉质检 ==========

    @Test
    void sorryNeedsThreePointsAndVerificationBelongsToPartner() {
        assertThatThrownBy(() -> service.sorryWrite("alice", "对不起", "SORRY,FACT"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("至少自评 3 项");
        assertThatThrownBy(() -> service.sorryWrite("alice", "对不起", "SORRY,HUH"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("要素只有");
        assertThatThrownBy(() -> service.sorryWrite("alice", "字".repeat(301), "SORRY,FACT,FEEL"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 300 字");

        service.sorryWrite("alice", "我昨天把你妈那件事拿出来当武器，是我不对", "FACT,FEEL,SORRY");
        verify(push).pushCoupleEvent(eq("repair-sorry"), eq("alice"), eq("bob"), any());
        String id = sorries.get(0).getId();

        assertThatThrownBy(() -> service.sorryVerify("alice", id, true, ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己给自己验货");
        assertThatThrownBy(() -> service.sorryVerify("bob", id, false, ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("打回要写一句差在哪");

        service.sorryVerify("bob", id, false, "你没说你打算怎么改");
        assertThat(sorries.get(0).getStatus()).isEqualTo(CoupleSorryReview.STATUS_BACK);
        verify(push).pushCoupleEvent(eq("repair-sorry-back"), eq("bob"), eq("alice"), any());

        service.sorryRewrite("alice", id, "补一句：以后不提家长旧账，提前先问你可以吗", "FACT,FEEL,SORRY,FIX,ASK");
        assertThat(sorries.get(0).getStatus()).isEqualTo(CoupleSorryReview.STATUS_VERIFY);
        assertThat(sorries.get(0).getPoints()).contains("FIX");
        assertThatThrownBy(() -> service.sorryRewrite("bob", id, "别人替我重写", "FACT,FEEL,SORRY"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("信主本人");

        service.sorryVerify("bob", id, true, "这版我收下了");
        assertThat(sorries.get(0).getStatus()).isEqualTo(CoupleSorryReview.STATUS_PASSED);
        verify(push).pushCoupleEventBoth(eq("repair-sorry-pass"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.sorryRewrite("alice", id, "还想改", "FACT,FEEL,SORRY"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("被打回的信");
        CoupleRepairService.SorryVO vo = service.workshop("bob").sorries().get(0);
        assertThat(vo.points()).hasSize(5);
    }

    // ========== F322 重来卡 ==========

    @Test
    void redoCardIsOnePerQuarterAndRatedOnce() {
        assertThatThrownBy(() -> service.redoPlay("alice", "没领卡想重放"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没领");
        service.redoApply("alice", "上周六那句「你总是」");
        verify(push).pushCoupleEvent(eq("repair-redo"), eq("alice"), eq("bob"), any());
        assertThat(service.workshop("alice").redo().quarter()).contains("Q");

        assertThatThrownBy(() -> service.redoRate("alice", 5))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没重放");
        service.redoPlay("alice", "这次我先说「我当时害怕」，没打断 TA");
        verify(push).pushCoupleEventBoth(eq("repair-redo-played"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.redoApply("bob", "再来一张"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经用掉了");

        assertThatThrownBy(() -> service.redoRate("alice", 9))
                .isInstanceOf(BusinessException.class).hasMessageContaining("1-5");
        service.redoRate("alice", 4);
        assertThat(redos.get(0).getSatisfaction()).isEqualTo(4);
        assertThatThrownBy(() -> service.redoRate("bob", 2))
                .isInstanceOf(BusinessException.class).hasMessageContaining("打过分了");
        CoupleRepairService.RedoVO vo = service.workshop("bob").redo();
        assertThat(vo.used()).isTrue();
        assertThat(vo.canRate()).isFalse();
    }

    // ========== F323 信任重建 ==========

    @Test
    void rebuildGuardsTasksDaysSignsAndCompletion() {
        assertThatThrownBy(() -> service.rebuildStart("alice", "重建", "爽约那次", 60, "每天说一句真话"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只有 14/30 两档");
        assertThatThrownBy(() -> service.rebuildStart("alice", "重建", "", 30, ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("至少一条");
        StringBuilder many = new StringBuilder();
        for (int i = 0; i < 11; i++) {
            many.append(i > 0 ? "," : "").append("任务").append(i);
        }
        assertThatThrownBy(() -> service.rebuildStart("alice", "重建", "", 30, many.toString()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 10 条");

        service.rebuildStart("alice", "重建信任", "那次爽约", 14, "每天说一句真话,睡前抱 20 秒");
        verify(push).pushCoupleEventBoth(eq("repair-rebuild"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.rebuildStart("bob", "重建信任", "", 14, "换个任务"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经开过了");

        String id = plans.get(0).getId();
        service.rebuildSign("alice", id, DAY);
        verify(push).pushCoupleEvent(eq("repair-rebuild-sign"), eq("alice"), eq("bob"), any());
        service.rebuildSign("alice", id, DAY);
        verify(push, times(1)).pushCoupleEvent(eq("repair-rebuild-sign"), eq("alice"), eq("bob"), any());
        service.rebuildSign("bob", id, DAY);
        verify(push).pushCoupleEventBoth(eq("repair-rebuild-day"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(service.workshop("alice").rebuilds().get(0).signedCount()).isEqualTo(1);
        assertThat(service.workshop("alice").rebuilds().get(0).tasks()).hasSize(2);

        service.rebuildReview("bob", id, "这周我第一次没追问地点");
        assertThat(plans.get(0).getReview()).contains("追问");
        assertThatThrownBy(() -> service.rebuildGiveup("bob", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("谁开的计划");

        // 造满档：补 13 天双签后再签今天 → 达成
        StringBuilder marks = new StringBuilder(plans.get(0).getSignedDays());
        for (int i = 1; i <= 13; i++) {
            String d = LocalDate.now().minusDays(i).toString().replace("-", "").substring(4);
            marks.append(",").append(d).append(":A,").append(d).append(":B");
        }
        plans.get(0).setSignedDays(marks.toString());
        String extra = LocalDate.now().minusDays(14).toString();
        service.rebuildSign("alice", id, extra);
        service.rebuildSign("bob", id, extra);
        assertThat(plans.get(0).getStatus()).isEqualTo(CoupleRebuildPlan.STATUS_DONE);
        verify(push).pushCoupleEventBoth(eq("repair-rebuild-done"), eq("bob"), eq("alice"), eq("bob"), any());
    }

    // ========== F324 和好了倒计时 ==========

    @Test
    void makeupCountdownPauseOfferAndEnd() {
        assertThatThrownBy(() -> service.makeupStart("alice", 5))
                .isInstanceOf(BusinessException.class).hasMessageContaining("10-60 分钟");
        service.makeupStart("alice", 20);
        verify(push).pushCoupleEventBoth(eq("repair-makeup-start"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.makeupStart("bob", 30))
                .isInstanceOf(BusinessException.class).hasMessageContaining("今天已经开过");

        String id = makeups.get(0).getId();
        assertThatThrownBy(() -> service.makeupPause("alice", id, true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己不能按");
        service.makeupPause("bob", id, true);
        verify(push).pushCoupleEventBoth(eq("repair-makeup-pause"), eq("bob"), eq("alice"), eq("bob"), any());

        // 暂停中到点不自动递台阶
        makeups.get(0).setStartAt(System.currentTimeMillis() - 21 * 60_000L);
        service.workshop("alice");
        assertThat(makeups.get(0).getStatus()).isEqualTo(CoupleRepairMakeup.STATUS_RUNNING);

        service.makeupPause("bob", id, false);
        service.workshop("alice");
        assertThat(makeups.get(0).getStatus()).isEqualTo(CoupleRepairMakeup.STATUS_OFFERED);
        assertThat(makeups.get(0).getStepCard()).isNotBlank();
        verify(push).pushCoupleEventBoth(eq("repair-makeup-step"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.makeupOffer("alice", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("台阶已经递过");

        service.makeupEnd("bob", id);
        assertThat(makeups.get(0).getStatus()).isEqualTo(CoupleRepairMakeup.STATUS_ENDED);
        verify(push).pushCoupleEventBoth(eq("repair-makeup-done"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(boxes).hasSize(1);
        assertThat(boxes.get(0).getOwnerUser()).isEqualTo("alice");
        service.makeupEnd("bob", id);
        verify(push, times(1)).pushCoupleEventBoth(eq("repair-makeup-done"), any(), any(), any(), any());
    }

    // ========== F326 底线声明卡 ==========

    @Test
    void bottomLinesAreThreeSlotsAndBreachReportedByStepper() {
        assertThatThrownBy(() -> service.bottomSet("alice", 4, "第四条", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("底线只有 1-3");
        service.bottomSet("alice", 1, "不许拿分手当武器", "");
        verify(push).pushCoupleEvent(eq("repair-bottom"), eq("alice"), eq("bob"), any());
        service.bottomSet("alice", 1, "改写：不许提分手两个字", "");
        verify(push, times(1)).pushCoupleEvent(eq("repair-bottom"), eq("alice"), eq("bob"), any());
        assertThat(bottoms).hasSize(1);
        assertThat(bottoms.get(0).getText()).contains("两个字");

        String id = bottoms.get(0).getId();
        assertThatThrownBy(() -> service.bottomBreach("alice", id, "我自己踩了"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("等 TA 来记");
        assertThatThrownBy(() -> service.bottomBreach("bob", id, " "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("为什么没刹住");
        service.bottomBreach("bob", id, "上头了，下次我先闭嘴三分钟");
        assertThat(bottoms.get(0).getBreachCount()).isEqualTo(1);
        verify(push).pushCoupleEvent(eq("repair-breach"), eq("bob"), eq("alice"), any());

        service.bottomSet("bob", 2, "别在我妈面前否我", "2026-01-01");
        assertThatThrownBy(() -> service.bottomSet("bob", 2, "超字数：" + "字".repeat(60), ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("底线最多 60 字");
        assertThat(service.workshop("alice").bottoms()).hasSize(2);
    }

    // ========== F327 我错了榜 ==========

    @Test
    void admitIsOncePerDayAndTouchBelongsToOtherSide() {
        assertThatThrownBy(() -> service.admit("alice", "  "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("写具体");
        service.admit("alice", "我把你送的东西随手放在阳台上落灰了");
        verify(push).pushCoupleEvent(eq("repair-admit"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.admit("alice", "再认一次"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("刷成打卡");

        String id = admits.get(0).getId();
        assertThatThrownBy(() -> service.admitTouch("alice", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("感动奖");
        service.admitTouch("bob", id);
        verify(push).pushCoupleEventBoth(eq("repair-admit-touch"), eq("bob"), eq("alice"), eq("bob"), any());
        service.admitTouch("bob", id);
        verify(push, times(1)).pushCoupleEventBoth(eq("repair-admit-touch"), any(), any(), any(), any());
        assertThat(service.workshop("bob").admits().get(0).canTouch()).isFalse();

        service.admit("bob", "我明明答应了却拖着不动");
        assertThat(admits).hasSize(2);
        assertThat(admits.get(1).getAboutUser()).isEqualTo("alice");
    }

    // ========== F328/F329 礼盒与纪念碑 ==========

    @Test
    void boxOnlyOwnerCanFinishAndPeaceIsOncePerDay() {
        boxes.add(CoupleRepairBox.of("s1", DAY, "alice", "写三件 TA 为你做过的小事", ""));
        String id = boxes.get(0).getId();
        assertThatThrownBy(() -> service.boxDone("bob", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("TA 的");
        service.boxDone("alice", id);
        assertThat(boxes.get(0).getStatus()).isEqualTo(CoupleRepairBox.STATUS_DONE);
        assertThat(boxes.get(0).getDoneAt()).isNotNull();
        verify(push).pushCoupleEventBoth(eq("repair-box-done"), eq("alice"), eq("alice"), eq("bob"), any());
        service.boxDone("alice", id);
        verify(push, times(1)).pushCoupleEventBoth(eq("repair-box-done"), any(), any(), any(), any());

        service.peaceSet("alice", "你从来不懂我", "现在看，这句最像我当时的慌");
        verify(push).pushCoupleEvent(eq("repair-peace"), eq("alice"), eq("bob"), any());
        service.peaceSet("alice", "你从来不懂我", "改：明年回看再补一句");
        verify(push, times(1)).pushCoupleEvent(eq("repair-peace"), eq("alice"), eq("bob"), any());
        assertThat(peaces).hasSize(1);
        assertThat(peaces.get(0).getNote()).contains("明年");
        assertThatThrownBy(() -> service.peaceSet("bob", "话".repeat(141), ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 140 字");
    }

    // ========== F325 冲突年报 ==========

    @Test
    void yearlyReportAggregatesWithoutTable() {
        freezes.add(CoupleRepairFreeze.of("s1", DAY, "alice", 6, System.currentTimeMillis() - 1000, "吵了"));
        freezes.add(CoupleRepairFreeze.of("s1", LocalDate.now().minusDays(20).toString(), "bob", 12,
                System.currentTimeMillis(), "又吵"));
        freezes.get(0).setStatus(CoupleRepairFreeze.STATUS_THAWED);
        service.sorryWrite("alice", "我不该翻旧账", "FACT,FEEL,SORRY");
        service.sorryVerify("bob", sorries.get(0).getId(), true, "收下");
        service.sorryWrite("bob", "我答应的事没做", "FACT,SORRY,FIX");
        service.admit("alice", "我把你的耳机弄丢了还没说");
        service.admitTouch("bob", admits.get(0).getId());
        service.peaceSet("alice", "随你怎么想", "");

        CoupleRepairService.ReportVO r = service.workshop("alice").report();
        assertThat(r.freezes()).isEqualTo(2);
        assertThat(r.thawed()).isEqualTo(1);
        assertThat(r.avgThawHours()).isEqualTo(9);
        assertThat(r.passed()).isEqualTo(1);
        assertThat(r.sorryIn()).isEqualTo(1);
        assertThat(r.admits()).isEqualTo(1);
        assertThat(r.touched()).isEqualTo(1);
        assertThat(r.peaceLines()).isEqualTo(1);
        assertThat(r.prize()).isNotBlank();
        assertThat(r.summary()).contains("挂过 2 次冷冻");
    }

    // ========== 空间校验 ==========

    @Test
    void requiresActiveSpace() {
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.workshop("alice"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("先邀请一位好友");
    }
}
