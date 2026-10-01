package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 常用收藏（F207）核心逻辑单测：去空白/去重/超 6 拒存/超长键拒存、
 * 首次 insert 再次 updateById 的 upsert、双方列表互不串、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CouplePinServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleUserPinMapper pinMapper;
    @InjectMocks
    private CouplePinService service;

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        space.setCreated(System.currentTimeMillis());
        return space;
    }

    private void stubSpace(String me) {
        lenient().when(spaceMapper.findActiveByUser(me)).thenReturn(Optional.of(space()));
    }

    @Test
    void savePinsTrimsDedupesAndRejectsOverLimit() {
        stubSpace("alice");
        lenient().when(pinMapper.find("s1", "alice")).thenReturn(null);
        lenient().when(pinMapper.find("s1", "bob")).thenReturn(null);

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
        lenient().when(pinMapper.find("s1", "bob")).thenReturn(null);
        CoupleUserPin existing = CoupleUserPin.of("s1", "alice", "dining");
        when(pinMapper.find("s1", "alice")).thenReturn(null, existing);

        var first = service.savePins("alice", List.of("dining"));
        assertThat(first.mine()).containsExactly("dining");
        ArgumentCaptor<CoupleUserPin> cap = ArgumentCaptor.forClass(CoupleUserPin.class);
        verify(pinMapper).insert(cap.capture());
        assertThat(cap.getValue().getPins()).isEqualTo("dining");
        assertThat(cap.getValue().getFromUser()).isEqualTo("alice");

        var second = service.savePins("alice", List.of("dining", "cozy"));
        verify(pinMapper, times(1)).insert(any(CoupleUserPin.class));
        verify(pinMapper).updateById(existing);
        assertThat(existing.getPins()).isEqualTo("dining,cozy");
        assertThat(second.mine()).containsExactly("dining", "cozy");
    }

    @Test
    void pinsReturnsBothSides() {
        stubSpace("alice");
        when(pinMapper.find("s1", "alice")).thenReturn(CoupleUserPin.of("s1", "alice", "meeting,host"));
        when(pinMapper.find("s1", "bob")).thenReturn(null);

        var vo = service.pins("alice");
        assertThat(vo.mine()).containsExactly("meeting", "host");
        assertThat(vo.partner()).isEmpty();
    }

    @Test
    void noSpaceThrows404() {
        when(spaceMapper.findActiveByUser(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.pins("ghost"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("情侣空间");
    }
}
