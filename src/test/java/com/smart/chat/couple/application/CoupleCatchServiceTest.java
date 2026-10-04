package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.persistence.CoupleCatchSafewordPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleCatchSafewordMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleCatchSafewordUsePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleCatchSafewordUseMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpacePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 安全词与暂停复盘（保留卡 `couple-catch-safeword`）单测。
 * 断言锁的是「真的落了一行、真的推给了对的人」，而不是返回值回显。
 */
@ExtendWith(MockitoExtension.class)
class CoupleCatchServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleCatchSafewordMapper safewordMapper;
    @Mock
    private CoupleCatchSafewordUseMapper useMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleCatchService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<CoupleCatchSafewordPO> words = new ArrayList<>();
    private final List<CoupleCatchSafewordUsePO> uses = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpacePO space = new CoupleSpacePO();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpacePO.STATUS_ACTIVE);
        space.setAnniversary(DAY);
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));

        lenient().when(safewordMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(words));
        lenient().when(safewordMapper.find(eq("s1"), any())).thenAnswer(inv -> words.stream()
                .filter(w -> w.getFromUser().equals(inv.getArgument(1))).findFirst().orElse(null));
        lenient().when(safewordMapper.insert(any(CoupleCatchSafewordPO.class))).thenAnswer(inv -> {
            words.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(safewordMapper.updateById(any(CoupleCatchSafewordPO.class))).thenReturn(1);

        lenient().when(useMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(uses));
        lenient().when(useMapper.find(eq("s1"), any(), any())).thenAnswer(inv -> uses.stream()
                .filter(u -> u.getDay().equals(inv.getArgument(1)) && u.getUserName().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        lenient().when(useMapper.insert(any(CoupleCatchSafewordUsePO.class))).thenAnswer(inv -> {
            uses.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(useMapper.updateById(any(CoupleCatchSafewordUsePO.class))).thenReturn(1);
        lenient().when(useMapper.selectById(any())).thenAnswer(inv -> uses.stream()
                .filter(u -> u.getId().equals(inv.getArgument(0, String.class))).findFirst().orElse(null));
    }

    @Test
    void safewordUpsertsOneRowPerUser() {
        service.setSafeword("alice", "暂停", "给我十分钟");
        service.setSafeword("alice", "先停", "改成十分钟");

        assertThat(words).hasSize(1);
        assertThat(service.board("alice").myWord().word()).isEqualTo("先停");
        assertThat(service.board("alice").myWord().useCount()).isZero();
    }

    @Test
    void safewordSetPushesPartnerAndWritesRow() {
        service.setSafeword("alice", "暂停", null);

        ArgumentCaptor<CoupleCatchSafewordPO> captor = ArgumentCaptor.forClass(CoupleCatchSafewordPO.class);
        verify(safewordMapper).insert(captor.capture());
        assertThat(captor.getValue().getSpaceId()).isEqualTo("s1");
        assertThat(captor.getValue().getFromUser()).isEqualTo("alice");
        verify(push).pushCoupleEvent(eq("catch-safeword"), eq("alice"), eq("bob"), any());
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
        assertThat(service.board("alice").usedTodayMine()).isTrue();
        assertThat(service.board("alice").usedTodayPartner()).isFalse();

        service.setSafeword("bob", "缓缓", null);
        service.useSafeword("bob");
        assertThat(service.board("alice").uses()).hasSize(2);
        assertThat(service.board("alice").usedTodayPartner()).isTrue();
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
        verify(push).pushCoupleEventBoth(eq("catch-safeword-reflect"), eq("alice"), eq("alice"), eq("bob"), any());
    }

    @Test
    void reflectIsPersistedNotJustEchoedBack() {
        service.setSafeword("alice", "暂停", null);
        service.useSafeword("alice");
        String useId = uses.get(0).getId();

        service.reflectUse("alice", useId, "下次先说我去倒杯水");

        // 只看返回值回显的话，坏代码（改了内存对象却没 update）也能过
        verify(useMapper).updateById(any(CoupleCatchSafewordUsePO.class));
        assertThat(uses.get(0).getReflect()).isEqualTo("下次先说我去倒杯水");
    }

    @Test
    void missingRowsAre404AndNoSpaceIs404() {
        assertThatThrownBy(() -> service.reflectUse("alice", "nope", "x")).isInstanceOf(BusinessException.class);
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("carol")).isInstanceOf(BusinessException.class);
        verify(useMapper, never()).insert(any(CoupleCatchSafewordUsePO.class));
    }
}
