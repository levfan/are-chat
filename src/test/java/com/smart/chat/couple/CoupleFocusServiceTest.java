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
 * 注意力保护区单测（系统裁剪后只留饭桌不低头、对视十秒、走神温柔哨）：
 * 双点卡只有本次真的 0→1 才推进度、凑齐两边才推一次 both、重复点击静默，
 * 且必须各自报告「我这一格按过没」（缺这个位前端会把「我已点」显示成「就差你一个」），
 * 温柔哨每人每天 2 张与长度守卫、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleFocusServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleFocusMealMapper mealMapper;
    @Mock
    private CoupleFocusGazeMapper gazeMapper;
    @Mock
    private CoupleFocusNudgeMapper nudgeMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleFocusService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<CoupleFocusMeal> meals = new ArrayList<>();
    private final List<CoupleFocusGaze> gazes = new ArrayList<>();
    private final List<CoupleFocusNudge> nudges = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        lenient().when(mealMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> meals.stream()
                .filter(m -> m.getDay().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(mealMapper.insert(any(CoupleFocusMeal.class))).thenAnswer(inv -> {
            meals.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(mealMapper.updateById(any(CoupleFocusMeal.class))).thenAnswer(inv -> 1);

        lenient().when(gazeMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> gazes.stream()
                .filter(g -> g.getDay().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(gazeMapper.insert(any(CoupleFocusGaze.class))).thenAnswer(inv -> {
            gazes.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(gazeMapper.updateById(any(CoupleFocusGaze.class))).thenAnswer(inv -> 1);

        lenient().when(nudgeMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> nudges.stream()
                .filter(n -> n.getDay().equals(inv.getArgument(1))).toList());
        lenient().when(nudgeMapper.findByDayUser(eq("s1"), any(), any())).thenAnswer(inv -> nudges.stream()
                .filter(n -> n.getDay().equals(inv.getArgument(1)) && n.getFromUser().equals(inv.getArgument(2)))
                .toList());
        lenient().when(nudgeMapper.insert(any(CoupleFocusNudge.class))).thenAnswer(inv -> {
            nudges.add(inv.getArgument(0));
            return 1;
        });
    }

    @Test
    void mealPushesBothOnlyWhenSecondSideTaps() {
        service.mealTick("alice");
        assertThat(meals).hasSize(1);
        // 只有一边按过，不该提前庆祝
        verify(push, never()).pushCoupleEventBoth(any(), any(), any(), any(), any());

        service.mealTick("bob");
        verify(push, times(1)).pushCoupleEventBoth(eq("focus-meal-both"), eq("bob"), eq("alice"), eq("bob"), any());

        // 重复点击：tick 不再返回真，既不新增 update 也不重推（两边各按一次 = 2 次落库，到此为止）
        verify(mealMapper, times(2)).updateById(any(CoupleFocusMeal.class));
        service.mealTick("bob");
        verify(mealMapper, times(2)).updateById(any(CoupleFocusMeal.class));
        verify(push, times(1)).pushCoupleEventBoth(eq("focus-meal-both"), any(), any(), any(), any());
    }

    @Test
    void doubleTickCardsReportWhoAlreadyTapped() {
        service.mealTick("alice");
        service.gazeTick("bob");

        CoupleFocusService.TodayVO asAlice = service.today("alice");
        assertThat(asAlice.mealMine()).isTrue();
        assertThat(asAlice.gazeMine()).isFalse();
        assertThat(asAlice.gazes()).isEqualTo(1);
        assertThat(asAlice.meals()).isEqualTo(1);

        CoupleFocusService.TodayVO asBob = service.today("bob");
        assertThat(asBob.mealMine()).isFalse();
        assertThat(asBob.gazeMine()).isTrue();
        // 双点还没凑齐时，界面不能谎报成「已点亮」
        assertThat(asAlice.mealBoth()).isFalse();
        assertThat(asBob.gazeBoth()).isFalse();
    }

    @Test
    void gazeLightsUpOnBothSides() {
        service.gazeTick("alice");
        assertThat(service.today("bob").gazeBoth()).isFalse();
        service.gazeTick("bob");
        assertThat(service.today("bob").gazeBoth()).isTrue();
        assertThat(service.today("bob").gazes()).isEqualTo(2);
    }

    @Test
    void nudgeQuotaTwoPerDayAndLengthGuard() {
        service.nudge("alice", "回来啦");
        service.nudge("alice", "发呆呢");
        assertThat(nudges).hasSize(2);
        // 只推收卡人，不推自己
        verify(push, times(2)).pushCoupleEvent(eq("focus-nudge-sent"), eq("alice"), eq("bob"), any());

        CoupleFocusService.TodayVO vo = service.today("alice");
        assertThat(vo.nudgesToday()).isEqualTo(2);
        assertThat(vo.nudgeQuotaLeft()).isZero();

        assertThatThrownBy(() -> service.nudge("alice", "第三次"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("用完");
        assertThatThrownBy(() -> service.nudge("bob", "太".repeat(41)))
                .isInstanceOf(BusinessException.class);
        assertThat(nudges).hasSize(2);
    }

    @Test
    void noSpaceIs404() {
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.today("carol"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("情侣空间");
        verify(nudgeMapper, never()).insert(any(CoupleFocusNudge.class));
    }
}
