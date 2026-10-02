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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 人生关卡（F370-F379）单测：关卡预告的日期/类型/重名/在途三场上限；战报只能本人交且一战一报、
 * 盖章只能对方盖且重复盖幂等；加班预报小时钳制与每日一行 upsert、灯卡只有对方能留；
 * 陪护单在途一张、代记只有陪护人且一天每种一次、留言只有陪护人、痊愈只有病人自己宣布；
 * 静音舱出舱日必须未来且在途一舱、加油卡只有舱外的人且每天一张、长信只有对方能标记；
 * 搬家区块归属（认领冲突/纸箱与完成只认认领人）、新家第一晚双点只推一次；
 * 低谷通行证 7-30 天跨度、卡一天一张、回升只能本人宣布；
 * 小胜利每人每天一条且改写不重推、小赢奖只能颁对方且一周一颁；
 * 关口预约 60 天窗口、到场只能对方点、撤单只能挂单人；成就墙真数字；无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleQuestServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleQuestBattleMapper battleMapper;
    @Mock
    private CoupleQuestReportMapper reportMapper;
    @Mock
    private CoupleQuestOvertimeMapper overtimeMapper;
    @Mock
    private CoupleQuestNurseMapper nurseMapper;
    @Mock
    private CoupleQuestCareMarkMapper careMarkMapper;
    @Mock
    private CoupleQuestPodMapper podMapper;
    @Mock
    private CoupleQuestMoveMapper moveMapper;
    @Mock
    private CoupleQuestMoveNightMapper moveNightMapper;
    @Mock
    private CoupleQuestValleyMapper valleyMapper;
    @Mock
    private CoupleQuestWinMapper winMapper;
    @Mock
    private CoupleQuestUpcomingMapper upcomingMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleQuestService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String TOMORROW = LocalDate.now().plusDays(1).toString();
    private static final String NEXT_WEEK = LocalDate.now().plusDays(7).toString();

    private final List<CoupleQuestBattle> battles = new ArrayList<>();
    private final List<CoupleQuestReport> reports = new ArrayList<>();
    private final List<CoupleQuestOvertime> overtimes = new ArrayList<>();
    private final List<CoupleQuestNurse> nurses = new ArrayList<>();
    private final List<CoupleQuestCareMark> marks = new ArrayList<>();
    private final List<CoupleQuestPod> pods = new ArrayList<>();
    private final List<CoupleQuestMove> moves = new ArrayList<>();
    private final List<CoupleQuestMoveNight> nights = new ArrayList<>();
    private final List<CoupleQuestValley> valleys = new ArrayList<>();
    private final List<CoupleQuestWin> wins = new ArrayList<>();
    private final List<CoupleQuestUpcoming> upcomings = new ArrayList<>();

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

        // F370 关卡
        lenient().when(battleMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(battles));
        lenient().when(battleMapper.find(eq("s1"), any(), any(), any())).thenAnswer(inv -> battles.stream()
                .filter(b -> b.getFromUser().equals(inv.getArgument(1)) && b.getDay().equals(inv.getArgument(2))
                        && b.getName().equals(inv.getArgument(3)))
                .findFirst().orElse(null));
        lenient().when(battleMapper.countPrep(eq("s1"), any())).thenAnswer(inv -> battles.stream()
                .filter(b -> b.getFromUser().equals(inv.getArgument(1)) && b.prep()).count());
        lenient().when(battleMapper.selectById(any())).thenAnswer(inv -> battles.stream()
                .filter(b -> b.getId().equals(inv.getArgument(0, String.class))).findFirst().orElse(null));
        lenient().when(battleMapper.deleteById(any(java.io.Serializable.class))).thenAnswer(inv -> {
            battles.removeIf(b -> b.getId().equals(inv.getArgument(0, String.class)));
            return 1;
        });
        stubUpsert(battleMapper, CoupleQuestBattle.class, battles);

        // F371 战报
        lenient().when(reportMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(reports));
        lenient().when(reportMapper.findByBattle(any())).thenAnswer(inv -> reports.stream()
                .filter(r -> r.getBattleId().equals(inv.getArgument(0))).findFirst().orElse(null));
        lenient().when(reportMapper.selectById(any())).thenAnswer(inv -> reports.stream()
                .filter(r -> r.getId().equals(inv.getArgument(0, String.class))).findFirst().orElse(null));
        stubUpsert(reportMapper, CoupleQuestReport.class, reports);

        // F372 加班预报
        lenient().when(overtimeMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(overtimes));
        lenient().when(overtimeMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> overtimes.stream()
                .filter(o -> o.getDay().equals(inv.getArgument(1)) && o.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(overtimeMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> overtimes.stream()
                .filter(o -> o.getDay().equals(inv.getArgument(1))).toList());
        lenient().when(overtimeMapper.selectById(any())).thenAnswer(inv -> overtimes.stream()
                .filter(o -> o.getId().equals(inv.getArgument(0, String.class))).findFirst().orElse(null));
        stubUpsert(overtimeMapper, CoupleQuestOvertime.class, overtimes);

        // F373 陪护单与代记
        lenient().when(nurseMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(nurses));
        lenient().when(nurseMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> nurses.stream()
                .filter(n -> n.getPatientUser().equals(inv.getArgument(1)) && n.getOpenDay().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(nurseMapper.findOpen(eq("s1"), any())).thenAnswer(inv -> nurses.stream()
                .filter(n -> n.getPatientUser().equals(inv.getArgument(1)) && n.open())
                .findFirst().orElse(null));
        lenient().when(nurseMapper.selectById(any())).thenAnswer(inv -> nurses.stream()
                .filter(n -> n.getId().equals(inv.getArgument(0, String.class))).findFirst().orElse(null));
        stubUpsert(nurseMapper, CoupleQuestNurse.class, nurses);
        lenient().when(careMarkMapper.findByNurse(any())).thenAnswer(inv -> marks.stream()
                .filter(m -> m.getNurseId().equals(inv.getArgument(0))).toList());
        lenient().when(careMarkMapper.find(any(), any(), any(), any())).thenAnswer(inv -> marks.stream()
                .filter(m -> m.getNurseId().equals(inv.getArgument(0)) && m.getDay().equals(inv.getArgument(1))
                        && m.getKind().equals(inv.getArgument(2)) && m.getByUser().equals(inv.getArgument(3)))
                .findFirst().orElse(null));
        stubInsert(careMarkMapper, CoupleQuestCareMark.class, marks);

        // F374 静音舱
        lenient().when(podMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(pods));
        lenient().when(podMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> pods.stream()
                .filter(p -> p.getFromUser().equals(inv.getArgument(1)) && p.getStartDay().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(podMapper.findIn(eq("s1"), any())).thenAnswer(inv -> pods.stream()
                .filter(p -> p.getFromUser().equals(inv.getArgument(1)) && p.in())
                .findFirst().orElse(null));
        lenient().when(podMapper.selectById(any())).thenAnswer(inv -> pods.stream()
                .filter(p -> p.getId().equals(inv.getArgument(0, String.class))).findFirst().orElse(null));
        stubUpsert(podMapper, CoupleQuestPod.class, pods);

        // F375 搬家
        lenient().when(moveMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(moves));
        lenient().when(moveMapper.findBySlot(eq("s1"), anyInt())).thenAnswer(inv -> moves.stream()
                .filter(m -> m.getSlot().equals(inv.getArgument(1))).findFirst().orElse(null));
        stubUpsert(moveMapper, CoupleQuestMove.class, moves);
        lenient().when(moveNightMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(nights));
        lenient().when(moveNightMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> nights.stream()
                .filter(n -> n.getDay().equals(inv.getArgument(1))).findFirst().orElse(null));
        stubUpsert(moveNightMapper, CoupleQuestMoveNight.class, nights);

        // F376 低谷
        lenient().when(valleyMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(valleys));
        lenient().when(valleyMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> valleys.stream()
                .filter(v -> v.getFromUser().equals(inv.getArgument(1)) && v.getOpenDay().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(valleyMapper.findLow(eq("s1"), any())).thenAnswer(inv -> valleys.stream()
                .filter(v -> v.getFromUser().equals(inv.getArgument(1)) && v.low())
                .findFirst().orElse(null));
        lenient().when(valleyMapper.selectById(any())).thenAnswer(inv -> valleys.stream()
                .filter(v -> v.getId().equals(inv.getArgument(0, String.class))).findFirst().orElse(null));
        stubUpsert(valleyMapper, CoupleQuestValley.class, valleys);

        // F377 小胜利
        lenient().when(winMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(wins));
        lenient().when(winMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> wins.stream()
                .filter(w -> w.getDay().equals(inv.getArgument(1)) && w.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(winMapper.findByDayRange(eq("s1"), any(), any())).thenAnswer(inv -> wins.stream()
                .filter(w -> w.getDay().compareTo((String) inv.getArgument(1)) >= 0
                        && w.getDay().compareTo((String) inv.getArgument(2)) <= 0)
                .toList());
        lenient().when(winMapper.selectById(any())).thenAnswer(inv -> wins.stream()
                .filter(w -> w.getId().equals(inv.getArgument(0, String.class))).findFirst().orElse(null));
        stubUpsert(winMapper, CoupleQuestWin.class, wins);

        // F379 关口预约
        lenient().when(upcomingMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(upcomings));
        lenient().when(upcomingMapper.find(eq("s1"), any(), any(), any())).thenAnswer(inv -> upcomings.stream()
                .filter(u -> u.getDay().equals(inv.getArgument(1)) && u.getFromUser().equals(inv.getArgument(2))
                        && u.getTitle().equals(inv.getArgument(3)))
                .findFirst().orElse(null));
        lenient().when(upcomingMapper.selectById(any())).thenAnswer(inv -> upcomings.stream()
                .filter(u -> u.getId().equals(inv.getArgument(0, String.class))).findFirst().orElse(null));
        lenient().when(upcomingMapper.deleteById(any(java.io.Serializable.class))).thenAnswer(inv -> {
            upcomings.removeIf(u -> u.getId().equals(inv.getArgument(0, String.class)));
            return 1;
        });
        stubUpsert(upcomingMapper, CoupleQuestUpcoming.class, upcomings);
    }

    /** insert/updateById 通用桩（实体 of() 工厂已写 created，袋里是同一引用，update 就地生效）。 */
    private <T> void stubUpsert(com.smart.chat.im.BaseMapperCompat<T> mapper, Class<T> type, List<T> bag) {
        lenient().when(mapper.insert(any(type))).thenAnswer(inv -> {
            bag.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(mapper.updateById(any(type))).thenReturn(1);
    }

    private <T> void stubInsert(com.smart.chat.im.BaseMapperCompat<T> mapper, Class<T> type, List<T> bag) {
        lenient().when(mapper.insert(any(type))).thenAnswer(inv -> {
            bag.add(inv.getArgument(0));
            return 1;
        });
    }

    /** 建一关并返回它的 id。 */
    private String seedBattle(String me, String day, String name) {
        service.addBattle(me, day, "INTERVIEW", name, "手心全是汗");
        return battles.get(battles.size() - 1).getId();
    }

    // ========== F370 关卡预告 ==========

    @Test
    void battleAnnouncesAndPushesPartner() {
        var vo = service.addBattle("alice", TOMORROW, "REPORT", "季度述职", "怕被追问数据");

        assertThat(vo.battles()).hasSize(1);
        assertThat(vo.battles().get(0).kindLabel()).isEqualTo("汇报");
        assertThat(vo.battles().get(0).mine()).isTrue();
        verify(push).pushCoupleEvent(eq("quest-battle"), eq("alice"), eq("bob"), any());
    }

    @Test
    void battleRejectsPastDayBlankNameAndBadKind() {
        assertThatThrownBy(() -> service.addBattle("alice", LocalDate.now().minusDays(1).toString(),
                "INTERVIEW", "补考", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("过去的日子");
        assertThatThrownBy(() -> service.addBattle("alice", TOMORROW, "INTERVIEW", "  ", null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.addBattle("alice", TOMORROW, "KARAOKE", "唱歌", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("面试");
    }

    @Test
    void battleRejectsDuplicateAndOverThreeInFlight() {
        service.addBattle("alice", TOMORROW, "DEFEND", "毕业答辩", null);
        assertThatThrownBy(() -> service.addBattle("alice", TOMORROW, "DEFEND", "毕业答辩", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("挂过");

        service.addBattle("alice", DAY, "CHECKUP", "体检", null);
        service.addBattle("alice", NEXT_WEEK, "OTHER", "考科三", null);
        assertThatThrownBy(() -> service.addBattle("alice", TOMORROW, "OTHER", "第四场", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("在途");
        // 对方的在途不受我的额度影响
        service.addBattle("bob", TOMORROW, "OTHER", "TA 的面试", null);
    }

    @Test
    void battleRemoveOnlyByOwnerAndNotAfterReport() {
        String id = seedBattle("alice", TOMORROW, "述职");
        assertThatThrownBy(() -> service.removeBattle("bob", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TA 的关卡");

        service.removeBattle("alice", id);
        assertThat(service.board("alice").battles()).isEmpty();
    }

    // ========== F371 出关战报 ==========

    @Test
    void reportOnlyByBattleOwnerAndOnce() {
        String battleId = seedBattle("alice", TOMORROW, "述职");
        assertThatThrownBy(() -> service.report("bob", battleId, "WIN", "还行"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TA 打的");

        service.report("alice", battleId, "SURVIVE", "活着出来了");
        assertThatThrownBy(() -> service.report("alice", battleId, "WIN", "再来一次"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("一关一份");

        var vo = service.board("alice");
        assertThat(vo.battles()).isEmpty();
        assertThat(vo.reports()).hasSize(1);
        assertThat(vo.reports().get(0).resultLabel()).isEqualTo("活着回来了");
        assertThat(vo.reports().get(0).battleName()).isEqualTo("述职");
    }

    @Test
    void reportRejectsBadResultAndLongFeeling() {
        String battleId = seedBattle("alice", TOMORROW, "答辩");
        assertThatThrownBy(() -> service.report("alice", battleId, "MAYBE", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("三种");
        assertThatThrownBy(() -> service.report("alice", battleId, "WIN", "字".repeat(CoupleQuestReport.FEELING_MAX + 1)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void sealSkipsSelfIsIdempotentAndLabelsByResult() {
        String battleId = seedBattle("alice", TOMORROW, "面试");
        service.report("alice", battleId, "WIN", "过了");
        String reportId = reports.get(0).getId();

        assertThatThrownBy(() -> service.seal("alice", reportId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TA 来盖");

        assertThat(service.seal("bob", reportId).reports().get(0).sealed()).isTrue();
        assertThat(service.seal("bob", reportId).reports().get(0).sealLabel()).isEqualTo("🏆 庆功章");
        verify(push, times(1)).pushCoupleEventBoth(eq("quest-seal"), any(), any(), any(), any());
    }

    // ========== F372 加班预报 ==========

    @Test
    void overtimeClampsHourAndUpsertsOneRowPerDay() {
        var vo = service.overtime("alice", 99, "赶版本");
        assertThat(vo.myOvertime().untilHour()).isEqualTo(CoupleQuestOvertime.HOUR_MAX);

        service.overtime("alice", 1, "算了不加班");
        assertThat(overtimes).hasSize(1);
        assertThat(service.board("alice").myOvertime().untilHour()).isEqualTo(CoupleQuestOvertime.HOUR_MIN);
    }

    @Test
    void lampOnlyByPartnerAndRequiresForecast() {
        assertThatThrownBy(() -> service.leaveLamp("bob", "nope", "灯给你留着"))
                .isInstanceOf(BusinessException.class);

        service.overtime("alice", 22, "赶版本");
        String id = overtimes.get(0).getId();
        assertThatThrownBy(() -> service.leaveLamp("alice", id, "我自己留灯"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己留");

        var vo = service.leaveLamp("bob", id, "汤在锅里，灯在门口");
        assertThat(vo.partnerOvertime().lamp()).isEqualTo("汤在锅里，灯在门口");
        verify(push).pushCoupleEvent(eq("quest-lamp"), eq("bob"), eq("alice"), any());
    }

    // ========== F373 生病陪护单 ==========

    @Test
    void nurseOpenOnlyOneInFlightAndCarerIsOpener() {
        service.openNurse("alice", "发烧 38 度");
        assertThat(service.board("alice").partnerNurse().carerUser()).isEqualTo("alice");
        assertThat(service.board("bob").myNurse()).isNotNull();

        assertThatThrownBy(() -> service.openNurse("alice", "又开一张"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还在途");
    }

    @Test
    void careMarkOnlyCarerOncePerKindPerDay() {
        service.openNurse("alice", "拉肚子");
        String nurseId = nurses.get(0).getId();

        assertThatThrownBy(() -> service.careMark("bob", nurseId, "WATER"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("代记轮不到");
        assertThatThrownBy(() -> service.careMark("alice", nurseId, "JUICE"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("喝水或吃药");

        var vo = service.careMark("alice", nurseId, "WATER");
        assertThat(vo.partnerNurse().waterCount()).isEqualTo(1);
        assertThatThrownBy(() -> service.careMark("alice", nurseId, "WATER"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经记过");

        service.careMark("alice", nurseId, "MED");
        assertThat(service.board("bob").myNurse().medCount()).isEqualTo(1);
    }

    @Test
    void nurseMessageOnlyByCarerAndCloseOnlyByPatient() {
        service.openNurse("alice", "偏头痛");
        String nurseId = nurses.get(0).getId();

        assertThatThrownBy(() -> service.nurseMessage("bob", nurseId, "多喝热水"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("陪护人写的");
        assertThat(service.nurseMessage("alice", nurseId, "药在床头").partnerNurse().message())
                .isEqualTo("药在床头");

        assertThatThrownBy(() -> service.closeNurse("alice", nurseId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能替 TA");

        var vo = service.closeNurse("bob", nurseId);
        assertThat(vo.myNurse()).isNull();
        assertThat(vo.nurses().get(0).open()).isFalse();
        verify(push).pushCoupleEventBoth(eq("quest-nurse-close"), eq("bob"), eq("alice"), eq("bob"), any());
    }

    // ========== F374 静音舱 ==========

    @Test
    void podRejectsPastUntilDayAndKeepsOneInFlight() {
        assertThatThrownBy(() -> service.enterPod("alice", DAY))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("晚于今天");

        service.enterPod("alice", NEXT_WEEK);
        assertThat(service.board("alice").myPod().in()).isTrue();
        assertThatThrownBy(() -> service.enterPod("alice", NEXT_WEEK))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("你还在舱里");
    }

    @Test
    void podCheerOnlyFromOutsideOncePerDay() {
        service.enterPod("alice", NEXT_WEEK);
        String podId = pods.get(0).getId();

        assertThatThrownBy(() -> service.cheerPod("alice", podId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("从外面递");

        assertThat(service.cheerPod("bob", podId).partnerPod().cheerCount()).isEqualTo(1);
        assertThatThrownBy(() -> service.cheerPod("bob", podId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("一天一张");
    }

    @Test
    void podOutByOwnerThenLetterByPartner() {
        service.enterPod("alice", NEXT_WEEK);
        String podId = pods.get(0).getId();

        assertThatThrownBy(() -> service.leavePod("bob", podId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己出舱");
        assertThatThrownBy(() -> service.podLetterDone("bob", podId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还在舱里");

        service.leavePod("alice", podId);
        assertThatThrownBy(() -> service.podLetterDone("alice", podId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("对面那个人写");

        var vo = service.podLetterDone("bob", podId);
        assertThat(vo.partnerPod().letterDone()).isTrue();
        verify(push, times(1)).pushCoupleEventBoth(eq("quest-pod-letter"), any(), any(), any(), any());
        service.podLetterDone("bob", podId);
        verify(push, times(1)).pushCoupleEventBoth(eq("quest-pod-letter"), any(), any(), any(), any());
    }

    // ========== F375 搬家互助 ==========

    @Test
    void moveClaimRejectsTakersAndBoxesNeedOwner() {
        assertThatThrownBy(() -> service.moveName("alice", 0, "厨房"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("区块位");
        assertThatThrownBy(() -> service.moveName("alice", 1, "字".repeat(CoupleQuestMove.NAME_MAX + 1)))
                .isInstanceOf(BusinessException.class);

        service.moveName("alice", 1, "厨房");
        assertThat(service.board("alice").moves().get(0).owner()).isEqualTo("alice");

        assertThatThrownBy(() -> service.moveClaim("bob", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已经认领");
        assertThatThrownBy(() -> service.moveBoxes("bob", 1, 5))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还没归你");
        assertThatThrownBy(() -> service.moveDone("bob", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("谁认领的谁");

        var vo = service.moveBoxes("alice", 1, 999);
        assertThat(vo.moves().get(0).boxes()).isEqualTo(CoupleQuestMove.BOX_MAX);
        assertThat(vo.moveBoxes()).isEqualTo(CoupleQuestMove.BOX_MAX);

        service.moveDone("alice", 1);
        verify(push).pushCoupleEventBoth(eq("quest-move-done"), any(), any(), any(), any());
        service.moveDone("alice", 1);
        verify(push, times(1)).pushCoupleEventBoth(eq("quest-move-done"), any(), any(), any(), any());
    }

    @Test
    void moveClaimCanBeReleasedByOwner() {
        service.moveName("bob", 3, "书房");
        assertThat(service.board("bob").moves().get(0).claimed()).isTrue();

        var vo = service.moveClaim("bob", 3);
        assertThat(vo.moves().get(0).claimed()).isFalse();
        verify(push).pushCoupleEvent(eq("quest-move-unclaim"), eq("bob"), eq("alice"), any());
    }

    @Test
    void moveNightBothTickedPushesOnce() {
        assertThat(service.moveNight("alice", TOMORROW, "第一晚吃火锅").moveNight().bothTicked()).isFalse();

        var vo = service.moveNight("bob", TOMORROW, null);
        assertThat(vo.moveNight().bothTicked()).isTrue();
        assertThat(vo.moveNight().note()).isEqualTo("第一晚吃火锅");
        verify(push, times(1)).pushCoupleEventBoth(eq("quest-move-night"), any(), any(), any(), any());

        service.moveNight("bob", TOMORROW, null);
        verify(push, times(1)).pushCoupleEventBoth(eq("quest-move-night"), any(), any(), any(), any());

        // 点过之后再补一句话必须真的落库：原先只有 0→1 翻转才 update，补的话只改了内存对象就被丢弃
        // （界面给成功提示、重进页面什么都没有）。所以锁 update 次数，而不是锁 VO 回显——mock 里对象是同一个实例，
        // 只断言回显的话坏代码也能过。推送仍然只在翻转那一次发。
        var late = service.moveNight("bob", TOMORROW, "补一句：半夜下楼跑了个步");
        assertThat(late.moveNight().note()).isEqualTo("补一句：半夜下楼跑了个步");
        verify(moveNightMapper, times(2)).updateById(any(CoupleQuestMoveNight.class));
        verify(push, times(1)).pushCoupleEventBoth(eq("quest-move-night"), any(), any(), any(), any());
    }

    // ========== F376 低谷通行证 ==========

    @Test
    void valleyChecksSpanAndAllowsOnlyOneInFlight() {
        assertThatThrownBy(() -> service.openValley("alice", LocalDate.now().plusDays(3).toString()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("通行证要挂");
        assertThatThrownBy(() -> service.openValley("alice", LocalDate.now().plusDays(60).toString()))
                .isInstanceOf(BusinessException.class);

        service.openValley("alice", LocalDate.now().plusDays(10).toString());
        assertThatThrownBy(() -> service.openValley("alice", LocalDate.now().plusDays(12).toString()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还在有效期内");
    }

    @Test
    void valleyCareFromOutsideOncePerDayAndRiseByOwner() {
        service.openValley("alice", LocalDate.now().plusDays(9).toString());
        String valleyId = valleys.get(0).getId();

        assertThatThrownBy(() -> service.valleyCare("alice", valleyId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("从外面递");
        assertThat(service.valleyCare("bob", valleyId).partnerValley().careCount()).isEqualTo(1);
        assertThatThrownBy(() -> service.valleyCare("bob", valleyId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("一天一张");

        assertThatThrownBy(() -> service.valleyRise("bob", valleyId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己说了算");

        var vo = service.valleyRise("alice", valleyId);
        assertThat(vo.myValley()).isNull();
        verify(push).pushCoupleEventBoth(eq("quest-valley-up"), eq("alice"), eq("alice"), eq("bob"), any());
    }

    // ========== F377 小胜利账本 ==========

    @Test
    void winUpsertsPerDayAndRewriteDoesNotRepush() {
        service.addWin("alice", "把简历改了", null);
        service.addWin("alice", "把简历改完了", null);

        assertThat(wins).hasSize(1);
        assertThat(service.board("alice").wins().get(0).content()).isEqualTo("把简历改完了");
        verify(push, times(1)).pushCoupleEvent(eq("quest-win"), any(), any(), any());

        assertThatThrownBy(() -> service.addWin("alice", "字".repeat(CoupleQuestWin.CONTENT_MAX + 1), null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.addWin("alice", "明天的事", LocalDate.now().plusDays(2).toString()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("预支");
    }

    @Test
    void winAwardOnlyForPartnerAndOncePerWeek() {
        service.addWin("alice", "改了简历", null);
        String winId = wins.get(0).getId();

        assertThatThrownBy(() -> service.awardWin("alice", winId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能自颁");

        assertThat(service.awardWin("bob", winId).wins().get(0).awarded()).isTrue();
        service.addWin("alice", "洗了外套", LocalDate.now().minusDays(1).toString());
        assertThatThrownBy(() -> service.awardWin("bob", wins.get(1).getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("这周你已经颁过");
        verify(push).pushCoupleEventBoth(eq("quest-win-award"), any(), any(), any(), any());
    }

    // ========== F379 下次关卡预约 ==========

    @Test
    void upcomingChecksWindowAttendsAndRemovesByOwner() {
        assertThatThrownBy(() -> service.addUpcoming("alice", LocalDate.now().minusDays(1).toString(), "昨天的面试"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("60 天");
        assertThatThrownBy(() -> service.addUpcoming("alice", LocalDate.now().plusDays(90).toString(), "明年"))
                .isInstanceOf(BusinessException.class);

        service.addUpcoming("alice", NEXT_WEEK, "答辩");
        assertThatThrownBy(() -> service.addUpcoming("alice", NEXT_WEEK, "答辩"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("挂过");

        String id = upcomings.get(0).getId();
        assertThatThrownBy(() -> service.attendUpcoming("alice", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己给自己应援");
        assertThat(service.attendUpcoming("bob", id).upcoming().get(0).attended()).isTrue();
        service.attendUpcoming("bob", id);
        verify(push, times(1)).pushCoupleEvent(eq("quest-attend"), any(), any(), any());

        assertThatThrownBy(() -> service.removeUpcoming("bob", id))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TA 挂的");
        assertThat(service.removeUpcoming("alice", id).upcoming()).isEmpty();
    }

    // ========== F378 成就墙与兜底 ==========

    @Test
    void wallCountsRealNumbers() {
        String battleId = seedBattle("alice", DAY, "述职");
        service.report("alice", battleId, "WIN", "过了");
        service.seal("bob", reports.get(0).getId());
        service.openNurse("alice", "发烧");
        service.enterPod("bob", NEXT_WEEK);
        service.addWin("alice", "改了简历", null);
        service.addUpcoming("alice", NEXT_WEEK, "答辩");
        service.attendUpcoming("bob", upcomings.get(0).getId());

        var wall = service.board("alice").wall();
        assertThat(wall.battles()).isEqualTo(1);
        assertThat(wall.reports()).isEqualTo(1);
        assertThat(wall.winRate()).isEqualTo(100);
        assertThat(wall.attends()).isEqualTo(1);
        assertThat(wall.summary()).contains("人生关卡墙");

        assertThatThrownBy(() -> service.wall("alice", "20"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("yyyy");
    }

    @Test
    void everyEntryWithoutSpaceIs404() {
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.board("carol")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.addBattle("carol", TOMORROW, "OTHER", "x", null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.addWin("carol", "x", null)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.wall("carol", null)).isInstanceOf(BusinessException.class);
    }

    @Test
    void missingRowsAre404NotSilent() {
        assertThatThrownBy(() -> service.seal("bob", "nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.careMark("alice", "nope", "WATER")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.cheerPod("bob", "nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.attendUpcoming("bob", "nope")).isInstanceOf(BusinessException.class);
        verify(push, never()).pushCoupleEvent(eq("quest-attend"), any(), any(), any());
    }
}
