package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.safeword.Safeword;
import com.smart.chat.couple.domain.safeword.SafewordRepository;
import com.smart.chat.couple.domain.safeword.SafewordUse;
import com.smart.chat.couple.domain.safeword.SafewordUseRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
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
import java.util.UUID;

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
 * 安全词与暂停复盘（保留卡 `couple-catch-safeword`）单测。
 * 断言锁的是「真的落了一行、真的推给了对的人」，而不是返回值回显。
 * <p>
 * 战术改造后 Service 只依赖仓储端口，所以这里把 mock 从 Mapper 换成端口，
 * 用内存里的领域对象复刻 upsert 语义；期望值（数据、事件名、文案）与改造前一字未改。
 */
@ExtendWith(MockitoExtension.class)
class CoupleCatchServiceTest {

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private SafewordRepository safewordRepository;
    @Mock
    private SafewordUseRepository useRepository;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleCatchService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<Safeword> words = new ArrayList<>();
    private final List<SafewordUse> uses = new ArrayList<>();

    @BeforeEach
    void setUp() {
        CoupleSpace space = CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 0L, DAY, null, null, null, null, null, null);
        lenient().when(spaceRepository.findActiveByMember("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceRepository.findActiveByMember("bob")).thenReturn(Optional.of(space));

        lenient().when(safewordRepository.listBySpace("s1")).thenAnswer(inv -> List.copyOf(words));
        lenient().when(safewordRepository.findBySpaceAndUser(eq("s1"), any())).thenAnswer(inv -> words.stream()
                .filter(w -> w.fromUser().equals(inv.getArgument(1))).findFirst());
        lenient().doAnswer(inv -> {
            String fromUser = inv.getArgument(1);
            Safeword agreed = inv.getArgument(2);
            int idx = -1;
            for (int i = 0; i < words.size(); i++) {
                if (words.get(i).fromUser().equals(fromUser)) {
                    idx = i;
                    break;
                }
            }
            Safeword stored = Safeword.restore(idx >= 0 ? words.get(idx).id() : UUID.randomUUID().toString(),
                    "s1", fromUser, agreed.word(), agreed.note());
            if (idx >= 0) {
                words.set(idx, stored);
            } else {
                words.add(stored);
            }
            return null;
        }).when(safewordRepository).save(eq("s1"), any(), any());

        lenient().when(useRepository.listBySpace("s1")).thenAnswer(inv -> List.copyOf(uses));
        lenient().when(useRepository.findBySpaceAndDayAndUser(eq("s1"), any(), any())).thenAnswer(inv -> uses.stream()
                .filter(u -> u.by().equals(inv.getArgument(1)) && u.day().equals(inv.getArgument(2)))
                .findFirst());
        lenient().when(useRepository.findByIdInSpace(eq("s1"), any())).thenAnswer(inv -> uses.stream()
                .filter(u -> u.id().equals(inv.getArgument(1, String.class)))
                .findFirst().map(u -> SafewordUse.restore(u.id(), u.day(), u.by(), u.reflect())));
        lenient().doAnswer(inv -> {
            SafewordUse pause = inv.getArgument(1);
            int idx = -1;
            for (int i = 0; i < uses.size(); i++) {
                if (uses.get(i).id().equals(pause.id())) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) {
                uses.set(idx, SafewordUse.restore(pause.id(), pause.day(), pause.by(), pause.reflect()));
            } else {
                uses.add(pause);
            }
            return null;
        }).when(useRepository).save(eq("s1"), any());
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

        ArgumentCaptor<Safeword> captor = ArgumentCaptor.forClass(Safeword.class);
        verify(safewordRepository).save(eq("s1"), eq("alice"), captor.capture());
        assertThat(captor.getValue().word()).isEqualTo("暂停");
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
        String useId = uses.get(0).id();

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
        String useId = uses.get(0).id();

        service.reflectUse("alice", useId, "下次先说我去倒杯水");

        // 只看返回值回显的话，坏代码（改了内存对象却没写回端口）也能过。
        // save 被调两次：useSafeword 落一条喊停 + reflectUse 写回复盘；复盘那次的聚合带着文字。
        ArgumentCaptor<SafewordUse> captor = ArgumentCaptor.forClass(SafewordUse.class);
        verify(useRepository, times(2)).save(eq("s1"), captor.capture());
        assertThat(captor.getAllValues().get(1).reflect()).isEqualTo("下次先说我去倒杯水");
        assertThat(uses.get(0).reflect()).isEqualTo("下次先说我去倒杯水");
    }

    @Test
    void missingRowsAre404AndNoSpaceIs404() {
        assertThatThrownBy(() -> service.reflectUse("alice", "nope", "x")).isInstanceOf(BusinessException.class);
        when(spaceRepository.findActiveByMember("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.board("carol")).isInstanceOf(BusinessException.class);
        verify(useRepository, never()).save(any(), any());
    }
}
