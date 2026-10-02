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
    private CoupleLaughDailyMapper dailyMapper;
    @Mock
    private CoupleLaughJokeMapper jokeMapper;
    @Mock
    private CoupleLaughCringeMapper cringeMapper;
    @Mock
    private CoupleLaughAttackMapper attackMapper;
    @Mock
    private CoupleLaughGuessMapper guessMapper;
    @Mock
    private CoupleLaughRxMapper rxMapper;
    @Mock
    private CoupleLaughStyleMapper styleMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleLaughService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String YESTERDAY = LocalDate.now().minusDays(1).toString();
    private static final String TOMORROW = LocalDate.now().plusDays(1).toString();
    private static final String LONG_AGO = LocalDate.now().minusDays(400).toString();

    private final List<CoupleLaughMoment> moments = new ArrayList<>();
    private final List<CoupleLaughDaily> dailies = new ArrayList<>();
    private final List<CoupleLaughJoke> jokes = new ArrayList<>();
    private final List<CoupleLaughCringe> cringes = new ArrayList<>();
    private final List<CoupleLaughAttack> attacks = new ArrayList<>();
    private final List<CoupleLaughGuess> guesses = new ArrayList<>();
    private final List<CoupleLaughRx> rxList = new ArrayList<>();
    private final List<CoupleLaughStyle> styles = new ArrayList<>();

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

        lenient().when(dailyMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(dailies));
        lenient().when(dailyMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> dailies.stream()
                .filter(d -> d.getDay().equals(inv.getArgument(1))).findFirst().orElse(null));
        stubAll(dailyMapper, CoupleLaughDaily.class, dailies, CoupleLaughDaily::getId);

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

        lenient().when(attackMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(attacks));
        lenient().when(attackMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> attacks.stream()
                .filter(a -> a.getDay().equals(inv.getArgument(1))).toList());
        lenient().when(attackMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> attacks.stream()
                .filter(a -> a.getFromUser().equals(inv.getArgument(1)) && a.getDay().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        stubAll(attackMapper, CoupleLaughAttack.class, attacks, CoupleLaughAttack::getId);

        lenient().when(guessMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(guesses));
        lenient().when(guessMapper.findByJoke(eq("s1"), any())).thenAnswer(inv -> guesses.stream()
                .filter(g -> g.getJokeId().equals(inv.getArgument(1))).toList());
        lenient().when(guessMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> guesses.stream()
                .filter(g -> g.getJokeId().equals(inv.getArgument(1)) && g.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        stubAll(guessMapper, CoupleLaughGuess.class, guesses, CoupleLaughGuess::getId);

        lenient().when(rxMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(rxList));
        lenient().when(rxMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> rxList.stream()
                .filter(r -> r.getFromUser().equals(inv.getArgument(1)) && r.getDay().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        stubAll(rxMapper, CoupleLaughRx.class, rxList, CoupleLaughRx::getId);

        lenient().when(styleMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(styles));
        lenient().when(styleMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> styles.stream()
                .filter(s -> s.getAboutUser().equals(inv.getArgument(1)) && s.getRater().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        stubAll(styleMapper, CoupleLaughStyle.class, styles, CoupleLaughStyle::getId);
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

    /** 从总览的轮换提示里读出今天值班的人（无行时提示必给）。 */
    private String onDuty() {
        return service.board("alice").rotationHint().contains("alice") ? "alice" : "bob";
    }

    private String offDuty() {
        return onDuty().equals("alice") ? "bob" : "alice";
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

    @Test
    void dailyRotationBlocksOffDutyAndJudgeOnlyByOtherSide() {
        String duty = onDuty();
        String off = offDuty();

        assertThatThrownBy(() -> service.serveDaily(off, "讲个谐音梗"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("轮不到你");

        service.serveDaily(duty, "为什么冷笑话会冷");
        assertThat(service.board(duty).today().content()).contains("冷笑话");
        assertThatThrownBy(() -> service.judgeDaily(duty, dailies.get(0).getId(), "HAPPY"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己判");
        assertThat(service.judgeDaily(off, dailies.get(0).getId(), "FAKE").today().verdictLabel())
                .isEqualTo("强撑的笑");
        assertThatThrownBy(() -> service.judgeDaily(off, dailies.get(0).getId(), "MAYBE"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("三种");
    }

    @Test
    void dailyJudgeIdempotentAndBlocksRewriteAfterJudge() {
        String duty = onDuty();
        String off = offDuty();
        service.serveDaily(duty, "节目一");
        service.serveDaily(duty, "改写后的节目");
        String id = dailies.get(0).getId();

        service.judgeDaily(off, id, "HAPPY");
        service.judgeDaily(off, id, "FLAT");

        verify(push, times(1)).pushCoupleEvent(eq("laugh-daily-judge"), any(), any(), any());
        assertThatThrownBy(() -> service.serveDaily(duty, "判完了还想改"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("判过分");
        assertThat(dailies.get(0).getVerdict()).isEqualTo("HAPPY");
    }

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

    @Test
    void weekFrozenCountFeedsYearKingOfCold() {
        service.addJoke("alice", "甲");
        service.addJoke("bob", "乙");
        service.judgeJoke("bob", jokes.get(0).getId(), true);
        service.judgeJoke("alice", jokes.get(1).getId(), true);

        var year = service.board("alice").year();
        assertThat(year.frozen()).isEqualTo(2);
        assertThat(year.kingOfCold()).isIn("alice", "bob");
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

    @Test
    void attackKindWhitelistOncePerDayAndHitByPartner() {
        assertThatThrownBy(() -> service.addAttack("alice", "POEM", "一首诗"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("三种");
        service.addAttack("alice", "praise", "你今天特别好闻");
        assertThatThrownBy(() -> service.addAttack("alice", "MEME", "今天还想突袭"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("突袭过一次");

        String id = attacks.get(0).getId();
        assertThatThrownBy(() -> service.hitAttack("alice", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己认");
        assertThat(service.hitAttack("bob", id).attacks().get(0).hit()).isTrue();
        service.hitAttack("bob", id);
        verify(push, times(1)).pushCoupleEvent(eq("laugh-hit"), any(), any(), any());
        assertThat(service.board("alice").attacks().get(0).kindLabel()).isEqualTo("一串夸奖");
    }

    // ========== F395 笑点默契考 ==========

    @Test
    void guessUpsertsOwnVoteAndTwinNeedsBothSides() {
        service.addJoke("alice", "冰箱门开着");
        String jokeId = jokes.get(0).getId();

        assertThat(service.guessJoke("alice", jokeId, true).guesses().get(0).twin()).isFalse();
        assertThat(service.guessJoke("alice", jokeId, false).guesses().get(0).minePredicted()).isTrue();
        var vo = service.guessJoke("bob", jokeId, false);
        assertThat(vo.guesses().get(0).twin()).isTrue();
        assertThat(vo.guesses().get(0).predictCount()).isEqualTo(2);
        assertThat(guesses).hasSize(2);
    }

    // ========== F396 大笑处方 ==========

    @Test
    void rxRejectsForeignTargetAndTakenOnlyByReceiver() {
        assertThatThrownBy(() -> service.addRx("alice", "POEM", "x", "看看"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只能指向");
        assertThatThrownBy(() -> service.addRx("alice", "MOMENT", "nope", "看看"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不在这儿");

        service.addMoment("alice", DAY, "笑一条", null, null, 5);
        service.addRx("bob", "MOMENT", moments.get(0).getId(), "笑一个");
        assertThatThrownBy(() -> service.addRx("bob", "MOMENT", moments.get(0).getId(), "再来一张"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("开过");

        String id = rxList.get(0).getId();
        assertThatThrownBy(() -> service.takeRx("bob", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("对方吃");
        assertThat(service.takeRx("alice", id).rxList().get(0).taken()).isTrue();
        service.takeRx("alice", id);
        verify(push, times(1)).pushCoupleEvent(eq("laugh-rx-taken"), any(), any(), any());
    }

    // ========== F397 幽默风格图鉴 ==========

    @Test
    void styleLimitedToCoupleAndHintTurnsToDiffAdvice() {
        assertThatThrownBy(() -> service.setStyle("alice", "carol", "PUN", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("给你们俩评");
        assertThatThrownBy(() -> service.setStyle("alice", "alice", "DRY", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("五种");

        service.setStyle("alice", "alice", "PUN", "我自认谐音梗");
        assertThat(service.board("alice").styleHint()).contains("还没填满");

        service.setStyle("bob", "alice", "COLD", "你太冷了");
        assertThat(service.board("alice").styleHint()).contains("不一样").contains("谐音梗").contains("冷幽默");

        service.setStyle("bob", "alice", "PUN", "改判谐音梗");
        assertThat(service.board("alice").styleHint()).contains("对上了");
        assertThat(styles).hasSize(2);
    }

    // ========== F398 / F399 榜单 ==========

    @Test
    void weekAndYearAggregateRealNumbers() {
        service.addMoment("alice", DAY, "倒立喝汤", "bob", "全家笑翻", 5);
        service.witnessMoment("bob", moments.get(0).getId(), "我在场");
        service.addJoke("alice", "冰棍面试");
        service.judgeJoke("bob", jokes.get(0).getId(), true);
        service.addCringe("bob", LONG_AGO, "叫错人");
        service.healCringe("alice", cringes.get(0).getId());
        service.addAttack("alice", "MEME", "丢一个梗");
        service.hitAttack("bob", attacks.get(0).getId());
        service.addRx("bob", "MOMENT", moments.get(0).getId(), "再笑一次");
        service.takeRx("alice", rxList.get(0).getId());

        var week = service.weekReport("alice");
        assertThat(week.moments()).isEqualTo(1);
        assertThat(week.frozen()).isEqualTo(1);
        assertThat(week.hits()).isEqualTo(1);
        assertThat(week.summary()).contains("周欢乐账");

        var year = service.board("alice").year();
        assertThat(year.bestLine()).isEqualTo("倒立喝汤");
        assertThat(year.laughs()).isEqualTo(1);
        assertThat(year.rxTaken()).isEqualTo(1);
        // 年报按「社死日」归年：400 天前那条属去年，今年既不计条目也不计转档
        assertThat(year.cringe()).isZero();
        assertThat(year.turns()).isZero();
        assertThat(service.board("alice").turnedFunny()).isEqualTo(1);
        assertThat(year.summary()).contains("喜剧奖");
        assertThatThrownBy(() -> service.yearReport("alice", "26"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("yyyy");
    }

    @Test
    void missingRowsAndNoSpaceAre404() {
        assertThatThrownBy(() -> service.witnessMoment("bob", "nope", "x")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.judgeDaily("bob", "nope", "HAPPY")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.judgeJoke("bob", "nope", true)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.healCringe("bob", "nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.hitAttack("bob", "nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.takeRx("alice", "nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.guessJoke("alice", "nope", true)).isInstanceOf(BusinessException.class);

        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("carol")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.addJoke("carol", "x")).isInstanceOf(BusinessException.class);
    }
}
