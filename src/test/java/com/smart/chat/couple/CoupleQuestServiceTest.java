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
import java.util.Locale;
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
 * 人生关卡单测（系统裁剪后只留加班预报与留灯、生病陪护单）：加班预报每人每天一行 upsert 与小时钳制、
 * 灯卡只有对方能留、陪护单只能为 TA 开且病人自己才算痊愈、代记只归陪护人且一天每种一次、
 * 关单带陪护天数与喝水吃药计数推双方、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleQuestServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleQuestOvertimeMapper overtimeMapper;
    @Mock
    private CoupleQuestNurseMapper nurseMapper;
    @Mock
    private CoupleQuestCareMarkMapper careMarkMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleQuestService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<CoupleQuestOvertime> overtimes = new ArrayList<>();
    private final List<CoupleQuestNurse> nurses = new ArrayList<>();
    private final List<CoupleQuestCareMark> marks = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        lenient().when(overtimeMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> overtimes.stream()
                .filter(o -> o.getDay().equals(inv.getArgument(1)) && o.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(overtimeMapper.findByDay(eq("s1"), any())).thenAnswer(inv -> overtimes.stream()
                .filter(o -> o.getDay().equals(inv.getArgument(1))).toList());
        lenient().when(overtimeMapper.insert(any(CoupleQuestOvertime.class))).thenAnswer(inv -> {
            overtimes.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(overtimeMapper.selectById(any())).thenAnswer(inv -> overtimes.stream()
                .filter(o -> o.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        lenient().when(overtimeMapper.updateById(any(CoupleQuestOvertime.class))).thenAnswer(inv -> 1);

        lenient().when(nurseMapper.findOpen(eq("s1"), any())).thenAnswer(inv -> nurses.stream()
                .filter(n -> n.getPatientUser().equals(inv.getArgument(1)) && n.open())
                .findFirst().orElse(null));
        lenient().when(nurseMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(nurses));
        lenient().when(nurseMapper.insert(any(CoupleQuestNurse.class))).thenAnswer(inv -> {
            nurses.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(nurseMapper.selectById(any())).thenAnswer(inv -> nurses.stream()
                .filter(n -> n.getId().equals(inv.getArgument(0))).findFirst().orElse(null));
        lenient().when(nurseMapper.updateById(any(CoupleQuestNurse.class))).thenAnswer(inv -> 1);

        lenient().when(careMarkMapper.find(any(), any(), any(), any())).thenAnswer(inv -> marks.stream()
                .filter(m -> m.getNurseId().equals(inv.getArgument(0)) && m.getDay().equals(inv.getArgument(1))
                        && m.getKind().equals(inv.getArgument(2)) && m.getByUser().equals(inv.getArgument(3)))
                .findFirst().orElse(null));
        lenient().when(careMarkMapper.findByNurse(any())).thenAnswer(inv -> marks.stream()
                .filter(m -> m.getNurseId().equals(inv.getArgument(0))).toList());
        lenient().when(careMarkMapper.insert(any(CoupleQuestCareMark.class))).thenAnswer(inv -> {
            marks.add(inv.getArgument(0));
            return 1;
        });
    }

    @Test
    void overtimeUpsertsOnceADayAndClampsHour() {
        service.overtime("alice", 26, "赶年结");
        assertThat(overtimes).hasSize(1);
        assertThat(overtimes.get(0).getUntilHour()).isEqualTo(23);

        service.overtime("alice", 2, null);
        assertThat(overtimes).hasSize(1);
        assertThat(overtimes.get(0).getUntilHour()).isEqualTo(13);
        verify(overtimeMapper, times(1)).insert(any(CoupleQuestOvertime.class));
        verify(overtimeMapper).updateById(any(CoupleQuestOvertime.class));
        verify(push, times(2)).pushCoupleEvent(eq("quest-overtime"), eq("alice"), eq("bob"), any());

        assertThatThrownBy(() -> service.overtime("alice", 20, "说".repeat(41)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 40 字");
    }

    @Test
    void lampOnlyForTheOtherPersonsRow() {
        assertThatThrownBy(() -> service.leaveLamp("bob", "nope", "等你"))
                .isInstanceOf(BusinessException.class);

        service.overtime("alice", 22, "加班");
        CoupleQuestOvertime row = overtimes.get(0);
        // 自己给自己留灯不算数
        assertThatThrownBy(() -> service.leaveLamp("alice", row.getId(), "我自己留"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己留不算");
        assertThat(row.getLampBy()).isNotEqualTo("alice");

        service.leaveLamp("bob", row.getId(), "灯给你留着");
        assertThat(overtimes.get(0).getLamp()).isEqualTo("灯给你留着");
        assertThat(overtimes.get(0).getLampBy()).isEqualTo("bob");
        verify(push).pushCoupleEvent(eq("quest-lamp"), eq("bob"), eq("alice"), any());
        // 同一条加班预报，两侧各自视角：记的人看到的是自己那格，对方看到的是 partner 那格
        assertThat(service.board("alice").myOvertime().lampBy()).isEqualTo("bob");
        assertThat(service.board("bob").partnerOvertime().lampBy()).isEqualTo("bob");
        assertThat(service.board("bob").myOvertime()).isNull();
    }

    @Test
    void nurseOpenedForPartnerOnlyAndClosedByPatient() {
        service.openNurse("alice", "发烧 38.5");
        assertThat(nurses).hasSize(1);
        // 生病的人自己顾不上记：开单的是陪护人，病人恒为对方
        assertThat(nurses.get(0).getPatientUser()).isEqualTo("bob");
        assertThat(nurses.get(0).getCarerUser()).isEqualTo("alice");
        assertThatThrownBy(() -> service.openNurse("alice", "又开了一张"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("还在途");

        CoupleQuestNurse nurse = nurses.get(0);
        // 痊愈只能病人自己说
        assertThatThrownBy(() -> service.closeNurse("alice", nurse.getId()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("病人自己说");

        service.careMark("alice", nurse.getId(), "WATER");
        assertThat(marks).hasSize(1);
        assertThatThrownBy(() -> service.careMark("alice", nurse.getId(), "water"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经记过");
        // 代记只归陪护人
        assertThatThrownBy(() -> service.careMark("bob", nurse.getId(), "MED"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("轮不到别人");
        assertThatThrownBy(() -> service.careMark("alice", nurse.getId(), "HUG"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("只能记喝水或吃药");

        service.careMark("alice", nurse.getId(), "MED");
        // 一天每种只记一次
        assertThatThrownBy(() -> service.careMark("alice", nurse.getId(), "MED"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经记过");
        assertThat(marks).hasSize(2);

        service.closeNurse("bob", nurse.getId());
        assertThat(nurse.open()).isFalse();
        verify(push).pushCoupleEventBoth(eq("quest-nurse-close"), eq("bob"), eq("alice"), eq("bob"), any());
        // 关单后再来记，直接挡
        assertThatThrownBy(() -> service.careMark("alice", nurse.getId(), "WATER"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("已经关了");
    }

    @Test
    void boardReportsBothSidesNurseView() {
        service.openNurse("alice", "拉肚子");
        CoupleQuestNurse nurse = nurses.get(0);
        service.careMark("alice", nurse.getId(), "WATER");

        CoupleQuestService.QuestVO asPatient = service.board("bob");
        assertThat(asPatient.myNurse()).isNotNull();
        assertThat(asPatient.myNurse().mineAsCarer()).isFalse();
        assertThat(asPatient.myNurse().waterCount()).isEqualTo(1);
        assertThat(asPatient.myNurse().marks()).hasSize(1);

        CoupleQuestService.QuestVO asCarer = service.board("alice");
        assertThat(asCarer.partnerNurse()).isNotNull();
        assertThat(asCarer.myNurse()).isNull();
        assertThat(asCarer.day()).isEqualTo(DAY);
    }

    @Test
    void noSpaceIs404() {
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("carol"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("情侣空间");
        verify(overtimeMapper, never()).insert(any(CoupleQuestOvertime.class));
    }
}
