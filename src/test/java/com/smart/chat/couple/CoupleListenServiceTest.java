package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
 * 倾听与发声（F260-F269）单测：时段在途唯一与自确认禁止、双评齐推 both、
 * 替我说自稿禁定/定稿 both、误会倒带齐推与改写不推、卡壳一问每周一题与双答齐、
 * 换位信开放日守卫与拆自己信 400、早想说惰性放行每日一句、三行连续 21 里程碑、
 * 语气枚举与改写不重推、休战旗双决定收旗、称呼日双方用完 both、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleListenServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleListenSlotMapper slotMapper;
    @Mock
    private CoupleProxyWordMapper proxyMapper;
    @Mock
    private CoupleMisrewindMapper misMapper;
    @Mock
    private CoupleStuckQMapper stuckMapper;
    @Mock
    private CoupleSwapLetterMapper letterMapper;
    @Mock
    private CoupleHoldWordMapper holdMapper;
    @Mock
    private CoupleThreeLineMapper threeMapper;
    @Mock
    private CoupleToneNoteMapper toneMapper;
    @Mock
    private CoupleTruceMapper truceMapper;
    @Mock
    private CoupleNameDayMapper nameMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleListenService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String WEEK = LocalDate.now().with(java.time.DayOfWeek.MONDAY).toString();

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        return space;
    }

    private void stubSpace() {
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space()));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space()));
    }

    // ========== F260 时段 ==========

    @Test
    void slotFlowUniqueConfirmRateBoth() {
        stubSpace();
        List<CoupleListenSlot> rows = new ArrayList<>();
        when(slotMapper.findCurrent("s1")).thenAnswer(inv -> rows.stream()
                .filter(r -> CoupleListenSlot.STATUS_OPEN.equals(r.getStatus())
                        || CoupleListenSlot.STATUS_CONFIRMED.equals(r.getStatus())).toList());
        when(slotMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        when(slotMapper.insert(any(CoupleListenSlot.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });

        service.requestSlot("alice", "最近的工作焦虑");
        assertThatThrownBy(() -> service.requestSlot("bob", "再来一个"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("在途");
        CoupleListenSlot slot = rows.get(0);
        assertThatThrownBy(() -> service.confirmSlot("alice", slot.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("耳朵是 TA 的");
        service.confirmSlot("bob", slot.getId());
        service.doneSlot("alice", slot.getId());
        verify(push).pushCoupleEventBoth(eq("slot-done"), eq("alice"), eq("alice"), eq("bob"), any());
        service.rateSlot("alice", slot.getId(), 5, null);
        service.rateSlot("bob", slot.getId(), 4, "下次还约");
        verify(push).pushCoupleEventBoth(eq("slot-rated"), eq("bob"), eq("alice"), eq("bob"), any());
        service.rateSlot("bob", slot.getId(), 1, null);
        verify(push, times(1)).pushCoupleEventBoth(eq("slot-rated"), any(), any(), any(), any());
    }

    // ========== F261 替我说 ==========

    @Test
    void proxyCannotAdoptOwnAndAdoptPushesBoth() {
        stubSpace();
        List<CoupleProxyWord> rows = new ArrayList<>();
        when(proxyMapper.findDraft("s1", "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> "alice".equals(r.getFromUser()) && CoupleProxyWord.STATUS_DRAFT.equals(r.getStatus()))
                .findFirst().orElse(null));
        when(proxyMapper.findByStatus("s1", CoupleProxyWord.STATUS_ADOPTED)).thenReturn(List.of());
        when(proxyMapper.insert(any(CoupleProxyWord.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(proxyMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));

        service.proxy("alice", "其实我很需要你");
        service.proxy("alice", "其实我很需要你，别嫌我烦");
        assertThat(rows).hasSize(1);
        CoupleProxyWord draft = rows.get(0);
        assertThatThrownBy(() -> service.adoptProxy("alice", draft.getId(), null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不能自己定稿");
        service.adoptProxy("bob", draft.getId(), "照念，后面加一句我也想你");
        verify(push).pushCoupleEventBoth(eq("proxy-adopted"), eq("bob"), eq("alice"), eq("bob"), any());
        service.adoptProxy("bob", draft.getId(), null);
        verify(push, times(1)).pushCoupleEventBoth(eq("proxy-adopted"), any(), any(), any(), any());
    }

    // ========== F262 误会倒带 ==========

    @Test
    void misrewindCompletesAndRewriteSilent() {
        stubSpace();
        List<CoupleMisrewind> rows = new ArrayList<>();
        when(misMapper.find(eq("s1"), eq(DAY), any(), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getTopic().equals(inv.getArgument(2)) && r.getFromUser().equals(inv.getArgument(3)))
                .findFirst().orElse(null));
        when(misMapper.findRecent(eq("s1"), any())).thenReturn(List.of());
        when(misMapper.insert(any(CoupleMisrewind.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.misrewind("alice", "洗碗那件事", "我以为你在怪我", "我猜你其实只是想早点睡");
        verify(push).pushCoupleEvent(eq("misrewind"), eq("alice"), eq("bob"), any());
        service.misrewind("bob", "洗碗那件事", "我没怪你", "我就是困了");
        verify(push).pushCoupleEventBoth(eq("misrewind-done"), eq("bob"), eq("alice"), eq("bob"), any());
        service.misrewind("bob", "洗碗那件事", "改一下", "还是困了");
        verify(push, times(1)).pushCoupleEventBoth(eq("misrewind-done"), any(), any(), any(), any());
        assertThatThrownBy(() -> service.misrewind("alice", "空主题", "", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("至少写一边");
    }

    // ========== F263 卡壳一问 ==========

    @Test
    void stuckOnePerWeekAndBothAnsweredPushes() {
        stubSpace();
        List<CoupleStuckQ> rows = new ArrayList<>();
        when(stuckMapper.find("s1", WEEK, "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> "alice".equals(r.getFromUser())).findFirst().orElse(null));
        when(stuckMapper.find("s1", WEEK, "bob")).thenAnswer(inv -> rows.stream()
                .filter(r -> "bob".equals(r.getFromUser())).findFirst().orElse(null));
        when(stuckMapper.findByWeek("s1", WEEK)).thenAnswer(inv -> List.copyOf(rows));
        when(stuckMapper.insert(any(CoupleStuckQ.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(stuckMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));

        service.stuck("alice", "我上次说你哪句话最暖心？");
        assertThatThrownBy(() -> service.stuck("alice", "第二题"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("问过一题");
        assertThatThrownBy(() -> service.answerStuck("alice", rows.get(0).getId(), "自答"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己的题");
        service.stuck("bob", "你最喜欢我做的哪道菜？");
        service.answerStuck("bob", rows.get(0).getId(), "那句「有你在我不慌」");
        verify(push).pushCoupleEvent(eq("stuck-answered"), eq("bob"), eq("alice"), any());
        service.answerStuck("alice", rows.get(1).getId(), "番茄炒蛋，不放糖那种");
        verify(push).pushCoupleEventBoth(eq("stuck-both"), eq("alice"), eq("alice"), eq("bob"), any());
    }

    // ========== F264 换位信 ==========

    @Test
    void letterGuardsAndOpenFlow() {
        stubSpace();
        List<CoupleSwapLetter> rows = new ArrayList<>();
        when(letterMapper.find("s1", DAY, "alice")).thenAnswer(inv -> rows.stream()
                .filter(r -> "alice".equals(r.getFromUser())).findFirst().orElse(null));
        when(letterMapper.find("s1", DAY, "bob")).thenAnswer(inv -> rows.stream()
                .filter(r -> "bob".equals(r.getFromUser())).findFirst().orElse(null));
        when(letterMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));
        when(letterMapper.insert(any(CoupleSwapLetter.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        when(letterMapper.selectById(any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0))).findFirst().orElse(null));

        assertThatThrownBy(() -> service.letter("alice", "以你的口吻写", DAY))
                .isInstanceOf(BusinessException.class).hasMessageContaining("之后");
        service.letter("alice", "今天的你是个嘴硬的傻瓜", LocalDate.now().plusDays(3).toString());
        service.letter("bob", "今天的我在偷偷喜欢你在意的样子", LocalDate.now().plusDays(3).toString());
        verify(push).pushCoupleEventBoth(eq("swap-letter-pair"), eq("bob"), eq("alice"), eq("bob"), any());
        CoupleSwapLetter fromAlice = rows.stream().filter(r -> r.getFromUser().equals("alice")).findFirst().orElseThrow();
        assertThatThrownBy(() -> service.openLetter("alice", fromAlice.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("你写的是底稿");
        assertThatThrownBy(() -> service.openLetter("bob", fromAlice.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没到开放日");
    }

    // ========== F265 早想说放行 ==========

    @Test
    void holdReleasesOnePerReadDayOnly() {
        stubSpace();
        CoupleHoldWord due = CoupleHoldWord.of("s1", "上周就想夸你今天很努力", "bob", DAY);
        List<CoupleHoldWord> rows = new ArrayList<>(List.of(due));
        when(holdMapper.findDue("s1", DAY)).thenAnswer(inv -> rows.stream()
                .filter(r -> CoupleHoldWord.STATUS_HELD.equals(r.getStatus())
                        && r.getOpenDay().compareTo(DAY) <= 0).toList());
        when(holdMapper.findNextHeld(eq("s1"), eq(DAY))).thenReturn(null);
        when(holdMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rows));
        when(holdMapper.insert(any(CoupleHoldWord.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });

        service.today("alice");
        assertThat(due.getStatus()).isEqualTo(CoupleHoldWord.STATUS_SENT);
        verify(push).pushCoupleEvent(eq("hold-sent"), eq("bob"), eq("alice"), any());
        service.today("alice");
        verify(push, times(1)).pushCoupleEvent(eq("hold-sent"), any(), any(), any());

        service.hold("alice", "先记一笔，七天后到");
        CoupleHoldWord fresh = rows.get(1);
        assertThat(fresh.getOpenDay()).isGreaterThan(DAY);
    }

    // ========== F266 三行 21 连 ==========

    @Test
    void threeLineBlankGuardAndBadgeCrossing() {
        stubSpace();
        List<CoupleThreeLine> rows = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            rows.add(CoupleThreeLine.of("s1", LocalDate.now().minusDays(i).toString(), "alice", "早", "谢", "夸"));
        }
        when(threeMapper.find(eq("s1"), any(), eq("alice"))).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getDay().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(threeMapper.find(eq("s1"), any(), eq("bob"))).thenReturn(null);
        when(threeMapper.findByUser("s1", "alice")).thenAnswer(inv -> {
            List<CoupleThreeLine> sorted = new ArrayList<>(rows);
            sorted.sort(Comparator.comparing(CoupleThreeLine::getDay));
            return sorted;
        });
        lenient().when(threeMapper.findByUser("s1", "bob")).thenReturn(List.of());
        when(threeMapper.insert(any(CoupleThreeLine.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        assertThatThrownBy(() -> service.three("alice", "", "", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("至少写一行");
        service.three("alice", "晴", "谢谢带咖啡", "夸你准时无敌");
        verify(push).pushCoupleEventBoth(eq("three-line-21"), eq("alice"), eq("alice"), eq("bob"), any());
    }

    // ========== F267 语气 ==========

    @Test
    void toneEnumGuardAndRewriteSilent() {
        stubSpace();
        List<CoupleToneNote> rows = new ArrayList<>();
        when(toneMapper.find(eq("s1"), eq(DAY), eq("alice"))).thenAnswer(inv -> rows.stream()
                .findFirst().orElse(null));
        lenient().when(toneMapper.find(eq("s1"), eq(DAY), eq("bob"))).thenReturn(null);
        lenient().when(toneMapper.findByDay("s1", DAY)).thenAnswer(inv -> List.copyOf(rows));
        when(toneMapper.insert(any(CoupleToneNote.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        assertThatThrownBy(() -> service.tone("alice", "ANGRY", null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("四种");
        service.tone("alice", "tired", "昨晚赶工");
        verify(push).pushCoupleEvent(eq("tone-marked"), eq("alice"), eq("bob"), any());
        service.tone("alice", "BUSY", "改成忙");
        verify(push, times(1)).pushCoupleEvent(eq("tone-marked"), any(), any(), any());
        assertThat(rows.get(0).getTone()).isEqualTo("BUSY");
    }

    // ========== F268 休战旗 ==========

    @Test
    void truceDualDecideEndsFlag() {
        stubSpace();
        List<CoupleTruce> rows = new ArrayList<>();
        when(truceMapper.findCurrent("s1")).thenAnswer(inv -> rows.stream()
                .filter(r -> CoupleTruce.STATUS_ON.equals(r.getStatus())).toList());
        when(truceMapper.insert(any(CoupleTruce.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.truce("alice", 10);
        assertThatThrownBy(() -> service.truce("bob", null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经举着");
        assertThatThrownBy(() -> service.decideTruce("alice", false))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没到解冻时刻");

        rows.get(0).setUntilAt(System.currentTimeMillis() - 1000);
        service.decideTruce("alice", false);
        verify(push).pushCoupleEvent(eq("truce-decide"), eq("alice"), eq("bob"), any());
        service.decideTruce("bob", false);
        verify(push).pushCoupleEventBoth(eq("truce-off"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(rows.get(0).getStatus()).isEqualTo(CoupleTruce.STATUS_ENDED);
    }

    // ========== F269 称呼日 ==========

    @Test
    void nameDayLazyCreateAndBothHit() {
        stubSpace();
        List<CoupleNameDay> rows = new ArrayList<>();
        when(nameMapper.find("s1", DAY)).thenAnswer(inv -> rows.stream().findFirst().orElse(null));
        when(nameMapper.insert(any(CoupleNameDay.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        service.useName("alice");
        verify(push).pushCoupleEvent(eq("nameday-use"), eq("alice"), eq("bob"), any());
        service.useName("alice");
        verify(push, times(1)).pushCoupleEvent(eq("nameday-use"), any(), any(), any());
        service.useName("bob");
        verify(push).pushCoupleEventBoth(eq("nameday-hit"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(rows.get(0).getNameText()).isNotBlank();
    }

    // ========== 兜底 ==========

    @Test
    void noSpaceLeadsTo404() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.today("solo"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没有建立情侣空间");
    }
}
