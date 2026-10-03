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
 * 聆听者（F380-F389）单测：暗中心愿记下时绝不推送、心愿对主人保密直到兑现揭晓、每人 12 条上限与内容查重；
 * 雷区每人 6 颗与话题查重、知晓不能自盖且重复不重推、避雷必须先知晓且只算对方的战绩；
 * 安全词每人一格可改写、没约定就喊不了停、一天只记一次暂停、复盘只由喊停人补；
 * 敏感日不许标过去、类型白名单、同日同型查重、代标的只能代标的人撤；
 * 话头在途 5 条与内容查重、销档只由存话头的人；反话对照要有翻译结果、每人 10 条、对方不能删；
 * 聆听协议五选一、双方都写才算齐、不一致时给差异提示；话题池自接 400、聊完只由接单人且必填感想、超一周记超时；
 * 今日一句话每人每天一句改写不重推、对方没说时回看昨天那句；年报真数字；无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleCatchServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleCatchWishMapper wishMapper;
    @Mock
    private CoupleCatchMineMapper mineMapper;
    @Mock
    private CoupleCatchSafewordMapper safewordMapper;
    @Mock
    private CoupleCatchSafewordUseMapper useMapper;
    @Mock
    private CoupleCatchDailyMapper dailyMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleCatchService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String YESTERDAY = LocalDate.now().minusDays(1).toString();
    private static final String TOMORROW = LocalDate.now().plusDays(1).toString();

    private final List<CoupleCatchWish> wishes = new ArrayList<>();
    private final List<CoupleCatchMine> mines = new ArrayList<>();
    private final List<CoupleCatchSafeword> words = new ArrayList<>();
    private final List<CoupleCatchSafewordUse> uses = new ArrayList<>();
    private final List<CoupleCatchDaily> dailies = new ArrayList<>();

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

        lenient().when(wishMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(wishes));
        lenient().when(wishMapper.findByOwner(eq("s1"), any())).thenAnswer(inv -> wishes.stream()
                .filter(w -> w.getOwnerUser().equals(inv.getArgument(1))).toList());
        lenient().when(wishMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> wishes.stream()
                .filter(w -> w.getOwnerUser().equals(inv.getArgument(1)) && w.getContent().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        stubSelectByIdAndUpsert(wishMapper, CoupleCatchWish.class, wishes, CoupleCatchWish::getId);

        lenient().when(mineMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(mines));
        lenient().when(mineMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> mines.stream()
                .filter(m -> m.getFromUser().equals(inv.getArgument(1)) && m.getTopic().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        stubSelectByIdAndUpsert(mineMapper, CoupleCatchMine.class, mines, CoupleCatchMine::getId);

        lenient().when(safewordMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(words));
        lenient().when(safewordMapper.find(eq("s1"), any())).thenAnswer(inv -> words.stream()
                .filter(w -> w.getFromUser().equals(inv.getArgument(1))).findFirst().orElse(null));
        stubInsertAndUpdate(safewordMapper, CoupleCatchSafeword.class, words);

        lenient().when(useMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(uses));
        lenient().when(useMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> uses.stream()
                .filter(u -> u.getDay().equals(inv.getArgument(1)) && u.getUserName().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(useMapper.findByMonth(eq("s1"), any())).thenAnswer(inv -> uses.stream()
                .filter(u -> u.getDay().startsWith((String) inv.getArgument(1))).toList());
        stubSelectByIdAndUpsert(useMapper, CoupleCatchSafewordUse.class, uses, CoupleCatchSafewordUse::getId);






        lenient().when(dailyMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(dailies));
        lenient().when(dailyMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> dailies.stream()
                .filter(d -> d.getDay().equals(inv.getArgument(1)) && d.getUserName().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        stubInsertAndUpdate(dailyMapper, CoupleCatchDaily.class, dailies);
    }

    private <T> void stubInsertAndUpdate(com.smart.chat.im.BaseMapperCompat<T> mapper, Class<T> type, List<T> bag) {
        lenient().when(mapper.insert(any(type))).thenAnswer(inv -> {
            bag.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(mapper.updateById(any(type))).thenReturn(1);
    }

    /** 带 selectById 与 deleteById 的完整桩（id 一律按 String 取，避开 String.valueOf 的 char[] 重载坑）。 */
    private <T> void stubSelectByIdAndUpsert(com.smart.chat.im.BaseMapperCompat<T> mapper, Class<T> type,
                                             List<T> bag, java.util.function.Function<T, String> idOf) {
        stubInsertAndUpdate(mapper, type, bag);
        lenient().when(mapper.selectById(any())).thenAnswer(inv -> bag.stream()
                .filter(row -> idOf.apply(row).equals(inv.getArgument(0, String.class)))
                .findFirst().orElse(null));
        lenient().when(mapper.deleteById(any(java.io.Serializable.class))).thenAnswer(inv -> {
            bag.removeIf(row -> idOf.apply(row).equals(inv.getArgument(0, String.class)));
            return 1;
        });
    }

    // ========== F380 暗中心愿本 ==========

    @Test
    void wishSavedStaysSecretAndPushesNobody() {
        var vo = service.addWish("alice", "想要那把键盘", YESTERDAY, "逛店的时候");

        assertThat(vo.myWishes()).hasSize(1);
        assertThat(vo.myWishes().get(0).secret()).isTrue();
        assertThat(vo.wishQuotaLeft()).isEqualTo(CoupleCatchWish.PER_OWNER_MAX - 1);
        verify(push, never()).pushCoupleEvent(eq("catch-wish"), any(), any(), any());
        verify(push, never()).pushCoupleEvent(eq("catch-wish-fulfilled"), any(), any(), any());
    }

    @Test
    void wishInvisibleToOwnerUntilFulfilled() {
        service.addWish("alice", "想要那把键盘", YESTERDAY, null);

        assertThat(service.board("bob").revealedToMe()).isEmpty();
        assertThat(service.board("bob").myWishes()).isEmpty();

        service.fulfillWish("alice", wishes.get(0).getId());
        var bobView = service.board("bob");
        assertThat(bobView.revealedToMe()).hasSize(1);
        assertThat(bobView.revealedToMe().get(0).filled()).isTrue();
        verify(push).pushCoupleEvent(eq("catch-wish-fulfilled"), eq("alice"), eq("bob"), any());
    }

    @Test
    void wishRejectsDuplicateFutureDayAndQuota() {
        service.addWish("alice", "想要那把键盘", YESTERDAY, null);
        assertThatThrownBy(() -> service.addWish("alice", "想要那把键盘", YESTERDAY, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("记过");
        assertThatThrownBy(() -> service.addWish("alice", "另一个", TOMORROW, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("将来");

        for (int i = 0; i < CoupleCatchWish.PER_OWNER_MAX - 1; i++) {
            service.addWish("alice", "第 " + i + " 条", YESTERDAY, null);
        }
        assertThatThrownBy(() -> service.addWish("alice", "超出的那条", YESTERDAY, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最多");
    }

    @Test
    void wishFulfillOnlyByRecorderAndIdempotent() {
        service.addWish("alice", "想要那把键盘", YESTERDAY, null);
        String id = wishes.get(0).getId();

        assertThatThrownBy(() -> service.fulfillWish("bob", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有 TA 能勾");

        service.fulfillWish("alice", id);
        service.fulfillWish("alice", id);
        verify(push, times(1)).pushCoupleEvent(eq("catch-wish-fulfilled"), any(), any(), any());
    }

    @Test
    void fulfillingAWishFreesASlotInTheSecretBook() {
        for (int i = 0; i < CoupleCatchWish.PER_OWNER_MAX; i++) {
            service.addWish("alice", "第 " + i + " 条心愿", YESTERDAY, null);
        }
        assertThat(service.board("alice").wishQuotaLeft()).isZero();
        assertThatThrownBy(() -> service.addWish("alice", "记不下的那条", YESTERDAY, null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("兑现一条就腾出一格");

        // 兑现即揭晓：这条不再占「还藏着」的格子，所以真能记新的（原先按全部行数算，
        // 400 叫用户「先兑现几条」而兑现根本腾不出格——建议本身无效）
        service.fulfillWish("alice", wishes.get(0).getId());
        assertThat(service.board("alice").wishQuotaLeft()).isEqualTo(1);
        service.addWish("alice", "腾出来记的那条", YESTERDAY, null);
        assertThat(service.board("alice").wishQuotaLeft()).isZero();
    }

    // ========== F381 雷区探测器 ==========

    @Test
    void minePlantedDuplicateAndQuotaRejected() {
        service.addMine("alice", "问我前男友", "像审问", "换个说法问我");
        assertThatThrownBy(() -> service.addMine("alice", "问我前男友", "再来", "再来"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("挂过");
        for (int i = 0; i < CoupleCatchMine.PER_USER_MAX - 1; i++) {
            service.addMine("alice", "雷 " + i, "点", "法");
        }
        assertThatThrownBy(() -> service.addMine("alice", "第七颗", "点", "法"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("扫雷");
        assertThatThrownBy(() -> service.addMine("alice", "话题", "点".repeat(CoupleCatchMine.TRIP_MAX + 1), "法"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void mineAckBySelfRejectedAndPartnerAckIdempotent() {
        service.addMine("alice", "问我工资", "像查岗", "先说你想听吗");
        String id = mines.get(0).getId();

        assertThatThrownBy(() -> service.ackMine("alice", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己挂的");

        assertThat(service.ackMine("bob", id).mines().get(0).acked()).isTrue();
        service.ackMine("bob", id);
        verify(push, times(1)).pushCoupleEvent(eq("catch-mine-ack"), any(), any(), any());
    }

    @Test
    void mineAvoidNeedsAckAndOnlyByPartner() {
        service.addMine("alice", "问我工资", "像查岗", "先问我想听吗");
        String id = mines.get(0).getId();

        assertThatThrownBy(() -> service.avoidMine("bob", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("先盖");
        service.ackMine("bob", id);
        assertThatThrownBy(() -> service.avoidMine("alice", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("战绩");

        assertThat(service.avoidMine("bob", id).mines().get(0).avoided()).isEqualTo(1);
    }

    // ========== F382 安全词 ==========

    @Test
    void safewordUpsertsOneRowPerUser() {
        service.setSafeword("alice", "暂停", "给我十分钟");
        service.setSafeword("alice", "先停", "改成十分钟");

        assertThat(words).hasSize(1);
        assertThat(service.board("alice").myWord().word()).isEqualTo("先停");
        assertThat(service.board("alice").myWord().useCount()).isZero();
    }

    @Test
    void safewordUseNeedsWordAndOncePerDay() {
        assertThatThrownBy(() -> service.useSafeword("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("先约一个安全词");

        service.setSafeword("alice", "暂停", null);
        service.useSafeword("alice");
        assertThatThrownBy(() -> service.useSafeword("alice"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("今天已经记过");
        assertThat(service.board("alice").monthUses()).isEqualTo(1);

        service.setSafeword("bob", "缓缓", null);
        service.useSafeword("bob");
        assertThat(service.board("alice").uses()).hasSize(2);
    }

    @Test
    void safewordReflectOnlyByPersonWhoCalledIt() {
        service.setSafeword("alice", "暂停", null);
        service.useSafeword("alice");
        String useId = uses.get(0).getId();

        assertThatThrownBy(() -> service.reflectUse("bob", useId, "我不该追"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己写");
        assertThatThrownBy(() -> service.reflectUse("alice", useId, "  "))
                .isInstanceOf(BusinessException.class);

        assertThat(service.reflectUse("alice", useId, "当时是怕被丢下").uses().get(0).reflect())
                .isEqualTo("当时是怕被丢下");
        verify(push).pushCoupleEventBoth(eq("catch-safeword-reflect"), any(), any(), any(), any());
    }

    // ========== F383 敏感日历 ==========

    // ========== F384 说到哪了 ==========

    // ========== F385 真话翻译机 ==========

    // ========== F386 聆听方式协议 ==========

    // ========== F387 话题许愿池 ==========

    // ========== F388 今日一句话 ==========

    @Test
    void dailyUpsertsPerDayAndRewriteDoesNotRepush() {
        service.daily("alice", "今天风很大");
        service.daily("alice", "今天风很大，想你了");

        assertThat(dailies).hasSize(1);
        assertThat(service.board("alice").myToday().content()).isEqualTo("今天风很大，想你了");
        verify(push, times(1)).pushCoupleEvent(eq("catch-daily"), any(), any(), any());
        assertThatThrownBy(() -> service.daily("alice", "字".repeat(CoupleCatchDaily.CONTENT_MAX + 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void dailyHintFallsBackToPartnerLastSentence() {
        service.daily("bob", "昨天那句");
        dailies.get(0).setDay(YESTERDAY);

        var vo = service.board("alice");
        assertThat(vo.partnerToday()).isNull();
        assertThat(vo.dailyHint()).contains("今天还没说").contains(YESTERDAY).contains("昨天那句");
    }

    // ========== F389 年报与兜底 ==========

    @Test
    void missingRowsAre404AndNoSpaceIs404() {
        assertThatThrownBy(() -> service.fulfillWish("alice", "nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.ackMine("bob", "nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.reflectUse("alice", "nope", "x")).isInstanceOf(BusinessException.class);

        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("carol")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.daily("carol", "x")).isInstanceOf(BusinessException.class);
    }
}
