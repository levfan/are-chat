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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 我们公司系（F240-F249）核心逻辑单测：封官限二与本人盖章、议案自裁禁止与否决留痕、
 * 述职 upsert 与双提交互见、发薪月限购与台账入账、点子转决议、10s 双签开会只推一次、
 * 职级定档、名片拼行、周报周锚过滤、无空间 404。
 */
@ExtendWith(MockitoExtension.class)
class CoupleBoardServiceTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleBoardRoleMapper roleMapper;
    @Mock
    private CoupleBoardVoteMapper voteMapper;
    @Mock
    private CoupleBoardReportMapper reportMapper;
    @Mock
    private CoupleBoardSalaryMapper salaryMapper;
    @Mock
    private CoupleBoardIdeaMapper ideaMapper;
    @Mock
    private CoupleBoardAttendMapper attendMapper;
    @Mock
    private CouplePointLedgerMapper pointLedgerMapper;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleBoardService service;

    private static final String DAY = LocalDate.now().toString();
    private static final String MONTH = YearMonth.now().toString();

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

    // ========== F240 头衔任命 ==========

    @Test
    void proposeRoleValidatesAndLimitsTwoPending() {
        stubSpace("alice");
        assertThatThrownBy(() -> service.proposeRole("alice", ""))
                .isInstanceOf(BusinessException.class).hasMessage("职位叫什么好呢（如财政部长）");

        List<CoupleBoardRole> pending = List.of(
                CoupleBoardRole.of("s1", "alice", "bob", "财政部长"),
                CoupleBoardRole.of("s1", "alice", "bob", "首席大厨"));
        when(roleMapper.findBySpace("s1")).thenReturn(pending);
        assertThatThrownBy(() -> service.proposeRole("alice", "第三个职位"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("最多封 2 个职位");

        when(roleMapper.findBySpace("s1")).thenReturn(List.of());
        service.proposeRole("alice", "财政部长");
        verify(roleMapper).insert(any(CoupleBoardRole.class));
        verify(push).pushCoupleEvent(eq("board-role"), eq("alice"), eq("bob"), any());
    }

    @Test
    void appointGuardsOwnershipAndRepeat() {
        stubSpace("alice");
        stubSpace("bob");
        CoupleBoardRole role = CoupleBoardRole.of("s1", "alice", "bob", "财政部长");
        role.setId("r1");
        when(roleMapper.selectById("r1")).thenReturn(role);

        assertThatThrownBy(() -> service.appoint("alice", "r1"))
                .isInstanceOf(BusinessException.class).hasMessage("任命章要本人盖，TA 才能生效");

        service.appoint("bob", "r1");
        assertThat(role.appointedFlag()).isTrue();
        verify(roleMapper).updateById(role);
        verify(push).pushCoupleEvent(eq("board-appointed"), eq("bob"), eq("alice"), any());

        assertThatThrownBy(() -> service.appoint("bob", "r1"))
                .isInstanceOf(BusinessException.class).hasMessage("这个职位已经生效啦");
    }

    // ========== F241 董事会决议 ==========

    @Test
    void voteForbidsSelfDecisionAndRecordsVeto() {
        stubSpace("bob");
        CoupleBoardVote own = CoupleBoardVote.of("s1", "bob", "下周去露营");
        own.setId("v1");
        when(voteMapper.selectById("v1")).thenReturn(own);
        assertThatThrownBy(() -> service.vote("bob", "v1", true))
                .isInstanceOf(BusinessException.class).hasMessage("自己的议案不能自己裁");

        CoupleBoardVote theirs = CoupleBoardVote.of("s1", "alice", "换一张乳胶枕");
        theirs.setId("v2");
        when(voteMapper.selectById("v2")).thenReturn(theirs);
        service.vote("bob", "v2", false);
        assertThat(theirs.getStatus()).isEqualTo(CoupleBoardVote.STATUS_VETOED);
        assertThat(theirs.getVetoBy()).isEqualTo("bob");
        verify(push).pushCoupleEventBoth(eq("board-vetoed"), eq("bob"), eq("alice"), eq("bob"), any());

        assertThatThrownBy(() -> service.vote("bob", "v2", true))
                .isInstanceOf(BusinessException.class).hasMessage("这个决议已经有结论了");
    }

    @Test
    void proposeVoteInsertsAndPushesPartner() {
        stubSpace("alice");
        service.proposeVote("alice", "国庆去海边");
        verify(voteMapper).insert(any(CoupleBoardVote.class));
        verify(push).pushCoupleEvent(eq("board-proposal"), eq("alice"), eq("bob"), any());
    }

    // ========== F242 年度述职 ==========

    @Test
    void saveReportUpsertsAndHiddenUntilBothIn() {
        stubSpace("alice");
        stubSpace("bob");
        String year = String.valueOf(LocalDate.now().getYear());
        assertThatThrownBy(() -> service.saveReport("alice", "23", "述职", "目标"))
                .isInstanceOf(BusinessException.class).hasMessage("年度写四位数字就行");

        List<CoupleBoardReport> rows = new ArrayList<>();
        when(reportMapper.find(eq("s1"), anyString(), anyString())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getYear().equals(inv.getArgument(1)) && r.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        when(reportMapper.insert(any(CoupleBoardReport.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });

        service.saveReport("alice", null, "今年很努力", "明年学会做饭");
        verify(push).pushCoupleEvent(eq("board-report-mine"), eq("alice"), eq("bob"), any());
        assertThat(service.overview("alice").report().partnerReview()).isNull();

        service.saveReport("bob", year, "今年辛苦了", "明年一起健身");
        assertThat(service.overview("alice").report().bothIn()).isTrue();
        assertThat(service.overview("alice").report().partnerGoal()).isEqualTo("明年一起健身");

        service.saveReport("alice", year, "改写述职", "改写目标");
        verify(reportMapper, times(2)).insert(any(CoupleBoardReport.class));
        verify(reportMapper, times(1)).updateById(any(CoupleBoardReport.class));
    }

    // ========== F244 发薪日 ==========

    @Test
    void paySalaryOncePerMonthAndCreditsLedger() {
        stubSpace("alice");
        List<CoupleBoardSalary> rows = new ArrayList<>();
        when(salaryMapper.find(eq("s1"), anyString(), anyString())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getMonth().equals(inv.getArgument(1)) && r.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        when(salaryMapper.insert(any(CoupleBoardSalary.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });

        service.paySalary("alice", "谢谢你本月也好好吃饭");
        ArgumentCaptor<CouplePointLedger> ledgerCaptor = ArgumentCaptor.forClass(CouplePointLedger.class);
        verify(pointLedgerMapper).insert(ledgerCaptor.capture());
        assertThat(ledgerCaptor.getValue().getType()).isEqualTo(CouplePointLedger.TYPE_EARN);
        assertThat(ledgerCaptor.getValue().getPoints()).isEqualTo(5);
        verify(push).pushCoupleEvent(eq("board-salary"), eq("alice"), eq("bob"), any());

        assertThatThrownBy(() -> service.paySalary("alice", "又想发一次"))
                .isInstanceOf(BusinessException.class).hasMessage("本月工资已发放，感谢留到下个月再说");
    }

    // ========== F245 金点子箱 ==========

    @Test
    void adoptIdeaCreatesPendingVote() {
        stubSpace("bob");
        stubSpace("alice");
        CoupleBoardIdea mine = CoupleBoardIdea.of("s1", "bob", "周末固定散步一小时");
        mine.setId("i1");
        when(ideaMapper.selectById("i1")).thenReturn(mine);
        assertThatThrownBy(() -> service.adoptIdea("bob", "i1"))
                .isInstanceOf(BusinessException.class).hasMessage("自己的点子要对方来采纳才算数");

        service.adoptIdea("alice", "i1");
        assertThat(mine.adoptedFlag()).isTrue();
        assertThat(mine.getVoteId()).isNotBlank();
        ArgumentCaptor<CoupleBoardVote> captor = ArgumentCaptor.forClass(CoupleBoardVote.class);
        verify(voteMapper).insert(captor.capture());
        assertThat(captor.getValue().getTitle()).startsWith("金点子：");
        assertThat(captor.getValue().getProposer()).isEqualTo("bob");
        verify(push).pushCoupleEventBoth(eq("board-idea-adopted"), eq("alice"), any(), any(), any());

        assertThatThrownBy(() -> service.adoptIdea("alice", "i1"))
                .isInstanceOf(BusinessException.class).hasMessage("这个点子已经转成决议了");
    }

    // ========== F247 会议签到 ==========

    @Test
    void attendConvenesWithinTenSecondWindowOnce() {
        stubSpace("alice");
        stubSpace("bob");
        String day = DAY;
        List<CoupleBoardAttend> rows = new ArrayList<>();
        when(attendMapper.find(eq("s1"), anyString(), anyString())).thenAnswer(inv -> rows.stream()
                .filter(r -> r.getDay().equals(inv.getArgument(1)) && r.getFromUser().equals(inv.getArgument(2)))
                .findFirst().orElse(null));
        when(attendMapper.insert(any(CoupleBoardAttend.class))).thenAnswer(inv -> {
            rows.add(inv.getArgument(0));
            return 1;
        });

        service.attend("alice");
        verify(push, never()).pushCoupleEventBoth(eq("board-convened"), any(), any(), any(), any());

        service.attend("bob");
        verify(push).pushCoupleEventBoth(eq("board-convened"), eq("bob"), eq("alice"), eq("bob"), any());

        service.attend("alice");
        verify(push, times(1)).pushCoupleEventBoth(eq("board-convened"), any(), any(), any(), any());
    }

    // ========== F243/F248/F249 聚合 ==========

    @Test
    void membersRanksAndCardAndWeeklyAggregate() {
        stubSpace("alice");
        CoupleBoardRole appointed = CoupleBoardRole.of("s1", "bob", "alice", "财政部长");
        appointed.setAppointed(1);
        when(roleMapper.findBySpace("s1")).thenReturn(List.of(appointed));
        long now = System.currentTimeMillis();
        CoupleBoardVote passed = CoupleBoardVote.of("s1", "bob", "议案一");
        passed.setStatus(CoupleBoardVote.STATUS_PASSED);
        when(voteMapper.findBySpace("s1")).thenReturn(List.of(passed));
        CouplePointLedger big = CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_EARN, "攒", 25);
        CouplePointLedger old = CouplePointLedger.of("s1", "bob", CouplePointLedger.TYPE_EARN, "很久以前", 7);
        old.setCreated(now - 30L * 24 * 3600 * 1000);
        CouplePointLedger spend = CouplePointLedger.of("s1", "bob", CouplePointLedger.TYPE_SPEND, "花", 9);
        when(pointLedgerMapper.findBySpace("s1")).thenReturn(List.of(big, old, spend));

        CoupleBoardService.OverviewVO vo = service.overview("alice");
        assertThat(vo.members()).hasSize(2);
        assertThat(vo.members().get(0).rank()).isEqualTo("正式职员");
        assertThat(vo.members().get(0).nextRank()).isEqualTo("小组主管");
        assertThat(vo.members().get(0).pointsToNext()).isEqualTo(35);
        assertThat(vo.members().get(0).titles()).containsExactly("财政部长");
        assertThat(vo.members().get(1).earned()).isEqualTo(7);
        assertThat(vo.members().get(1).rank()).isEqualTo("实习生");

        assertThat(vo.card().lines()).anyMatch(l -> l.startsWith("【我们公司】" + DAY));
        assertThat(vo.card().lines()).anyMatch(l -> l.contains("财政部长"));
        assertThat(vo.card().lines()).anyMatch(l -> l.contains("通过 1 项"));
        assertThat(vo.card().lines()).anyMatch(l -> l.contains("发薪日：随缘"));

        assertThat(vo.weekly().votes()).isEqualTo(1);
        assertThat(vo.weekly().pointsEarned()).isEqualTo(25);
    }

    @Test
    void salaryStateFindsEarliestPayDayOfMonth() {
        stubSpace("alice");
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        CoupleBoardSalary early = CoupleBoardSalary.of("s1", MONTH, "bob", "早发的谢谢");
        early.setCreated(monthStart.plusDays(2).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        CoupleBoardSalary late = CoupleBoardSalary.of("s1", MONTH, "alice", "晚发的谢谢");
        late.setCreated(monthStart.plusDays(4).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        when(salaryMapper.findBySpace("s1")).thenReturn(List.of(late, early));

        CoupleBoardService.OverviewVO vo = service.overview("alice");
        assertThat(vo.salary().payDay()).isEqualTo(3);
        assertThat(vo.salary().bothPaid()).isTrue();
    }

    // ========== 通用 ==========

    @Test
    void noSpaceThrows404() {
        when(spaceMapper.findActiveByUser("solo")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.overview("solo"))
                .isInstanceOf(BusinessException.class).hasMessage("还没有建立情侣空间，先邀请一位好友吧");
    }
}
