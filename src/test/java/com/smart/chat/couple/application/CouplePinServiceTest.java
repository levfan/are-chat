package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.pin.UserPin;
import com.smart.chat.couple.domain.pin.UserPinRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 常用收藏（F207）核心逻辑单测：去空白/去重/超 6 拒存/超长键拒存、
 * 反复保存改的还是同一行（insert/update 的分流见 UserPinRepositoryAdapterTest）、
 * 双方列表互不串、无空间 404。
 * 假表建在 {@link UserPinRepository} 这一层，PO 与 Mapper 不出现在用例里。
 */
@ExtendWith(MockitoExtension.class)
class CouplePinServiceTest {

    @Mock
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private UserPinRepository pinRepository;
    @InjectMocks
    private CouplePinService service;

    private CoupleSpace space() {
        return CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, System.currentTimeMillis(), null, null, null, null, null, null, null);
    }

    private void stubSpace(String me) {
        lenient().when(spaceRepository.findActiveByMember(me)).thenReturn(Optional.of(space()));
    }

    @Test
    void savePinsTrimsDedupesAndRejectsOverLimit() {
        stubSpace("alice");
        lenient().when(pinRepository.findBySpaceAndUser("s1", "alice")).thenReturn(Optional.empty());
        lenient().when(pinRepository.findBySpaceAndUser("s1", "bob")).thenReturn(Optional.empty());

        var vo = service.savePins("alice", Arrays.asList(" meeting ", "meeting", "", null, "host"));
        assertThat(vo.mine()).containsExactly("meeting", "host");

        assertThatThrownBy(() -> service.savePins("alice", List.of("a", "b", "c", "d", "e", "f", "g")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("6");

        assertThatThrownBy(() -> service.savePins("alice", List.of("x".repeat(41))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("太长");
    }

    @Test
    void savePinsUpsertsInsertThenUpdate() {
        stubSpace("alice");
        UserPin[] stored = new UserPin[1];
        lenient().when(pinRepository.findBySpaceAndUser("s1", "alice"))
                .thenAnswer(inv -> Optional.ofNullable(stored[0]));
        lenient().when(pinRepository.findBySpaceAndUser("s1", "bob")).thenReturn(Optional.empty());
        lenient().doAnswer(inv -> {
            stored[0] = inv.getArgument(0);
            return null;
        }).when(pinRepository).save(any(UserPin.class));

        var first = service.savePins("alice", List.of("dining"));
        assertThat(first.mine()).containsExactly("dining");
        ArgumentCaptor<UserPin> cap = ArgumentCaptor.forClass(UserPin.class);
        verify(pinRepository).save(cap.capture());
        assertThat(cap.getValue().joined()).isEqualTo("dining");
        assertThat(cap.getValue().fromUser()).isEqualTo("alice");

        var second = service.savePins("alice", List.of("dining", "cozy"));
        verify(pinRepository, times(2)).save(cap.capture());
        assertThat(cap.getAllValues().get(1).id()).as("第二次改的还是同一行，没有另起一行").isEqualTo(cap.getAllValues().get(0).id());
        assertThat(cap.getAllValues().get(1).joined()).isEqualTo("dining,cozy");
        assertThat(second.mine()).containsExactly("dining", "cozy");
    }

    @Test
    void pinsReturnsBothSides() {
        stubSpace("alice");
        when(pinRepository.findBySpaceAndUser("s1", "alice"))
                .thenReturn(Optional.of(UserPin.restore("p1", "s1", "alice", "meeting,host", 1L, 1L)));
        when(pinRepository.findBySpaceAndUser("s1", "bob")).thenReturn(Optional.empty());

        var vo = service.pins("alice");
        assertThat(vo.mine()).containsExactly("meeting", "host");
        assertThat(vo.partner()).isEmpty();
    }

    @Test
    void noSpaceThrows404() {
        when(spaceRepository.findActiveByMember(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.pins("ghost"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("情侣空间");
    }
}
