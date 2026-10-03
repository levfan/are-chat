package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
 * 喜好 TOP10 互猜单测（系统裁剪后我们百科唯一保留项）：建榜才推人来猜、类目白名单、
 * 揭榜门槛要「TA 写过 + 我下过注」两边齐、猜漏的进重新认识清单、改写落库但不重复推、
 * 条数与单条字数上限、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleCodexServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleTopListMapper topListMapper;
    @Mock
    private CoupleTopGuessMapper topGuessMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleCodexService service;

    private final List<CoupleTopList> lists = new ArrayList<>();
    private final List<CoupleTopGuess> guesses = new ArrayList<>();

    private CoupleSpace space() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        return space;
    }

    private void stubSpace() {
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space()));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space()));
    }

    private void stubStores() {
        lenient().when(topListMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(lists));
        lenient().when(topListMapper.find(eq("s1"), eq("FOOD"), any())).thenAnswer(inv -> lists.stream()
                .filter(l -> l.getOwnerUser().equals(inv.getArgument(2))).findFirst().orElse(null));
        lenient().when(topGuessMapper.findBySpace("s1")).thenAnswer(inv -> List.copyOf(guesses));
        lenient().when(topGuessMapper.find(eq("s1"), eq("FOOD"), any())).thenAnswer(inv -> guesses.stream()
                .filter(g -> g.getGuesserUser().equals(inv.getArgument(2))).findFirst().orElse(null));
        lenient().when(topListMapper.insert(any(CoupleTopList.class))).thenAnswer(inv -> {
            lists.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(topGuessMapper.insert(any(CoupleTopGuess.class))).thenAnswer(inv -> {
            guesses.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(topListMapper.updateById(any(CoupleTopList.class))).thenAnswer(inv -> 1);
        lenient().when(topGuessMapper.updateById(any(CoupleTopGuess.class))).thenAnswer(inv -> 1);
    }

    @Test
    void topListGuessRevealRematch() {
        stubSpace();
        stubStores();
        service.topList("bob", "FOOD", "火锅,日料,烧烤");
        verify(push).pushCoupleEvent(eq("codex-top-list"), eq("bob"), eq("alice"), any());
        assertThatThrownBy(() -> service.topList("bob", "NOPE", "x"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("类目不存在");
        service.topGuess("alice", "FOOD", "火锅,甜品,烧烤");
        verify(push).pushCoupleEvent(eq("codex-top-guess"), eq("alice"), eq("bob"), any());
        CoupleCodexService.TopBoardVO food = service.overview("alice").tops().stream()
                .filter(t -> t.category().equals("FOOD")).findFirst().orElseThrow();
        assertThat(food.revealed()).isTrue();
        assertThat(food.rematch()).anyMatch(line -> line.contains("日料"));
    }

    @Test
    void notRevealedUntilBothSidesExist() {
        stubSpace();
        stubStores();
        service.topList("bob", "FOOD", "火锅");
        // 我只写了榜、还没猜，不该揭榜
        service.topList("alice", "FOOD", "甜品");
        CoupleCodexService.TopBoardVO food = service.overview("alice").tops().stream()
                .filter(t -> t.category().equals("FOOD")).findFirst().orElseThrow();
        assertThat(food.revealed()).isFalse();
        assertThat(food.rematch()).isEmpty();
    }

    @Test
    void rewriteLandsInDbButDoesNotRepush() {
        stubSpace();
        stubStores();
        service.topList("bob", "FOOD", "火锅");
        service.topGuess("alice", "FOOD", "火锅");
        verify(push, times(1)).pushCoupleEvent(eq("codex-top-guess"), any(), any(), any());

        service.topGuess("alice", "FOOD", "火锅,烧烤");

        // 改猜必须真落到那一行，而不是只回一份新 VO
        verify(topGuessMapper).updateById(any(CoupleTopGuess.class));
        assertThat(guesses).hasSize(1);
        assertThat(guesses.get(0).getItems()).isEqualTo("火锅,烧烤");
        // 也不该再骚扰对方一次
        verify(push, times(1)).pushCoupleEvent(eq("codex-top-guess"), any(), any(), any());
    }

    @Test
    void rejectsTooManyOrOverlongItems() {
        stubSpace();
        stubStores();
        String eleven = String.join(",", java.util.Collections.nCopies(11, "x"));
        assertThatThrownBy(() -> service.topList("alice", "FOOD", eleven))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多 10 项");
        assertThatThrownBy(() -> service.topList("alice", "FOOD", "一".repeat(61)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("每条最多 60 字");
        assertThatThrownBy(() -> service.topList("alice", "FOOD", " , "))
                .isInstanceOf(BusinessException.class).hasMessageContaining("至少写一项");
        assertThat(lists).isEmpty();
        verify(topListMapper, never()).insert(any(CoupleTopList.class));
    }

    @Test
    void noSpaceIs404() {
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.overview("carol"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("情侣空间");
    }
}
