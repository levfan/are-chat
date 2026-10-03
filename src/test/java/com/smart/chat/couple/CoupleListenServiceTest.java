package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
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
 * 误会倒带单测（系统裁剪后倾听与发声唯一保留项）：第一份只推 TA 等齐、双份齐才推 both、
 * 改写必须真落到那一行且不重复推 both、主题与正文长度守卫、两边全空不许落库、
 * 回看窗口只吃最近 30 天、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleListenServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleMisrewindMapper misMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleListenService service;

    private static final String DAY = LocalDate.now().toString();

    private final List<CoupleMisrewind> rows = new ArrayList<>();

    private void stubSpace() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        lenient().when(spaceMapper.findActiveByUser("alice")).thenReturn(Optional.of(space));
        lenient().when(spaceMapper.findActiveByUser("bob")).thenReturn(Optional.of(space));
    }

    private void stubStore() {
        lenient().when(misMapper.find(eq("s1"), eq(DAY), any(), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getTopic().equals(inv.getArgument(2)) && r.getFromUser().equals(inv.getArgument(3)))
                .findFirst().orElse(null));
        lenient().when(misMapper.findRecent(eq("s1"), any())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getDay().compareTo(inv.getArgument(1)) >= 0).toList());
        lenient().when(misMapper.insert(any(CoupleMisrewind.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });
        lenient().when(misMapper.updateById(any(CoupleMisrewind.class))).thenAnswer(inv -> 1);
    }

    @Test
    void misrewindCompletesAndRewriteSilent() {
        stubSpace();
        stubStore();
        service.misrewind("alice", "洗碗那件事", "我以为你在怪我", "我猜你其实只是想早点睡");
        verify(push).pushCoupleEvent(eq("misrewind"), eq("alice"), eq("bob"), any());
        service.misrewind("bob", "洗碗那件事", "我没怪你", "我就是困了");
        verify(push).pushCoupleEventBoth(eq("misrewind-done"), eq("bob"), eq("alice"), eq("bob"), any());

        service.misrewind("bob", "洗碗那件事", "改一下", "还是困了");

        // 改写不重复推 both，但必须真的改到那一行——只回一份新 VO 等于界面说谎
        verify(push, times(1)).pushCoupleEventBoth(eq("misrewind-done"), any(), any(), any(), any());
        verify(misMapper).updateById(any(CoupleMisrewind.class));
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1).getMine()).isEqualTo("改一下");
    }

    @Test
    void bothSidesShowUpPairedOnceBothWrote() {
        stubSpace();
        stubStore();
        service.misrewind("alice", "谁洗碗", "我以为你在怪我", "");
        service.misrewind("bob", "谁洗碗", "我没怪你", "");

        CoupleListenService.MisVO vo = service.today("alice").misrewinds().stream()
                .filter(m -> m.topic().equals("谁洗碗")).findFirst().orElseThrow();
        assertThat(vo.both()).isTrue();
        // alice 视角：mine 是自己的那一份，partner 是 bob 的
        assertThat(vo.mineThought()).isEqualTo("我以为你在怪我");
        assertThat(vo.partnerThought()).isEqualTo("我没怪你");

        CoupleListenService.MisVO bobView = service.today("bob").misrewinds().stream()
                .filter(m -> m.topic().equals("谁洗碗")).findFirst().orElseThrow();
        assertThat(bobView.mineThought()).isEqualTo("我没怪你");
    }

    @Test
    void guardsRejectBeforeAnyWrite() {
        stubSpace();
        stubStore();
        assertThatThrownBy(() -> service.misrewind("alice", " ", "有内容", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("主题要写一句");
        assertThatThrownBy(() -> service.misrewind("alice", "一件事", "", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("至少写一边");
        assertThatThrownBy(() -> service.misrewind("alice", "一件事", "一".repeat(201), ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("每条最多 200 字");
        assertThatThrownBy(() -> service.misrewind("alice", "一".repeat(61), "x", ""))
                .isInstanceOf(BusinessException.class).hasMessageContaining("主题最多 60 字");
        assertThat(rows).isEmpty();
        verify(misMapper, never()).insert(any(CoupleMisrewind.class));
    }

    @Test
    void noSpaceIs404() {
        when(spaceMapper.findActiveByUser("carol")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.today("carol"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("情侣空间");
    }
}
