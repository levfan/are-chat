package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
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
 * 生活经营系（F180-F189）核心逻辑单测：议题关闭推送、主理人按周轮换与越权拦截、
 * 技能交换自揭拦截与状态机、月度互评星级钳制与双评互见、应急卡最少一项、
 * 存档温度钳制、积分默认值/钳制/余额门槛、OURS 认领规则、策划案日期窗口与状态只进不退、经营周报聚合。
 */
@ExtendWith(MockitoExtension.class)
class CoupleManageServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleFamilyMeetingMapper meetingMapper;
    @Mock
    private CoupleWeekHostMapper hostMapper;
    @Mock
    private CoupleSkillSwapMapper skillMapper;
    @Mock
    private CoupleMonthReviewMapper reviewMapper;
    @Mock
    private CoupleEmergencyCardMapper cardMapper;
    @Mock
    private CoupleMonthSnapshotMapper snapshotMapper;
    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private CoupleFiveYearPlanMapper planMapper;
    @Mock
    private CoupleAnnivPlanMapper annivMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleManageService manageService;

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

    // ========== F180 家庭会议 ==========

    @Test
    void addMeetingUsesCurrentWeekAndPushesPartner() {
        stubSpace("alice");
        when(meetingMapper.findBySpace("s1")).thenReturn(List.of());

        manageService.addMeeting("alice", "周末去哪吃饭", null);

        ArgumentCaptor<CoupleFamilyMeeting> captor = ArgumentCaptor.forClass(CoupleFamilyMeeting.class);
        verify(meetingMapper).insert(captor.capture());
        assertThat(captor.getValue().getWeek()).isEqualTo(CoupleManageService.currentWeek());
        assertThat(captor.getValue().getClosed()).isZero();
        verify(push).pushCoupleEvent(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void addMeetingRejectsBlankTopic() {
        stubSpace("alice");
        assertThatThrownBy(() -> manageService.addMeeting("alice", "  ", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("议题");
    }

    @Test
    void closeMeetingPushesBothOnceOnly() {
        stubSpace("alice");
        stubSpace("bob");
        CoupleFamilyMeeting row = CoupleFamilyMeeting.of("s1", "2026-01-05", "alice", "谁倒垃圾");
        when(meetingMapper.selectById(row.getId())).thenReturn(row);
        when(meetingMapper.findBySpace("s1")).thenReturn(List.of(row));

        manageService.closeMeeting("alice", row.getId());
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());

        // 已关闭再关：不重复推送
        manageService.closeMeeting("bob", row.getId());
        verify(push, times(1)).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ========== F181 本周主理人 ==========

    @Test
    void hostRotatesByWeekParity() {
        CoupleSpace space = space();
        assertThat(CoupleManageService.hostOf(space, "2026-01-05")).isEqualTo("bob");
        assertThat(CoupleManageService.hostOf(space, "2026-01-12")).isEqualTo("alice");
        assertThat(CoupleManageService.hostOf(space, "2026-01-06")).isEqualTo("alice");
    }

    @Test
    void saveHostPlanBlockedForNonHost() {
        stubSpace("bob");
        String week = CoupleManageService.currentWeek();
        String host = CoupleManageService.hostOf(space(), week);
        String nonHost = host.equals("alice") ? "bob" : "alice";

        assertThatThrownBy(() -> manageService.saveHostPlan(nonHost, "周五火锅"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当家");
    }

    @Test
    void hostCanSavePlan() {
        String week = CoupleManageService.currentWeek();
        String host = CoupleManageService.hostOf(space(), week);
        stubSpace(host);
        when(hostMapper.find("s1", week)).thenReturn(null);

        CoupleManageService.WeekHostVO vo = manageService.saveHostPlan(host, "周五火锅 🍲");
        ArgumentCaptor<CoupleWeekHost> captor = ArgumentCaptor.forClass(CoupleWeekHost.class);
        verify(hostMapper).insert(captor.capture());
        assertThat(captor.getValue().getPlan()).isEqualTo("周五火锅 🍲");
        assertThat(vo.mine()).isTrue();
    }

    // ========== F182 技能交换所 ==========

    @Test
    void takeSkillRejectsOwnListingAndTakenOnes() {
        stubSpace("alice");
        stubSpace("bob");
        CoupleSkillSwap own = CoupleSkillSwap.of("s1", "alice", "做饭", "修电脑");
        when(skillMapper.selectById(own.getId())).thenReturn(own);
        assertThatThrownBy(() -> manageService.takeSkill("alice", own.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("自己的摊");

        CoupleSkillSwap taken = CoupleSkillSwap.of("s1", "alice", "做饭", "修电脑");
        taken.setStatus(CoupleSkillSwap.STATUS_TAKEN);
        when(skillMapper.selectById(taken.getId())).thenReturn(taken);
        assertThatThrownBy(() -> manageService.takeSkill("bob", taken.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("成交");
    }

    @Test
    void takeThenDoneSkillFlow() {
        stubSpace("bob");
        CoupleSkillSwap row = CoupleSkillSwap.of("s1", "alice", "做饭", "修电脑");
        when(skillMapper.selectById(row.getId())).thenReturn(row);
        when(skillMapper.findBySpace("s1")).thenReturn(List.of(row));

        manageService.takeSkill("bob", row.getId());
        assertThat(row.getStatus()).isEqualTo(CoupleSkillSwap.STATUS_TAKEN);

        manageService.doneSkill("bob", row.getId());
        assertThat(row.getStatus()).isEqualTo(CoupleSkillSwap.STATUS_DONE);
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ========== F183 月度互评 ==========

    @Test
    void monthReviewClampsStarsAndNeedsBothToReveal() {
        String month = YearMonth.now().toString();
        stubSpace("alice");
        when(reviewMapper.find("s1", month, "alice")).thenReturn(null);
        when(reviewMapper.find("s1", month, "bob")).thenReturn(null);

        manageService.saveMonthReview("alice", 9, "少熬夜");
        ArgumentCaptor<CoupleMonthReview> captor = ArgumentCaptor.forClass(CoupleMonthReview.class);
        verify(reviewMapper).insert(captor.capture());
        assertThat(captor.getValue().getStars()).isEqualTo(5);
        assertThat(captor.getValue().getMonth()).isEqualTo(month);

        // 只有我评了：对方不可见
        when(reviewMapper.find("s1", month, "alice")).thenReturn(captor.getValue());
        CoupleManageService.MonthReviewPairVO half = manageService.monthReviews("alice");
        assertThat(half.partner()).isNull();
        assertThat(half.bothDone()).isFalse();

        // 双方都评了：互见
        when(reviewMapper.find("s1", month, "bob")).thenReturn(CoupleMonthReview.of("s1", month, "bob", 4, "多抱抱"));
        CoupleManageService.MonthReviewPairVO full = manageService.monthReviews("alice");
        assertThat(full.partner().stars()).isEqualTo(4);
        assertThat(full.bothDone()).isTrue();
    }

    // ========== F184 家庭应急卡 ==========

    @Test
    void emergencyCardRequiresAtLeastOneField() {
        stubSpace("alice");
        assertThatThrownBy(() -> manageService.saveEmergencyCard("alice", "", null, "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("至少写一项");
    }

    // ========== F185 情侣存档点 ==========

    @Test
    void snapshotClampsLoveTemp() {
        stubSpace("alice");
        when(snapshotMapper.find(anyString(), anyString(), anyString())).thenReturn(null);
        when(snapshotMapper.findBySpace("s1")).thenReturn(List.of());

        manageService.saveSnapshot("alice", 150, "忙但充实", "感冒好了");
        ArgumentCaptor<CoupleMonthSnapshot> captor = ArgumentCaptor.forClass(CoupleMonthSnapshot.class);
        verify(snapshotMapper).insert(captor.capture());
        assertThat(captor.getValue().getLoveTemp()).isEqualTo(100);
        assertThat(captor.getValue().getMonth()).isEqualTo(YearMonth.now().toString());
    }

    // ========== F186 家务积分市场 ==========

    @Test
    void earnPointsDefaultsAndClamps() {
        stubSpace("alice");
        when(ledgerMapper.findBySpace("s1")).thenReturn(List.of());

        manageService.earnPoints("alice", "拖地", null);
        manageService.earnPoints("alice", "洗油烟机", 999);

        ArgumentCaptor<CouplePointLedger> captor = ArgumentCaptor.forClass(CouplePointLedger.class);
        verify(ledgerMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues().get(0).getPoints()).isEqualTo(5);
        assertThat(captor.getAllValues().get(1).getPoints()).isEqualTo(200);
    }

    @Test
    void redeemChecksBalanceGate() {
        stubSpace("alice");
        CouplePointLedger earn = CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_EARN, "做饭", 10);
        when(ledgerMapper.findBySpace("s1")).thenReturn(List.of(earn));

        // 电影之夜 20 分，只有 10 分余额
        assertThatThrownBy(() -> manageService.redeemReward("alice", "MOVIE"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还差 10");

        // 二十分钟抱抱 10 分，刚好够
        manageService.redeemReward("alice", "HUG20");
        ArgumentCaptor<CouplePointLedger> captor = ArgumentCaptor.forClass(CouplePointLedger.class);
        verify(ledgerMapper).insert(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(CouplePointLedger.TYPE_SPEND);
    }

    // ========== F187 五年计划双轨 ==========

    @Test
    void claimRulesForTracks() {
        stubSpace("bob");
        CoupleFiveYearPlan mine = CoupleFiveYearPlan.of("s1", CoupleFiveYearPlan.TRACK_MINE, "alice", "考下证书");
        when(planMapper.selectById(mine.getId())).thenReturn(mine);
        assertThatThrownBy(() -> manageService.claimPlan("bob", mine.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不用认领");

        CoupleFiveYearPlan ours = CoupleFiveYearPlan.of("s1", CoupleFiveYearPlan.TRACK_OURS, "alice", "存一趟旅行基金");
        ours.setOwnerUser("alice");
        when(planMapper.selectById(ours.getId())).thenReturn(ours);
        assertThatThrownBy(() -> manageService.claimPlan("bob", ours.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("认领");

        ours.setOwnerUser(null);
        when(planMapper.findBySpace("s1")).thenReturn(List.of(ours));
        manageService.claimPlan("bob", ours.getId());
        assertThat(ours.getOwnerUser()).isEqualTo("bob");
    }

    // ========== F188 纪念日策划案 ==========

    @Test
    void annivPlanDateWindow() {
        stubSpace("alice");
        assertThatThrownBy(() -> manageService.addAnnivPlan("alice", LocalDate.now().minusDays(1).toString(), "上周纪念日", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("400 天");

        assertThatThrownBy(() -> manageService.addAnnivPlan("alice", "2026/05/20", "格式错误", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("日期");
    }

    @Test
    void annivPlanStatusOnlyMovesForward() {
        stubSpace("alice");
        stubSpace("bob");
        CoupleAnnivPlan row = CoupleAnnivPlan.of("s1", LocalDate.now().plusDays(30).toString(), "一周年", "alice", "重走初次约会的路");
        when(annivMapper.selectById(row.getId())).thenReturn(row);
        when(annivMapper.findBySpace("s1")).thenReturn(List.of(row));

        manageService.advanceAnnivPlan("bob", row.getId());
        assertThat(row.getStatus()).isEqualTo(CoupleAnnivPlan.STATUS_LOCKED);

        manageService.advanceAnnivPlan("bob", row.getId());
        assertThat(row.getStatus()).isEqualTo(CoupleAnnivPlan.STATUS_DONE);
        verify(push).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());

        manageService.advanceAnnivPlan("bob", row.getId());
        assertThat(row.getStatus()).isEqualTo(CoupleAnnivPlan.STATUS_DONE);
        verify(push, times(1)).pushCoupleEventBoth(anyString(), anyString(), anyString(), anyString(), anyString());
    }

    // ========== F189 经营周报 ==========

    @Test
    void weeklyAggregatesMeetingsAndPoints() {
        stubSpace("alice");
        CoupleFamilyMeeting meeting = CoupleFamilyMeeting.of("s1", CoupleManageService.currentWeek(), "alice", "谁交水电费");
        when(meetingMapper.findBySpace("s1")).thenReturn(List.of(meeting));
        CouplePointLedger earn = CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_EARN, "大扫除", 25);
        when(ledgerMapper.findBySpace("s1")).thenReturn(List.of(earn));
        when(hostMapper.find("s1", CoupleManageService.currentWeek())).thenReturn(null);

        CoupleManageService.ManageWeeklyVO vo = manageService.weekly("alice");
        assertThat(vo.meetings()).isEqualTo(1);
        assertThat(vo.earned()).isEqualTo(25);
        assertThat(vo.summary()).contains("议了 1 个议题").contains("赚积分 25");
    }

    @Test
    void weeklyEmptySummaryEncouragesStart() {
        stubSpace("alice");
        when(meetingMapper.findBySpace("s1")).thenReturn(List.of());
        when(ledgerMapper.findBySpace("s1")).thenReturn(List.of());
        when(hostMapper.find("s1", CoupleManageService.currentWeek())).thenReturn(null);

        CoupleManageService.ManageWeeklyVO vo = manageService.weekly("alice");
        assertThat(vo.summary()).contains("还没开张");
    }
}
