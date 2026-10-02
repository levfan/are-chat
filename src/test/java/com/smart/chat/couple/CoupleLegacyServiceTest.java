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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 传世系统（F340-F349）单测：十问逐格填写与写满才推、格数越界；年审三条上限与同年改卷；
 * 发言稿本人可重发并作废评分、评分卡只能对方打且一季一次；汇率两人各报才结算且同年不重结；
 * 品牌发布权在对方、改动后需重新确认；年度盘点用真数字组文且按年 upsert；
 * 传世清单查重与封存签字权；抽奖每人一年一次、周年当天读时提醒；里程碑按近 30 天速率倒推；
 * 空间等级读时算；无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleLegacyServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleLegacyTenMapper tenMapper;
    @Mock
    private CoupleLegacyAuditMapper auditMapper;
    @Mock
    private CoupleLegacySpeechMapper speechMapper;
    @Mock
    private CoupleLegacyFxMapper fxMapper;
    @Mock
    private CoupleLegacyBrandMapper brandMapper;
    @Mock
    private CoupleLegacyReviewMapper reviewMapper;
    @Mock
    private CoupleLegacyItemMapper itemMapper;
    @Mock
    private CoupleLegacyDrawMapper drawMapper;
    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private CoupleQuoteMapper quoteMapper;
    @Mock
    private CoupleTicketMapper ticketMapper;
    @Mock
    private CoupleFirstMapper firstMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleLegacyService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String YEAR = String.valueOf(LocalDate.now().getYear());
    private static final String LAST_YEAR = String.valueOf(LocalDate.now().getYear() - 1);

    private final List<CoupleLegacyTen> tens = new ArrayList<>();
    private final List<CoupleLegacyAudit> audits = new ArrayList<>();
    private final List<CoupleLegacySpeech> speeches = new ArrayList<>();
    private final List<CoupleLegacyFx> fxes = new ArrayList<>();
    private final List<CoupleLegacyBrand> brands = new ArrayList<>();
    private final List<CoupleLegacyReview> reviews = new ArrayList<>();
    private final List<CoupleLegacyItem> items = new ArrayList<>();
    private final List<CoupleLegacyDraw> draws = new ArrayList<>();
    private final List<CouplePointLedger> ledger = new ArrayList<>();

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

        lenient().when(tenMapper.findByYearUser(eq("s1"), any(), any())).thenAnswer(inv -> tens.stream()
                .filter(t -> t.getYear().equals(inv.getArgument(1)) && t.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(tenMapper.findByYear(eq("s1"), any())).thenAnswer(inv -> tens.stream()
                .filter(t -> t.getYear().equals(inv.getArgument(1))).toList());
        lenient().when(tenMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(tens));
        lenient().when(tenMapper.insert(any(CoupleLegacyTen.class))).thenAnswer(inv -> {
            tens.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(auditMapper.findByYear(eq("s1"), any())).thenAnswer(inv -> audits.stream()
                .filter(a -> a.getYear().equals(inv.getArgument(1))).toList());
        lenient().when(auditMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(audits));
        lenient().when(auditMapper.insert(any(CoupleLegacyAudit.class))).thenAnswer(inv -> {
            audits.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(speechMapper.findByYear(eq("s1"), any())).thenAnswer(inv -> speeches.stream()
                .filter(s -> s.getYear().equals(inv.getArgument(1))).toList());
        lenient().when(speechMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(speeches));
        lenient().when(speechMapper.insert(any(CoupleLegacySpeech.class))).thenAnswer(inv -> {
            speeches.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(fxMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> fxes.stream()
                .filter(f -> f.getFromUser().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(fxMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(fxes));
        lenient().when(fxMapper.insert(any(CoupleLegacyFx.class))).thenAnswer(inv -> {
            fxes.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(brandMapper.find("s1")).thenAnswer(inv -> brands.stream().findFirst().orElse(null));
        lenient().when(brandMapper.insert(any(CoupleLegacyBrand.class))).thenAnswer(inv -> {
            brands.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(reviewMapper.findByYear(eq("s1"), any())).thenAnswer(inv -> reviews.stream()
                .filter(r -> r.getYear().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(reviewMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(reviews));
        lenient().when(reviewMapper.insert(any(CoupleLegacyReview.class))).thenAnswer(inv -> {
            reviews.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(itemMapper.findByItem(eq("s1"), any())).thenAnswer(inv -> items.stream()
                .filter(i -> i.getItem().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(itemMapper.findOpen("s1")).thenAnswer(inv -> items.stream()
                .filter(i -> CoupleLegacyItem.STATUS_OPEN.equals(i.getStatus())).toList());
        lenient().when(itemMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(items));
        lenient().when(itemMapper.insert(any(CoupleLegacyItem.class))).thenAnswer(inv -> {
            items.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(drawMapper.findByYear(eq("s1"), any())).thenAnswer(inv -> draws.stream()
                .filter(d -> d.getYear().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(drawMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(draws));
        lenient().when(drawMapper.insert(any(CoupleLegacyDraw.class))).thenAnswer(inv -> {
            draws.add(inv.getArgument(0));
            return 1;
        });

        lenient().when(ledgerMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(ledger));
        CoupleQuote quote = new CoupleQuote();
        quote.setSpaceId("s1");
        quote.setContent("你说「随便」的时候其实已经想好了三家店");
        lenient().when(quoteMapper.findBySpace("s1")).thenReturn(List.of(quote));
        CoupleTicket ticket = new CoupleTicket();
        ticket.setSpaceId("s1");
        ticket.setTitle("深夜场那部烂片");
        lenient().when(ticketMapper.findBySpace("s1")).thenReturn(List.of(ticket));
        lenient().when(firstMapper.findBySpace("s1")).thenReturn(List.of());
    }

    // ========== F340 年度十问 ==========

    @Test
    void tenAnswersAreSlottedAndCompletionPushesOnce() {
        assertThatThrownBy(() -> service.tenAnswer("alice", YEAR, 11, "越界"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("1-10 题");
        assertThatThrownBy(() -> service.tenAnswer("alice", YEAR, 1, "字".repeat(141)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 140 字");
        assertThatThrownBy(() -> service.tenAnswer("alice", "26", 1, "答"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy");
        assertThatThrownBy(() -> service.tenAnswer("alice", YEAR, 2, "  "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("答一句");

        for (int i = 1; i <= 10; i++) {
            service.tenAnswer("alice", YEAR, i, "第" + i + "答");
        }
        verify(push, times(1)).pushCoupleEvent(eq("legacy-ten-done"), eq("alice"), eq("bob"), any());
        assertThat(tens).hasSize(1);
        CoupleLegacyService.TenVO vo = service.legacy("alice", null).tens().get(0);
        assertThat(vo.answeredCount()).isEqualTo(10);
        assertThat(vo.myAnswers().get(3)).isEqualTo("第4答");
        assertThat(vo.bothDone()).isFalse();

        // 改写不重复推完成
        service.tenAnswer("alice", YEAR, 5, "改写第五答");
        verify(push, times(1)).pushCoupleEvent(eq("legacy-ten-done"), eq("alice"), eq("bob"), any());
        assertThat(tens.get(0).getAnswers()).contains("改写第五答");

        // 逐题钳制：bob 一格没答，就看不到 alice 已答的十格
        CoupleLegacyService.TenVO blind = service.legacy("bob", null).tens().get(0);
        assertThat(blind.partnerAnswers()).hasSize(10);
        assertThat(blind.partnerAnswers()).allMatch(String::isEmpty);

        for (int i = 1; i <= 10; i++) {
            service.tenAnswer("bob", YEAR, i, "TA" + i);
        }
        // bob 答完第 1 格之前 alice 也看不到那格：先改 bob 的第 1 格，alice 侧应立刻可见
        assertThat(service.legacy("alice", null).tens().get(0).bothDone()).isTrue();
        List<String> revealed = service.legacy("alice", null).tens().get(0).partnerAnswers();
        assertThat(revealed).hasSize(10);
        assertThat(revealed.get(0)).isEqualTo("TA1");
        assertThat(revealed.get(9)).isEqualTo("TA10");
    }

    // ========== F341 记忆库年审 ==========

    @Test
    void auditCapsThreeAndRewritesSameYear() {
        assertThatThrownBy(() -> service.audit("alice", YEAR, "留1,留2,留3,留4", "", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 3 条");
        assertThatThrownBy(() -> service.audit("alice", YEAR, "", "", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("至少留一条");
        assertThatThrownBy(() -> service.audit("alice", YEAR, "留" + "字".repeat(70), "", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("每条最多 60 字");

        service.audit("alice", YEAR, "那年的票根,第一次的录音", "那次吵架的长文", "留证据别留情绪");
        verify(push).pushCoupleEvent(eq("legacy-audit"), eq("alice"), eq("bob"), any());
        service.audit("bob", YEAR, "猫的照片", "糊掉的自拍", "");
        assertThat(audits).hasSize(2);
        service.audit("alice", YEAR, "只留一条", "", "改口了");
        assertThat(audits).hasSize(2);
        CoupleLegacyService.AuditVO vo = service.legacy("alice", null).audits().get(0);
        assertThat(vo.keepThree()).containsExactly("只留一条");
        assertThat(vo.submitted()).isEqualTo(2);
    }

    // ========== F342 续约发布会 ==========

    @Test
    void speechResendVoidScoreAndRateBelongsToPartner() {
        assertThatThrownBy(() -> service.speech("alice", YEAR, "  "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("说一句");
        service.speech("alice", YEAR, "今年我学会了先闭嘴再讲理");
        verify(push).pushCoupleEvent(eq("legacy-speech"), eq("alice"), eq("bob"), any());

        assertThatThrownBy(() -> service.speechRate("alice", YEAR, 5, "给自己满分"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("TA 还没发");
        assertThatThrownBy(() -> service.speechRate("bob", YEAR, 6, ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("1-5 档");
        service.speechRate("bob", YEAR, 4, "具体但少了一句你要什么");
        verify(push).pushCoupleEventBoth(eq("legacy-speech-rated"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.speechRate("bob", YEAR, 5, "再来一次"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("打过了");

        service.speech("alice", YEAR, "重发：明年我要你每周陪我走两次");
        assertThat(speeches.get(0).getScore()).isNull();
        assertThat(speeches.get(0).getRatedBy()).isEmpty();
        assertThatThrownBy(() -> service.speechRate("bob", LAST_YEAR, 3, ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("TA 还没发");
        assertThat(service.legacy("bob", null).speeches().get(0).canRate()).isTrue();
    }

    // ========== F344 恋爱汇率 ==========

    @Test
    void fxNeedsBothSidesAndSettlesOncePerYear() {
        assertThatThrownBy(() -> service.fx("alice", 0, 3))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只能填 1-20");
        service.fx("alice", 5, 3);
        verify(push).pushCoupleEvent(eq("legacy-fx"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.fxSettle("alice", YEAR))
                .isInstanceOf(BusinessException.class).hasMessageContaining("两人都报");

        service.fx("bob", 3, 2);
        assertThat(fxes).hasSize(2);
        service.fxSettle("alice", YEAR);
        verify(push).pushCoupleEventBoth(eq("legacy-fx-settled"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThat(fxes).allMatch(f -> YEAR.equals(f.getSettledYear()));
        assertThatThrownBy(() -> service.fxSettle("bob", YEAR))
                .isInstanceOf(BusinessException.class).hasMessageContaining("结算过了");

        service.fx("alice", 8, 4);
        assertThat(fxes).hasSize(2);
        assertThat(service.legacy("alice", null).fxes()).hasSize(2);
        assertThat(service.legacy("alice", null).fxes().get(0).settleLine()).contains("年末汇率结算");
    }

    // ========== F345 情侣品牌 ==========

    @Test
    void brandPublicationNeedsTheOtherSideToConfirm() {
        assertThatThrownBy(() -> service.brand("alice", "名字" + "字".repeat(30), "", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("名字最多 30 字");
        assertThatThrownBy(() -> service.brand("alice", "  ", "", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("叫什么名");

        service.brand("alice", "两个饭桶", "吃在一起，久一点", "主营：一日三餐与深夜谈心");
        verify(push).pushCoupleEvent(eq("legacy-brand"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.brandConfirm("alice"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己确认不作数");
        assertThat(service.legacy("alice", null).brand().published()).isFalse();

        service.brandConfirm("bob");
        verify(push).pushCoupleEventBoth(eq("legacy-brand-published"), eq("bob"), eq("alice"), eq("bob"), any());
        assertThat(service.legacy("alice", null).brand().published()).isTrue();
        service.brandConfirm("bob");
        verify(push, times(1)).pushCoupleEventBoth(eq("legacy-brand-published"), any(), any(), any(), any());

        service.brand("bob", "两个饭桶", "改：一起吃很久", "");
        assertThat(brands.get(0).publishedFlag()).isFalse();
        assertThat(brands.get(0).getByUser()).isEqualTo("bob");
        assertThatThrownBy(() -> service.brandConfirm("bob"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己确认不作数");
    }

    // ========== F346 我们的一年 ==========

    @Test
    void reviewUsesRealNumbersAndUpsertsPerYear() {
        for (int i = 0; i < 10; i++) {
            service.tenAnswer("alice", LAST_YEAR, i + 1, "去年答" + (i + 1));
        }
        service.audit("alice", LAST_YEAR, "票根", "长文", "");
        service.speech("alice", LAST_YEAR, "去年的发言");
        ledger.add(CouplePointLedger.of("s1", "alice", "EARN", "发薪日感谢工资", 5));
        CouplePointLedger old = CouplePointLedger.of("s1", "bob", "EARN", "去年的分", 5);
        old.setCreated(LocalDate.of(LocalDate.now().getYear() - 1, 6, 1)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        ledger.add(old);

        service.review("alice", LAST_YEAR);
        verify(push).pushCoupleEvent(eq("legacy-review"), eq("alice"), eq("bob"), any());
        String content = service.legacy("alice", null).reviews().get(0).content();
        assertThat(content).contains(LAST_YEAR).contains("1 笔互动进了台账").contains("十问共答了 10 条")
                .contains("年审交了 1 份").contains("传世清单在册 0 项");

        service.review("alice", LAST_YEAR);
        assertThat(reviews).hasSize(1);
        assertThatThrownBy(() -> service.review("alice", "abc"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("yyyy");
    }

    // ========== F347 传世清单 ==========

    @Test
    void itemSealNeedsSecondSignature() {
        assertThatThrownBy(() -> service.itemAdd("alice", "", "THING", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("叫什么");
        service.itemAdd("alice", "老屋钥匙", "PLACE", "抽屉第二层，密码是你的生日倒着写");
        verify(push).pushCoupleEvent(eq("legacy-item"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.itemAdd("bob", "老屋钥匙", "THING", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经在清单上");
        assertThatThrownBy(() -> service.itemAdd("bob", "存折", "MONEY", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("PLACE/PASSWORD/THING/WORD");

        String id = items.get(0).getId();
        assertThatThrownBy(() -> service.itemSeal("alice", id))
                .isInstanceOf(BusinessException.class).hasMessageContaining("对方签字");
        service.itemSeal("bob", id);
        assertThat(items.get(0).getStatus()).isEqualTo(CoupleLegacyItem.STATUS_SEALED);
        verify(push).pushCoupleEventBoth(eq("legacy-sealed"), eq("bob"), eq("alice"), eq("bob"), any());
        service.itemSeal("bob", id);
        verify(push, times(1)).pushCoupleEventBoth(eq("legacy-sealed"), any(), any(), any(), any());
        assertThat(service.legacy("bob", null).items().get(0).canSeal()).isFalse();
    }

    // ========== F348 周年抽奖箱 ==========

    @Test
    void drawOnceEachAndAnniversaryReminderSettlesOnRead() {
        assertThat(draws).isEmpty();
        service.draw("alice");
        assertThat(draws).hasSize(1);
        assertThat(draws.get(0).getPrizeA()).isNotBlank();
        verify(push).pushCoupleEvent(eq("legacy-draw"), eq("alice"), eq("bob"), any());
        assertThatThrownBy(() -> service.draw("alice"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("抽过了");
        service.draw("bob");
        assertThat(draws.get(0).drawnBFlag()).isTrue();

        // 周年当天读时提醒（不建定时任务）
        draws.get(0).setNotified(0);
        service.legacy("alice", null);
        verify(push).pushCoupleEventBoth(eq("legacy-draw-remind"), eq("alice"), eq("alice"), eq("bob"), any());
        assertThat(draws.get(0).notifiedFlag()).isTrue();
        service.legacy("alice", null);
        verify(push, times(1)).pushCoupleEventBoth(eq("legacy-draw-remind"), any(), any(), any(), any());

        CoupleLegacyService.DrawVO vo = service.legacy("alice", null).draw();
        assertThat(vo.year()).isEqualTo(YEAR);
        assertThat(vo.drawnMine()).isTrue();
        assertThat(vo.prizeMine()).isNotBlank();
        // 今年一条愿望都没攒过，才回落 Bank 的固定迷你愿望位
        assertThat(vo.prizeMine()).isIn(CoupleLegacyBank.PRIZES.toArray());
    }

    @Test
    void drawPoolEatsOnlyThisYearsEarnedWishes() {
        ledger.add(CouplePointLedger.of("s1", "alice", "EARN", "陪看一部老片", 5));
        ledger.add(CouplePointLedger.of("s1", "bob", "EARN", "一次不挑餐厅", 5));
        // SPEND 是花掉的花销、去年 EARN 是去年的愿望，都不该进今年的箱子
        ledger.add(CouplePointLedger.of("s1", "alice", "SPEND", "已经换掉的花销", 3));
        CouplePointLedger lastYear = CouplePointLedger.of("s1", "alice", "EARN", "去年的愿望", 5);
        lastYear.setCreated(LocalDate.of(LocalDate.now().getYear() - 1, 6, 1)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        ledger.add(lastYear);

        service.draw("alice");
        service.draw("bob");

        assertThat(draws.get(0).getPrizeA()).isIn("陪看一部老片", "一次不挑餐厅");
        assertThat(draws.get(0).getPrizeB()).isIn("陪看一部老片", "一次不挑餐厅");
    }

    // ========== F343 里程碑倒推 + F349 空间等级 ==========

    @Test
    void milestoneEstimatesFromRecentRateAndLevelReadsTotal() {
        CoupleLegacyService.MilestoneVO none = service.legacy("alice", 300).milestone();
        assertThat(none.estimateDays()).isEqualTo(-1);
        assertThat(none.advice()).contains("先攒一周");

        for (int i = 0; i < 12; i++) {
            ledger.add(CouplePointLedger.of("s1", "alice", "EARN", "互动" + i, 1));
        }
        CoupleLegacyService.MilestoneVO m = service.legacy("alice", 112).milestone();
        assertThat(m.goal()).isEqualTo(112);
        assertThat(m.achieved()).isEqualTo(12);
        assertThat(m.last30()).isEqualTo(12);
        assertThat(m.estimateDays()).isEqualTo(250);
        assertThat(m.estimateDay()).isNotBlank();
        assertThat(m.advice()).isNotEqualTo("近 30 天没有互动记录，先攒一周再来倒推。");

        // 越界目标回落到默认 300
        assertThat(service.legacy("alice", 5).milestone().goal()).isEqualTo(300);

        List<String> candidates = service.legacy("alice", null).auditCandidates();
        assertThat(candidates).containsExactly("语录：你说「随便」的时候其实已经想…", "票根：深夜场那部烂片");

        CoupleLegacyService.LevelVO lv = service.legacy("alice", null).level();
        assertThat(lv.total()).isEqualTo(12);
        assertThat(lv.level()).isGreaterThanOrEqualTo(1);
        assertThat(lv.title()).isNotBlank();
        assertThat(lv.line()).contains("空间等级 Lv.");

        service.itemAdd("alice", "老屋钥匙", "PLACE", "");
        assertThat(service.legacy("alice", null).level().legacyCount()).isEqualTo(1);
    }

    // ========== 空间校验 ==========

    @Test
    void requiresActiveSpace() {
        when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.legacy("alice", null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("先邀请一位好友");
    }
}
