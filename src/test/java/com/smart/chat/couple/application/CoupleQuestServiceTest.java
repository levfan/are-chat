package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.quest.QuestOvertime;
import com.smart.chat.couple.domain.quest.QuestOvertimeRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
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
 * 加班预报与留灯（保留卡 `couple-quest-overtime`）单测：每人每天一行 upsert 与小时钳制、
 * 灯卡只有对方能留、同一行在两侧的视角不同、无空间 404。
 * <p>
 * Service 改走 {@link QuestOvertimeRepository} 端口后，这里用内存领域列表复刻 upsert/查询语义；
 * 期望值（钳后小时、灯卡文字、留灯人、视角、事件名）与改造前一字未改。insert-vs-update 落到
 * {@code QuestOvertimeRepositoryAdapterTest} 验。
 */
@ExtendWith(MockitoExtension.class)
class CoupleQuestServiceTest {

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private QuestOvertimeRepository overtimeRepository;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleQuestService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<QuestOvertime> overtimes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpace space = CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 0L, null, null, null, null, null, null, null);
        lenient().when(spaceRepository.findActiveByMember("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceRepository.findActiveByMember("bob")).thenReturn(Optional.of(space));

        lenient().when(overtimeRepository.findBySpaceAndUserAndDay(eq("s1"), any(), any())).thenAnswer(inv -> {
            String user = inv.getArgument(1);
            String day = inv.getArgument(2);
            return overtimes.stream().filter(o -> o.fromUser().equals(user) && o.day().equals(day)).findFirst();
        });
        lenient().when(overtimeRepository.findByIdInSpace(eq("s1"), any())).thenAnswer(inv -> {
            String id = inv.getArgument(1, String.class);
            return overtimes.stream().filter(o -> o.id().equals(id)).findFirst();
        });
        lenient().when(overtimeRepository.listByDay(eq("s1"), any())).thenAnswer(inv -> {
            String day = inv.getArgument(1);
            return overtimes.stream().filter(o -> o.day().equals(day)).toList();
        });
        lenient().doAnswer(inv -> {
            QuestOvertime overtime = inv.getArgument(0);
            int idx = -1;
            for (int i = 0; i < overtimes.size(); i++) {
                if (overtimes.get(i).id().equals(overtime.id())) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                overtimes.set(idx, overtime);
            } else {
                overtimes.add(overtime);
            }
            return null;
        }).when(overtimeRepository).save(any());
    }

    @Test
    void overtimeUpsertsOnceADayAndClampsHour() {
        service.overtime("alice", 26, "赶年结");
        assertThat(overtimes).hasSize(1);
        assertThat(overtimes.get(0).untilHour()).isEqualTo(23);

        service.overtime("alice", 2, null);
        assertThat(overtimes).hasSize(1);
        assertThat(overtimes.get(0).untilHour()).isEqualTo(13);
        verify(overtimeRepository, times(2)).save(any());
        verify(push, times(2)).pushCoupleEvent(eq("quest-overtime"), eq("alice"), eq("bob"), any());

        assertThatThrownBy(() -> service.overtime("alice", 20, "说".repeat(41)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 40 字");
    }

    @Test
    void lampOnlyForTheOtherPersonsRow() {
        assertThatThrownBy(() -> service.leaveLamp("bob", "nope", "等你"))
                .isInstanceOf(BusinessException.class);

        service.overtime("alice", 22, "加班");
        QuestOvertime row = overtimes.get(0);
        // 自己给自己留灯不算数
        assertThatThrownBy(() -> service.leaveLamp("alice", row.id(), "我自己留"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("自己留不算");
        assertThat(row.lampBy()).isNotEqualTo("alice");

        service.leaveLamp("bob", row.id(), "灯给你留着");
        assertThat(overtimes.get(0).lamp()).isEqualTo("灯给你留着");
        assertThat(overtimes.get(0).lampBy()).isEqualTo("bob");
        verify(push).pushCoupleEvent(eq("quest-lamp"), eq("bob"), eq("alice"), any());
        // 同一条加班预报，两侧各自视角：记的人看到的是自己那格，对方看到的是 partner 那格
        assertThat(service.board("alice").myOvertime().lampBy()).isEqualTo("bob");
        assertThat(service.board("bob").partnerOvertime().lampBy()).isEqualTo("bob");
        assertThat(service.board("bob").myOvertime()).isNull();
    }

    @Test
    void noSpaceIs404() {
        when(spaceRepository.findActiveByMember("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("carol"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("情侣空间");
        verify(overtimeRepository, never()).save(any());
    }
}
