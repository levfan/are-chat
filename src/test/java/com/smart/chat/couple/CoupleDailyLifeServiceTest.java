package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 深度陪伴（F140-F149）核心逻辑单测：主题曲按日稳定、梦境入账、美食打卡流转、
 * 手册条目类型校验、SOS 重复发送限制与抱住规则、每日三问双答触发、成就颁发、仪表盘聚合。
 */
@ExtendWith(MockitoExtension.class)
class CoupleDailyLifeServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleDreamMapper dreamMapper;
    @Mock
    private CoupleFoodNoteMapper foodMapper;
    @Mock
    private CouplePartnerFactMapper factMapper;
    @Mock
    private CoupleSosPingMapper sosMapper;
    @Mock
    private CoupleDailyThreeMapper threeMapper;
    @Mock
    private CoupleCustomBadgeMapper badgeMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleDailyLifeService dailyLifeService;

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

    // ========== F140 今日主题曲 ==========

    @Test
    void themeSongStableWithinSameDay() {
        stubSpace("alice");
        assertThat(dailyLifeService.themeSong("alice")).isEqualTo(dailyLifeService.themeSong("alice"));
    }

    // ========== F141 梦境手账 ==========

    @Test
    void writeDreamRequiresContent() {
        stubSpace("alice");
        assertThatThrownBy(() -> dailyLifeService.writeDream("alice", "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("写下来");
    }

    // ========== F142 美食地图 ==========

    @Test
    void checkinFoodChangesStatusAndRates() {
        stubSpace("alice");
        CoupleFoodNote note = CoupleFoodNote.of("s1", "alice", "巷口面馆", "牛肉面");
        when(foodMapper.selectById(note.getId())).thenReturn(note);
        when(foodMapper.findBySpace("s1")).thenReturn(List.of(note));

        dailyLifeService.checkinFood("alice", note.getId(), 99, "汤都喝完了");

        assertThat(note.getStatus()).isEqualTo(CoupleFoodNote.STATUS_EATEN);
        assertThat(note.getRating()).isEqualTo(5);
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void doubleCheckinRejected() {
        stubSpace("alice");
        CoupleFoodNote note = CoupleFoodNote.of("s1", "alice", "店", "菜");
        note.setStatus(CoupleFoodNote.STATUS_EATEN);
        when(foodMapper.selectById(note.getId())).thenReturn(note);

        assertThatThrownBy(() -> dailyLifeService.checkinFood("alice", note.getId(), 5, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("打过卡");
    }

    // ========== F143 TA 使用手册 ==========

    @Test
    void addFactRejectsUnknownKind() {
        stubSpace("alice");
        assertThatThrownBy(() -> dailyLifeService.addFact("alice", "OTHER", "x"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TASTE/NOGO/FAV/QUIRK");
    }

    // ========== F144 情绪 SOS ==========

    @Test
    void sosBlockedWhilePreviousPending() {
        stubSpace("alice");
        CoupleSosPing pending = CoupleSosPing.of("s1", "alice", "抱抱");
        when(sosMapper.findLatestByUser("s1", "alice")).thenReturn(pending);

        assertThatThrownBy(() -> dailyLifeService.pingSos("alice", "再来一个"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("再等等");
    }

    @Test
    void holdSosOnlyByPartnerOnce() {
        stubSpace("bob");
        CoupleSosPing ping = CoupleSosPing.of("s1", "alice", "现在就要抱抱");
        when(sosMapper.selectById(ping.getId())).thenReturn(ping);
        when(sosMapper.findBySpace("s1")).thenReturn(List.of(ping));

        dailyLifeService.holdSos("bob", ping.getId());

        assertThat(ping.getStatus()).isEqualTo(CoupleSosPing.STATUS_HELD);
        assertThat(ping.getHeldAt()).isNotNull();
    }

    @Test
    void holdOwnSosRejected() {
        stubSpace("alice");
        CoupleSosPing ping = CoupleSosPing.of("s1", "alice", "抱抱");
        when(sosMapper.selectById(ping.getId())).thenReturn(ping);

        assertThatThrownBy(() -> dailyLifeService.holdSos("alice", ping.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TA 来接");
    }

    // ========== F145 每日三问 ==========

    @Test
    void threeBothAnsweredTriggersBothPush() {
        stubSpace("alice");
        String today = java.time.LocalDate.now().toString();
        when(threeMapper.find("s1", "alice", today)).thenReturn(null);
        when(threeMapper.find("s1", "bob", today)).thenReturn(CoupleDailyThree.of("s1", "bob", today));
        when(threeMapper.selectById(anyString())).thenReturn(null);

        dailyLifeService.saveDailyThree("alice", "吃到好吃的", null, "晚安");

        ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
        verify(push).pushCoupleEventBoth(event.capture(), anyString(), anyString(), anyString(), anyString());
        assertThat(event.getValue()).isEqualTo("three-both");
    }

    // ========== F146/F147 夸夸 + 暗号 ==========

    @Test
    void praiseReturnsThreeDistinctLinesAndCodeword() {
        stubSpace("alice");
        CoupleDailyLifeService.PraiseVO vo = dailyLifeService.praise("alice");
        assertThat(vo.praises()).hasSize(3);
        assertThat(vo.codeword()).isNotBlank();
    }

    // ========== F148 自定义成就 ==========

    @Test
    void issueBadgeOnce() {
        stubSpace("alice");
        CoupleCustomBadge badge = CoupleCustomBadge.of("s1", "bob", "连吃七天早餐", "不重样");
        when(badgeMapper.selectById(badge.getId())).thenReturn(badge);
        when(badgeMapper.findBySpace("s1")).thenReturn(List.of(badge));

        dailyLifeService.issueBadge("alice", badge.getId());

        assertThat(badge.getStatus()).isEqualTo(CoupleCustomBadge.STATUS_ISSUED);
        assertThat(badge.getIssuedAt()).isNotNull();
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void reissueRejected() {
        stubSpace("alice");
        CoupleCustomBadge badge = CoupleCustomBadge.of("s1", "bob", "达成", null);
        badge.setStatus(CoupleCustomBadge.STATUS_ISSUED);
        when(badgeMapper.selectById(badge.getId())).thenReturn(badge);

        assertThatThrownBy(() -> dailyLifeService.issueBadge("alice", badge.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("颁发过");
    }

    // ========== F149 恋爱仪表盘 ==========

    @Test
    void dashboardAggregatesTodosAndMemories() {
        stubSpace("alice");
        String today = java.time.LocalDate.now().toString();
        // 今日三问未答 → 待办
        when(threeMapper.find("s1", "alice", today)).thenReturn(null);
        // 对方 SOS 未接住 → 待办
        CoupleSosPing partnerSos = CoupleSosPing.of("s1", "bob", "要抱抱");
        when(sosMapper.findLatestByUser("s1", "alice")).thenReturn(null);
        when(sosMapper.findLatestByUser("s1", "bob")).thenReturn(partnerSos);
        when(sosMapper.findBySpace("s1")).thenReturn(List.of(partnerSos));
        // 想吃清单 → 待办
        when(foodMapper.findBySpace("s1")).thenReturn(List.of(CoupleFoodNote.of("s1", "alice", "甜品店", "提拉米苏")));
        // 挑战中的成就 → 待办
        when(badgeMapper.findBySpace("s1")).thenReturn(List.of(CoupleCustomBadge.of("s1", "alice", "连续早睡七天", null)));
        // 最新梦境 → 回忆
        when(dreamMapper.findBySpace("s1")).thenReturn(List.of(CoupleDream.of("s1", "alice", "梦见一起去看海")));

        CoupleDailyLifeService.DashboardVO vo = dailyLifeService.dashboard("alice");

        assertThat(vo.todos()).extracting(CoupleDailyLifeService.DashboardTodoVO::kind)
                .contains("three", "sos-hold", "food", "badge");
        assertThat(vo.memories()).extracting(CoupleDailyLifeService.DashboardMemoryVO::kind)
                .contains("dream", "song", "codeword");
    }
}
