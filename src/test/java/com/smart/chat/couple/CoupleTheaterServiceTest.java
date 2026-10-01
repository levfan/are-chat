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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 扮演剧场（F300-F309）单测：身份签稳定与日终双评、互换日记双齐互见、师徒归属与定级门槛、
 * 电话亭封存/周年接通与给过去当场接通、黑话收录查重/考卷归属/判卷归属、奥斯卡一日一提名可改、
 * 家长题双答互见、双角色追剧日记上限与剧终合剧本、今日客服接单/评分/差评申诉状态机、
 * 颁奖礼读时聚合、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleTheaterServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleRoleDayMapper roleMapper;
    @Mock
    private CoupleSwapDiaryMapper diaryMapper;
    @Mock
    private CoupleMasterDayMapper masterMapper;
    @Mock
    private CoupleBoothNoteMapper boothMapper;
    @Mock
    private CouplePrivateRefMapper refMapper;
    @Mock
    private CoupleActAwardMapper awardMapper;
    @Mock
    private CoupleIfFamilyMapper familyMapper;
    @Mock
    private CoupleRoleMovieMapper movieMapper;
    @Mock
    private CoupleServiceTicketMapper ticketMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleTheaterService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<CoupleRoleDay> roles = new ArrayList<>();
    private final List<CoupleSwapDiary> diaries = new ArrayList<>();
    private final List<CoupleMasterDay> masters = new ArrayList<>();
    private final List<CoupleBoothNote> booths = new ArrayList<>();
    private final List<CouplePrivateRef> refs = new ArrayList<>();
    private final List<CoupleActAward> awards = new ArrayList<>();
    private final List<CoupleIfFamily> families = new ArrayList<>();
    private final List<CoupleRoleMovie> movies = new ArrayList<>();
    private final List<CoupleServiceTicket> tickets = new ArrayList<>();

    private CoupleSpace space;

    @BeforeEach
    void setUp() {
        space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);

        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        lenient().when(roleMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> roles.stream()
                .filter(r -> r.getDay().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(roleMapper.insert(any(CoupleRoleDay.class))).thenAnswer(inv -> {
            roles.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(diaryMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> diaries.stream()
                .filter(d -> d.getDay().equals(inv.getArgument(1))).toList());
        lenient().when(diaryMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(diaries));
        lenient().when(diaryMapper.insert(any(CoupleSwapDiary.class))).thenAnswer(inv -> {
            diaries.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(masterMapper.findByWeek(eq("s1"), any())).thenAnswer(inv -> masters.stream()
                .filter(m -> m.getWeek().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(masterMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(masters));
        lenient().when(masterMapper.insert(any(CoupleMasterDay.class))).thenAnswer(inv -> {
            masters.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(boothMapper.findDue(eq("s1"), any())).thenAnswer(inv -> {
            String today = inv.getArgument(1);
            return booths.stream().filter(b -> CoupleBoothNote.STATUS_SEALED.equals(b.getStatus())
                    && b.getOpenDay().compareTo(today) <= 0).toList();
        });
        lenient().when(boothMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(booths));
        lenient().when(boothMapper.insert(any(CoupleBoothNote.class))).thenAnswer(inv -> {
            booths.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(refMapper.findByTerm(eq("s1"), any())).thenAnswer(inv -> refs.stream()
                .filter(r -> r.getTerm().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(refMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(refs));
        lenient().when(refMapper.insert(any(CouplePrivateRef.class))).thenAnswer(inv -> {
            refs.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(awardMapper.findByDayUser(eq("s1"), any(), any())).thenAnswer(inv -> awards.stream()
                .filter(a -> a.getDay().equals(inv.getArgument(1)) && a.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(awardMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(awards));
        lenient().when(awardMapper.insert(any(CoupleActAward.class))).thenAnswer(inv -> {
            awards.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(familyMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> families.stream()
                .filter(f -> f.getDay().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(familyMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(families));
        lenient().when(familyMapper.insert(any(CoupleIfFamily.class))).thenAnswer(inv -> {
            families.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(movieMapper.findByWork(eq("s1"), any())).thenAnswer(inv -> movies.stream()
                .filter(m -> m.getWork().equals(inv.getArgument(1))).toList());
        lenient().when(movieMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(movies));
        lenient().when(movieMapper.insert(any(CoupleRoleMovie.class))).thenAnswer(inv -> {
            movies.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(ticketMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(tickets));
        lenient().when(ticketMapper.selectById(any())).thenAnswer(inv -> tickets.stream()
                .filter(t -> t.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        lenient().when(ticketMapper.insert(any(CoupleServiceTicket.class))).thenAnswer(inv -> {
            tickets.add(inv.getArgument(0));
            return 1;
        });
    }

    // ========== F300 今日身份签 ==========

    @Test
    void roleIsStablePerDayAndBothRatingPushesOnce() {
        CoupleTheaterService.TheaterVO first = service.today("alice");
        String name = first.role().roleName();
        assertThat(name).isNotBlank();
        assertThat(first.role().guide()).isNotBlank();
        assertThat(service.today("bob").role().roleName()).isEqualTo(name);
        assertThat(roles).hasSize(1);

        service.rateRole("alice", 4);
        verify(push).pushCoupleEvent(eq("theater-role-rated"), eq("alice"), eq("bob"), any());
        service.rateRole("bob", 5);
        verify(push).pushCoupleEventBoth(eq("theater-role-rated"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(service.today("alice").role().bothRated()).isTrue();
        assertThat(service.today("alice").role().myRate()).isEqualTo(4);

        // 重复打分不再推送，分数保持第一次
        service.rateRole("alice", 1);
        assertThat(roles.get(0).getRateA()).isEqualTo(4);
        verify(push, times(1)).pushCoupleEvent(eq("theater-role-rated"), eq("alice"), eq("bob"), any());

        assertThatThrownBy(() -> service.rateRole("bob", 9))
                .isInstanceOf(BusinessException.class).hasMessageContaining("1-5");
    }

    // ========== F301 一日互换日记 ==========

    @Test
    void swapDiaryStaysHiddenUntilBothPagesIn() {
        service.swapDiary("alice", "今天我替他开会，才发现他被打断三次");
        assertThat(service.today("alice").diaries()).hasSize(1);
        assertThat(service.today("alice").diaries().get(0).bothIn()).isFalse();
        assertThat(service.today("alice").diaries().get(0).partner()).isEmpty();
        verify(push).pushCoupleEvent(eq("theater-diary"), eq("alice"), eq("bob"), any());

        // 本人改写不重推
        service.swapDiary("alice", "改写：他其实忍了一整个下午");
        verify(push, times(1)).pushCoupleEvent(eq("theater-diary"), eq("alice"), eq("bob"), any());

        service.swapDiary("bob", "我替她回了那条最难的消息");
        verify(push).pushCoupleEventBoth(eq("theater-diary-both"), eq("bob"), eq("alice"), eq("bob"), any());
        CoupleTheaterService.DiaryVO vo = service.today("alice").diaries().get(0);
        assertThat(vo.bothIn()).isTrue();
        assertThat(vo.partner()).contains("最难的消息");

        assertThatThrownBy(() -> service.swapDiary("alice", "字".repeat(301)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("300 字");
    }

    // ========== F302 师徒日 ==========

    @Test
    void masterFlowGuardsRolesAndServeTarget() {
        CoupleTheaterService.MasterVO week = service.today("alice").master();
        String master = week.masterUser();
        String apprentice = week.apprenticeUser();
        assertThat(master).isNotEqualTo(apprentice);

        // 师父不能替徒弟打卡
        assertThatThrownBy(() -> service.masterServe(master))
                .isInstanceOf(BusinessException.class).hasMessageContaining("你是师父");

        service.masterServe(apprentice);
        service.masterServe(apprentice);
        assertThat(service.today(apprentice).master().serveCount()).isEqualTo(1);
        assertThat(service.today(apprentice).master().servedToday()).isTrue();

        // 侍奉未满三次不许出师
        assertThatThrownBy(() -> service.masterReview(master, "手艺人", "GRADUATED"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("先留级");
        // 徒弟无权定级
        assertThatThrownBy(() -> service.masterReview(apprentice, "x", "REPEAT"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("师父本人");

        service.masterReview(master, "勉强像样", "REPEAT");
        verify(push).pushCoupleEventBoth(eq("theater-master-grade"), eq(master), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.masterReview(master, "再写一次", "GRADUATED"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("定过级");

        // 徒弟隔日可再打卡（构造昨天）
        CoupleMasterDay row = masters.get(0);
        row.setServes(LocalDate.now().minusDays(1).toString());
        row.setUpdatedAt(System.currentTimeMillis());
        service.masterServe(apprentice);
        assertThat(row.serveCount()).isEqualTo(2);
    }

    // ========== F303 时空电话亭 ==========

    @Test
    void boothFutureSealsThenConnectsOnAnniversary() {
        service.booth("alice", "FUTURE", "一年后的今天，我们还在拌嘴吗");
        CoupleTheaterService.BoothVO vo = service.today("alice").booths().get(0);
        assertThat(vo.status()).isEqualTo(CoupleBoothNote.STATUS_SEALED);
        assertThat(vo.openDay()).isEqualTo(LocalDate.now().plusYears(1).toString());
        assertThat(vo.daysLeft()).isGreaterThan(360L);
        assertThat(vo.line()).isEmpty();
        verify(push).pushCoupleEvent(eq("theater-booth-sealed"), eq("alice"), eq("bob"), any());

        booths.get(0).setOpenDay(DAY);
        service.today("bob");
        assertThat(booths.get(0).getStatus()).isEqualTo(CoupleBoothNote.STATUS_SENT);
        verify(push).pushCoupleEventBoth(eq("theater-booth-connected"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThat(service.today("bob").booths().get(0).line()).isNotBlank();

        assertThatThrownBy(() -> service.booth("alice", "MARS", "喂"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("两种去向");
    }

    @Test
    void boothPastConnectsImmediatelyWithStaticNoise() {
        service.booth("bob", "PAST", "去年的我们，别怕那次搬家");
        assertThat(booths.get(0).getStatus()).isEqualTo(CoupleBoothNote.STATUS_SENT);
        verify(push).pushCoupleEventBoth(eq("theater-booth-past"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(service.today("alice").booths().get(0).line()).isNotBlank();
        assertThatThrownBy(() -> service.booth("bob", "PAST", "字".repeat(301)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("300 字");
    }

    // ========== F304 黑话大全 ==========

    @Test
    void refQuizAndJudgeOwnershipChain() {
        service.addRef("alice", "橘子", "指代那次没接起来的电话", "1 月 3 日楼下");
        verify(push).pushCoupleEvent(eq("theater-ref"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.addRef("bob", "橘子", "重复", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经收进");

        // 收录人不能考自己
        assertThatThrownBy(() -> service.quizRef("alice", "橘子", "就是水果"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不能考自己");
        service.quizRef("bob", "橘子", "应该是那通没接的电话");
        verify(push).pushCoupleEvent(eq("theater-ref-quiz"), eq("bob"), eq("alice"), any());

        // 答题人不能判自己的卷
        assertThatThrownBy(() -> service.judgeRef("bob", "橘子", true))
                .isInstanceOf(BusinessException.class).hasMessageContaining("收录人");
        service.judgeRef("alice", "橘子", true);
        verify(push).pushCoupleEventBoth(eq("theater-ref-judged"), eq("alice"), eq("alice"), eq("bob"), any());
        CoupleTheaterService.RefVO vo = service.today("bob").refs().get(0);
        assertThat(vo.judged()).isEqualTo("RIGHT");
        assertThat(vo.canQuiz()).isFalse();

        assertThatThrownBy(() -> service.quizRef("bob", "不存在", "x"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("查不到");
        assertThat(refs).hasSize(1);
    }

    // ========== F305 每日奥斯卡 ==========

    @Test
    void awardOneNominationPerPersonPerDayButRewritable() {
        service.nominate("alice", "她把「随便」演成了三个方案");
        verify(push).pushCoupleEvent(eq("theater-award"), eq("alice"), eq("bob"), any());
        service.nominate("alice", "改：眼神已经写好了方案");
        assertThat(awards).hasSize(1);
        verify(push, times(1)).pushCoupleEvent(eq("theater-award"), eq("alice"), eq("bob"), any());
        service.nominate("bob", "他今天一句话没说，演技满分");
        assertThat(awards).hasSize(2);
        CoupleTheaterService.AwardVO vo = service.today("alice").awards().get(0);
        assertThat(vo.line()).contains("bob");
        assertThatThrownBy(() -> service.nominate("alice", "字".repeat(141)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("140 字");
    }

    // ========== F306 如果我是你爸妈 ==========

    @Test
    void familyQuestionStableAndBothReveal() {
        CoupleTheaterService.TheaterVO vo = service.today("alice");
        String q = vo.family().question();
        assertThat(q).isNotBlank();
        assertThat(service.today("bob").family().question()).isEqualTo(q);

        service.familyAnswer("alice", "我会先问他到底怕什么");
        verify(push).pushCoupleEvent(eq("theater-family"), eq("alice"), eq("bob"), any());
        assertThat(service.today("alice").family().partnerAnswer()).isEmpty();

        service.familyAnswer("bob", "我直接把我妈的顾虑念出来");
        verify(push).pushCoupleEventBoth(eq("theater-family-both"), eq("bob"), eq("alice"), eq("bob"), any());
        CoupleTheaterService.FamilyVO both = service.today("alice").family();
        assertThat(both.bothIn()).isTrue();
        assertThat(both.partnerAnswer()).contains("顾虑");

        // 改写不再重推
        service.familyAnswer("alice", "改：先问他上次为什么沉默");
        verify(push, times(1)).pushCoupleEventBoth(eq("theater-family-both"), any(), any(), any(), any());
    }

    // ========== F307 双角色追剧 ==========

    @Test
    void movieScriptMergesOnlyWhenBothFinished() {
        service.movieWrite("alice", "山海之间", "女警", "第 3 集我替她值了夜班");
        verify(push).pushCoupleEvent(eq("theater-movie"), eq("alice"), eq("bob"), any());
        service.movieWrite("bob", "山海之间", "嫌疑人", "第 3 集我被他那句「没有」骗了");
        assertThat(service.today("alice").movies().get(0).bothClaimed()).isTrue();
        assertThat(service.today("alice").movies().get(0).partnerDiary()).isEmpty();

        service.movieFinish("alice", "山海之间");
        verify(push).pushCoupleEvent(eq("theater-movie-finish"), eq("alice"), eq("bob"), any());
        service.movieFinish("bob", "山海之间");
        verify(push).pushCoupleEventBoth(eq("theater-movie-script"), eq("bob"), eq("alice"), eq("bob"), any());
        CoupleTheaterService.MovieVO done = service.today("alice").movies().get(0);
        assertThat(done.finished()).isTrue();
        assertThat(done.partnerDiary()).contains("骗了");

        // 未认领的人不能剧终
        assertThatThrownBy(() -> service.movieFinish("alice", "另一部"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还没");
    }

    @Test
    void movieDiaryAppendCapsAtSixHundred() {
        service.movieWrite("alice", "长镜头", "摄影师", "a".repeat(190));
        service.movieWrite("alice", "长镜头", "摄影师", "b".repeat(190));
        service.movieWrite("alice", "长镜头", "摄影师", "c".repeat(190));
        assertThat(movies.get(0).getDiary()).hasSize(572);
        assertThatThrownBy(() -> service.movieWrite("alice", "长镜头", "摄影师", "d".repeat(28)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("600 字");
        assertThatThrownBy(() -> service.movieWrite("alice", "长镜头", "摄影师", "e".repeat(201)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("200 字，追更");
    }

    // ========== F308 今日客服 ==========

    @Test
    void serviceTicketStateMachine() {
        service.placeOrder("alice", "把阳台那盆绿萝搬进来");
        verify(push).pushCoupleEvent(eq("theater-order"), eq("alice"), eq("bob"), any());
        String id = tickets.get(0).getId();

        // 自己不能接自己的单
        assertThatThrownBy(() -> service.answerOrder("alice", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己的单");
        service.answerOrder("bob", id);
        assertThat(tickets.get(0).getOnTime()).isEqualTo(1);
        verify(push).pushCoupleEvent(eq("theater-answered"), eq("bob"), eq("alice"), any());
        // 重复接单
        assertThatThrownBy(() -> service.answerOrder("bob", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("有人接过");

        // 客服不能给自己打分
        assertThatThrownBy(() -> service.scoreOrder("bob", id, 5))
                .isInstanceOf(BusinessException.class).hasMessageContaining("归顾客");
        service.scoreOrder("alice", id, 2);
        assertThat(tickets.get(0).getStatus()).isEqualTo(CoupleServiceTicket.STATUS_RATED);

        // 差评可申诉一次，好评不许申诉
        service.appealOrder("bob", id, "我在开会，但已经记住了");
        assertThat(tickets.get(0).getStatus()).isEqualTo(CoupleServiceTicket.STATUS_APPEALED);
        verify(push).pushCoupleEventBoth(eq("theater-appeal"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.appealOrder("bob", id, "再申一次"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("评过分的差评单");

        service.placeOrder("alice", "另一单：顺路买豆浆");
        String second = tickets.get(1).getId();
        service.answerOrder("bob", second);
        service.scoreOrder("alice", second, 5);
        assertThatThrownBy(() -> service.appealOrder("bob", second, "我还能更好"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("好评");
        assertThat(service.today("bob").tickets().get(0).canAnswer()).isFalse();
    }

    @Test
    void serviceTicketGuardsScoreRangeAndUnknownId() {
        service.placeOrder("bob", "洗碗两遍");
        String id = tickets.get(0).getId();
        service.answerOrder("alice", id);
        assertThatThrownBy(() -> service.scoreOrder("bob", id, 0))
                .isInstanceOf(BusinessException.class).hasMessageContaining("1-5");
        assertThatThrownBy(() -> service.answerOrder("alice", "no-such-id"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("不存在");
        assertThatThrownBy(() -> service.placeOrder("bob", "字".repeat(81)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("80 字");
    }

    // ========== F309 冷知识颁奖礼 ==========

    @Test
    void galaAggregatesTheaterCounters() {
        service.nominate("alice", "今日最稳演技");
        service.nominate("bob", "今日最会接梗");
        service.addRef("alice", "橘子", "那通没接的电话", "楼下");
        service.quizRef("bob", "橘子", "电话");
        service.judgeRef("alice", "橘子", true);
        service.swapDiary("alice", "一页");
        service.swapDiary("bob", "另一页");

        CoupleTheaterService.GalaVO gala = service.today("alice").gala();
        assertThat(gala.nominations()).isEqualTo(2);
        assertThat(gala.terms()).isEqualTo(1);
        assertThat(gala.quizzed()).isEqualTo(1);
        assertThat(gala.rights()).isEqualTo(1);
        assertThat(gala.diaryDays()).isEqualTo(1);
        assertThat(gala.prize()).isNotBlank();
        assertThat(gala.line()).contains("2 项提名");
    }

    // ========== 空间校验 ==========

    @Test
    void requiresActiveSpace() {
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.today("alice"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("先邀请一位好友");
    }
}
