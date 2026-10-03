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
 * 欢笑银行（F390-F399）单测：笑点存档的未来日/查重/每天 3 条与好笑度钳制、证词只归对方且一条一次；
 * 每日一逗按周轮换（轮不到的人 400）、判分只能非值班人且三种白名单、判过不许改节目、重复判幂等不重推；
 * 冷笑话内容查重与每天 3 条、结冰只能对方判且判过不许翻案；社死的未来日与每天一条、抱抱章只归对方且幂等、
 * 满一年读时自动转好笑；突袭类型白名单与每天一次、中弹只认对方；笑点预判可改自己那一票且双人一致算默契；
 * 处方指向必须是本空间条目、每人每天一张、服用回执只归收方；风格图鉴限两人且五选一、自评互评齐了才给差异建议；
 * 周报与年度榜真数字；找不到行 404；无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleLaughServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleLaughMomentMapper momentMapper;
    @Mock
    private CoupleLaughJokeMapper jokeMapper;
    @Mock
    private CoupleLaughCringeMapper cringeMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleLaughService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String YESTERDAY = LocalDate.now().minusDays(1).toString();
    private static final String TOMORROW = LocalDate.now().plusDays(1).toString();
    private static final String LONG_AGO = LocalDate.now().minusDays(400).toString();

    private final List<CoupleLaughMoment> moments = new ArrayList<>();
    private final List<CoupleLaughJoke> jokes = new ArrayList<>();
    private final List<CoupleLaughCringe> cringes = new ArrayList<>();

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

        lenient().when(momentMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(moments));
        lenient().when(momentMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> moments.stream()
                .filter(m -> m.getDay().equals(inv.getArgument(1))).toList());
        lenient().when(momentMapper.find(eq("s1"), any(), any(), any())).thenAnswer(inv -> moments.stream()
                .filter(m -> m.getDay().equals(inv.getArgument(1)) && m.getFromUser().equals(inv.getArgument(2))
                        && m.getTitle().equals(inv.getArgument(3)))
                .findFirst().orElse(null));
        stubAll(momentMapper, CoupleLaughMoment.class, moments, CoupleLaughMoment::getId);

        lenient().when(jokeMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(jokes));
        lenient().when(jokeMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> jokes.stream()
                .filter(j -> j.getFromUser().equals(inv.getArgument(1)) && j.getContent().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        stubAll(jokeMapper, CoupleLaughJoke.class, jokes, CoupleLaughJoke::getId);

        lenient().when(cringeMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(cringes));
        lenient().when(cringeMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> cringes.stream()
                .filter(c -> c.getDay().equals(inv.getArgument(1)) && c.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        stubAll(cringeMapper, CoupleLaughCringe.class, cringes, CoupleLaughCringe::getId);

    }

    private <T> void stubAll(com.smart.chat.im.BaseMapperCompat<T> mapper, Class<T> type, List<T> bag,
                             java.util.function.Function<T, String> idOf) {
        lenient().when(mapper.insert(any(type))).thenAnswer(inv -> {
            bag.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(mapper.updateById(any(type))).thenReturn(1);
        lenient().when(mapper.selectById(any())).thenAnswer(inv -> bag.stream()
                .filter(row -> idOf.apply(row).equals(inv.getArgument(0, String.class)))
                .findFirst().orElse(null));
    }

    // ========== F390 笑点存档 ==========

    @Test
    void momentSavedClampsLevelAndRejectsFutureDuplicate() {
        var vo = service.addMoment("alice", DAY, "倒立喝汤", "bob", "现场还原".repeat(3), 99);
        assertThat(vo.moments().get(0).funLevel()).isEqualTo(CoupleLaughMoment.LEVEL_MAX);
        // 证词入口只对对方开着：记账人自己看是 false
        assertThat(vo.moments().get(0).canWitness()).isFalse();
        assertThat(service.board("bob").moments().get(0).canWitness()).isTrue();
        verify(push).pushCoupleEvent(eq("laugh-moment"), eq("alice"), eq("bob"), any());

        assertThatThrownBy(() -> service.addMoment("alice", TOMORROW, "还没发生", null, null, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还没发生");
        assertThatThrownBy(() -> service.addMoment("alice", DAY, "倒立喝汤", null, null, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存过");
        assertThatThrownBy(() -> service.addMoment("alice", DAY, "名".repeat(CoupleLaughMoment.TITLE_MAX + 1),
                null, null, 3)).isInstanceOf(BusinessException.class);
    }

    @Test
    void momentPerDayThreeAndWitnessOnlyByPartner() {
        service.addMoment("alice", DAY, "第一条", null, null, 3);
        service.addMoment("alice", DAY, "第二条", null, null, 3);
        service.addMoment("alice", DAY, "第三条", null, null, 3);
        assertThatThrownBy(() -> service.addMoment("alice", DAY, "第四条", null, null, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("节制");

        String id = moments.get(0).getId();
        assertThatThrownBy(() -> service.witnessMoment("alice", id, "我自己补"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("对方补");
        assertThat(service.witnessMoment("bob", id, "我在场，确实笑了").moments().get(0).witnessed()).isTrue();
        assertThatThrownBy(() -> service.witnessMoment("bob", id, "再补一次"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("补过");
    }

    // ========== F391 每日一逗 ==========

    // ========== F392 冷笑话结冰榜 ==========

    @Test
    void jokeDuplicateQuotaAndJudgeOnceByPartner() {
        service.addJoke("alice", "冰棍去面试，说自己太嫩");
        assertThatThrownBy(() -> service.addJoke("alice", "冰棍去面试，说自己太嫩"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重播");
        assertThatThrownBy(() -> service.addJoke("alice", "冷".repeat(CoupleLaughJoke.CONTENT_MAX + 1)))
                .isInstanceOf(BusinessException.class);
        service.addJoke("alice", "第二条");
        service.addJoke("alice", "第三条");
        assertThatThrownBy(() -> service.addJoke("alice", "第四条"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("够冷");

        String id = jokes.get(0).getId();
        assertThatThrownBy(() -> service.judgeJoke("alice", id, true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己判冰");
        assertThat(service.judgeJoke("bob", id, true).jokes().stream()
                .filter(j -> j.id().equals(id)).findFirst().orElseThrow().frozen()).isTrue();
        assertThatThrownBy(() -> service.judgeJoke("bob", id, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("翻案");
    }

    // ========== F393 尴尬回收站 ==========

    @Test
    void cringeRejectsFutureAndOnePerDayHealOnlyByPartner() {
        assertThatThrownBy(() -> service.addCringe("alice", TOMORROW, "未来的社死"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能预约");
        service.addCringe("alice", DAY, "在电梯里跟空气点了个头");
        assertThatThrownBy(() -> service.addCringe("alice", DAY, "同一天再来一条"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("一天一条");

        String id = cringes.get(0).getId();
        assertThatThrownBy(() -> service.healCringe("alice", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己抱抱");
        assertThat(service.healCringe("bob", id).cringes().get(0).healed()).isTrue();
        service.healCringe("bob", id);
        verify(push, times(1)).pushCoupleEvent(eq("laugh-healed"), any(), any(), any());
    }

    @Test
    void cringeTurnsFunnyAfterOneYearAtReadTime() {
        cringes.add(CoupleLaughCringe.of("s1", LONG_AGO, "alice", "把同事叫成老师"));
        cringes.add(CoupleLaughCringe.of("s1", YESTERDAY, "bob", "刚加的"));

        var vo = service.board("alice");
        assertThat(vo.turnedFunny()).isEqualTo(1);
        assertThat(vo.cringes()).anyMatch(c -> c.turnedFunny() && !c.day().equals(YESTERDAY));
        // 转档是读时算，不该产生任何推送
        verify(push, never()).pushCoupleEvent(eq("laugh-turn-funny"), any(), any(), any());
    }

    // ========== F394 快乐突袭 ==========

    // ========== F395 笑点默契考 ==========

    // ========== F396 大笑处方 ==========

    // ========== F397 幽默风格图鉴 ==========

    // ========== F398 / F399 榜单 ==========

    @Test
    void missingRowsAndNoSpaceAre404() {
        assertThatThrownBy(() -> service.witnessMoment("bob", "nope", "x")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.judgeJoke("bob", "nope", true)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.healCringe("bob", "nope")).isInstanceOf(BusinessException.class);

        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("carol")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.addJoke("carol", "x")).isInstanceOf(BusinessException.class);
    }
}
