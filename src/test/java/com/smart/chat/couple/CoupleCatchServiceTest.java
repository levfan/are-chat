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
    private CoupleCatchSensitiveMapper sensitiveMapper;
    @Mock
    private CoupleCatchThreadMapper threadMapper;
    @Mock
    private CoupleCatchSayMapper sayMapper;
    @Mock
    private CoupleCatchProtocolMapper protocolMapper;
    @Mock
    private CoupleCatchTopicMapper topicMapper;
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
    private final List<CoupleCatchSensitive> sensitives = new ArrayList<>();
    private final List<CoupleCatchThread> threads = new ArrayList<>();
    private final List<CoupleCatchSay> says = new ArrayList<>();
    private final List<CoupleCatchProtocol> protocols = new ArrayList<>();
    private final List<CoupleCatchTopic> topics = new ArrayList<>();
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

        lenient().when(sensitiveMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(sensitives));
        lenient().when(sensitiveMapper.find(eq("s1"), any(), any(), any())).thenAnswer(inv -> sensitives.stream()
                .filter(s -> s.getOwnerUser().equals(inv.getArgument(1)) && s.getDay().equals(inv.getArgument(2))
                        && s.getKind().equals(inv.getArgument(3)))
                .findFirst().orElse(null));
        lenient().when(sensitiveMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> sensitives.stream()
                .filter(s -> s.getDay().equals(inv.getArgument(1))).toList());
        stubSelectByIdAndUpsert(sensitiveMapper, CoupleCatchSensitive.class, sensitives, CoupleCatchSensitive::getId);

        lenient().when(threadMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(threads));
        lenient().when(threadMapper.findByUserStatus(eq("s1"), any(), any())).thenAnswer(inv -> threads.stream()
                .filter(t -> t.getFromUser().equals(inv.getArgument(1)) && t.getStatus().equals(inv.getArgument(2)))
                .toList());
        stubSelectByIdAndUpsert(threadMapper, CoupleCatchThread.class, threads, CoupleCatchThread::getId);

        lenient().when(sayMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(says));
        lenient().when(sayMapper.findByUser(eq("s1"), any())).thenAnswer(inv -> says.stream()
                .filter(s -> s.getFromUser().equals(inv.getArgument(1))).toList());
        lenient().when(sayMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> says.stream()
                .filter(s -> s.getFromUser().equals(inv.getArgument(1)) && s.getSay().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        stubSelectByIdAndUpsert(sayMapper, CoupleCatchSay.class, says, CoupleCatchSay::getId);

        lenient().when(protocolMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(protocols));
        lenient().when(protocolMapper.find(eq("s1"), any())).thenAnswer(inv -> protocols.stream()
                .filter(p -> p.getFromUser().equals(inv.getArgument(1))).findFirst().orElse(null));
        stubInsertAndUpdate(protocolMapper, CoupleCatchProtocol.class, protocols);

        lenient().when(topicMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(topics));
        lenient().when(topicMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> topics.stream()
                .filter(t -> t.getFromUser().equals(inv.getArgument(1)) && t.getTitle().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(topicMapper.findByStatus(eq("s1"), any())).thenAnswer(inv -> topics.stream()
                .filter(t -> t.getStatus().equals(inv.getArgument(1))).toList());
        stubSelectByIdAndUpsert(topicMapper, CoupleCatchTopic.class, topics, CoupleCatchTopic::getId);

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

    @Test
    void sensitiveRejectsPastDayBadKindAndDuplicate() {
        assertThatThrownBy(() -> service.addSensitive("alice", YESTERDAY, "PERIOD", "别问我"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("提前标");
        assertThatThrownBy(() -> service.addSensitive("alice", TOMORROW, "TAX_DAY", "别问我"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("周期");

        service.addSensitive("alice", TOMORROW, "CHECK", "帮我留灯");
        assertThatThrownBy(() -> service.addSensitive("alice", TOMORROW, "CHECK", "又标一次"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标过");

        var vo = service.board("bob");
        assertThat(vo.sensitives()).hasSize(1);
        assertThat(vo.sensitives().get(0).mineAsOwner()).isTrue();
        assertThat(vo.sensitives().get(0).remindTomorrow()).isTrue();
    }

    @Test
    void sensitiveRemoveBlockedForOwner() {
        service.addSensitive("alice", TOMORROW, "MEMORY", "安静陪着");
        String id = sensitives.get(0).getId();

        assertThatThrownBy(() -> service.removeSensitive("bob", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TA 替你标的");
        assertThat(service.removeSensitive("alice", id).sensitives()).isEmpty();
    }

    // ========== F384 说到哪了 ==========

    @Test
    void threadQuotaFiveInFlightAndDuplicateTopic() {
        for (int i = 0; i < CoupleCatchThread.IN_FLIGHT_MAX; i++) {
            service.addThread("alice", "话头 " + i, "说到第 " + i + " 句");
        }
        assertThatThrownBy(() -> service.addThread("alice", "第六个", "没说完"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("在途");
        assertThatThrownBy(() -> service.addThread("alice", "话头 1", "重复"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("线轴");
    }

    @Test
    void threadFinishOnlyByOwnerAndIdempotent() {
        service.addThread("alice", "要不要换工作", "说到第三步");
        String id = threads.get(0).getId();

        assertThatThrownBy(() -> service.finishThread("bob", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TA 存的");

        assertThat(service.finishThread("alice", id).myThreads()).isEmpty();
        service.finishThread("alice", id);
        verify(push, times(1)).pushCoupleEventBoth(eq("catch-thread-done"), any(), any(), any(), any());
    }

    // ========== F385 真话翻译机 ==========

    @Test
    void sayRequiresMeansAndQuotaTen() {
        assertThatThrownBy(() -> service.addSay("alice", "我没事", "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("翻译结果");
        assertThatThrownBy(() -> service.addSay("alice", "句子".repeat(11), "其实有事"))
                .isInstanceOf(BusinessException.class);

        service.addSay("alice", "我没事", "其实想被抱一下");
        assertThatThrownBy(() -> service.addSay("alice", "我没事", "换个说法"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("申报过");
        for (int i = 0; i < CoupleCatchSay.PER_USER_MAX - 1; i++) {
            service.addSay("alice", "词条 " + i, "意思 " + i);
        }
        assertThatThrownBy(() -> service.addSay("alice", "第十一条", "意思"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void sayCannotBeDeletedByPartner() {
        service.addSay("alice", "我没事", "其实想被抱一下");
        String id = says.get(0).getId();

        assertThatThrownBy(() -> service.removeSay("bob", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能改也不能删");
        assertThat(service.board("bob").partnerSays()).hasSize(1);
        assertThat(service.removeSay("alice", id).mySays()).isEmpty();
    }

    // ========== F386 聆听方式协议 ==========

    @Test
    void protocolRejectsUnknownModeAndHintsWhenIncomplete() {
        assertThatThrownBy(() -> service.setProtocol("alice", "SILENCE_TREAT", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("五种里选一个");

        service.setProtocol("alice", "HUG", "抱抱就好");
        assertThat(service.board("alice").protocolHint()).contains("还差TA");
        assertThat(service.board("alice").myProtocol().modeLabel()).isEqualTo("抱抱，别说话");
        assertThat(service.board("bob").partnerProtocol().modeLabel()).isEqualTo("抱抱，别说话");
    }

    @Test
    void protocolClashHintWhenModesDiffer() {
        service.setProtocol("alice", "HUG", null);
        service.setProtocol("bob", "REASON", null);

        assertThat(service.board("alice").protocolHint()).contains("不一样").contains("跟我讲道理");

        service.setProtocol("bob", "HUG", null);
        assertThat(service.board("alice").protocolHint()).contains("两份说明书都交了");
    }

    // ========== F387 话题许愿池 ==========

    @Test
    void topicTakeBySelfRejectedAndTalkNeedsTakerAndReflect() {
        service.addTopic("alice", "以后想在哪座城市");
        String id = topics.get(0).getId();

        assertThatThrownBy(() -> service.addTopic("alice", "以后想在哪座城市"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("许过");
        assertThatThrownBy(() -> service.takeTopic("alice", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能自己接");
        assertThatThrownBy(() -> service.talkTopic("alice", id, "聊得很好"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还没接单");

        service.takeTopic("bob", id);
        assertThatThrownBy(() -> service.talkTopic("alice", id, "轮不到你"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("谁来说");
        assertThatThrownBy(() -> service.talkTopic("bob", id, "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("感想");

        var vo = service.talkTopic("bob", id, "原来TA是这么想的");
        assertThat(vo.topics().get(0).status()).isEqualTo("TALKED");
        assertThat(vo.topics().get(0).overdue()).isFalse();
    }

    @Test
    void topicTakeTwiceIsIdempotent() {
        service.addTopic("alice", "婚礼怎么办");
        String id = topics.get(0).getId();

        service.takeTopic("bob", id);
        service.takeTopic("bob", id);
        verify(push, times(1)).pushCoupleEvent(eq("catch-topic-take"), any(), any(), any());
    }

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
    void yearReportCountsRealNumbers() {
        service.addWish("alice", "键盘", YESTERDAY, null);
        service.fulfillWish("alice", wishes.get(0).getId());
        service.addMine("alice", "问工资", "查岗", "先问");
        service.ackMine("bob", mines.get(0).getId());
        service.avoidMine("bob", mines.get(0).getId());
        service.setSafeword("alice", "暂停", null);
        service.useSafeword("alice");
        service.reflectUse("alice", uses.get(0).getId(), "怕被丢下");
        service.addThread("alice", "换工作", "说到一半");
        service.finishThread("alice", threads.get(0).getId());
        service.addSay("alice", "我没事", "想被抱");
        service.addTopic("alice", "城市");
        service.takeTopic("bob", topics.get(0).getId());
        service.talkTopic("bob", topics.get(0).getId(), "聊完了");
        service.daily("alice", "今天想你");

        var year = service.board("alice").year();
        assertThat(year.wishes()).isEqualTo(1);
        assertThat(year.fulfilled()).isEqualTo(1);
        assertThat(year.mines()).isEqualTo(1);
        assertThat(year.acked()).isEqualTo(1);
        assertThat(year.avoids()).isEqualTo(1);
        assertThat(year.uses()).isEqualTo(1);
        assertThat(year.reflected()).isEqualTo(1);
        assertThat(year.threads()).isEqualTo(1);
        assertThat(year.finished()).isEqualTo(1);
        assertThat(year.says()).isEqualTo(1);
        assertThat(year.talked()).isEqualTo(1);
        assertThat(year.onTime()).isEqualTo(1);
        assertThat(year.dailies()).isEqualTo(1);
        assertThat(year.summary()).contains("聆听者年报");

        assertThatThrownBy(() -> service.yearReport("alice", "26"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("yyyy");
    }

    @Test
    void missingRowsAre404AndNoSpaceIs404() {
        assertThatThrownBy(() -> service.fulfillWish("alice", "nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.ackMine("bob", "nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.reflectUse("alice", "nope", "x")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.removeSensitive("alice", "nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.finishThread("alice", "nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.removeSay("alice", "nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.takeTopic("bob", "nope")).isInstanceOf(BusinessException.class);

        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("carol")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.daily("carol", "x")).isInstanceOf(BusinessException.class);
    }
}
