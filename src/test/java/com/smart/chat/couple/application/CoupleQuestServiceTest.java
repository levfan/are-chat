package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.persistence.CoupleQuestOvertime;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestOvertimeMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpace;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
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
 * 加班预报与留灯（保留卡 `couple-quest-overtime`）单测：每人每天一行 upsert 与小时钳制、
 * 灯卡只有对方能留、同一行在两侧的视角不同、无空间 404。
 * 关卡预告/战报/陪护单/静音舱/搬家/低谷/小胜利/关口预约 随功能下线，对应用例一并删除。
 */
@ExtendWith(MockitoExtension.class)
class CoupleQuestServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleQuestOvertimeMapper overtimeMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleQuestService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<CoupleQuestOvertime> overtimes = new ArrayList<>();

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
    void noSpaceIs404() {
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("carol"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("情侣空间");
        verify(overtimeMapper, never()).insert(any(CoupleQuestOvertime.class));
    }
}
