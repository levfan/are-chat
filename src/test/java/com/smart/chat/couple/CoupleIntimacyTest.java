package com.smart.chat.couple;

import com.smart.chat.auth.AppUserService;
import com.smart.chat.im.FriendMapper;
import com.smart.chat.im.ImPushService;
import com.smart.chat.im.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 心动值（`GET /api/couple/intimacy`）算式单测。
 * 裁剪后六个进账项必须全部来自保留的 10 张卡：心情、贴贴双向往来、好事簿、留灯、安全词复盘、台账赚分。
 * 这里把每一项的权重和「单向贴贴不算一天」这类判定锁死——它们没有独立表，只有读时算，
 * 改错了不会被任何写路径的测试发现。
 */
@ExtendWith(MockitoExtension.class)
class CoupleIntimacyTest {

    @Mock
    private CoupleSpaceMapper spaceMapper;
    @Mock
    private CoupleInviteMapper inviteMapper;
    @Mock
    private CoupleAnniversaryMapper anniversaryMapper;
    @Mock
    private CoupleMoodMapper moodMapper;
    @Mock
    private CoupleActionMapper actionMapper;
    @Mock
    private CoupleEchoDeedMapper deedMapper;
    @Mock
    private CoupleQuestOvertimeMapper overtimeMapper;
    @Mock
    private CoupleCatchSafewordUseMapper safewordUseMapper;
    @Mock
    private CouplePointLedgerMapper ledgerMapper;
    @Mock
    private FriendMapper friendMapper;
    @Mock
    private UserProfileMapper profileMapper;
    @Mock
    private AppUserService userService;
    @Mock
    private ImPushService push;

    @InjectMocks
    private CoupleService service;

    private static final String DAY = LocalDate.now().toString();

    @BeforeEach
    void setUp() {
        CoupleSpace space = new CoupleSpace();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpace.STATUS_ACTIVE);
        lenient().when(spaceMapper.findActiveByUser(any())).thenReturn(Optional.of(space));
        lenient().when(moodMapper.findBySpace("s1")).thenReturn(List.of());
        lenient().when(actionMapper.findBySpace("s1")).thenReturn(List.of());
        lenient().when(deedMapper.findBySpace("s1")).thenReturn(List.of());
        lenient().when(overtimeMapper.findBySpace("s1")).thenReturn(List.of());
        lenient().when(safewordUseMapper.findBySpace("s1")).thenReturn(List.of());
        lenient().when(ledgerMapper.findBySpace("s1")).thenReturn(List.of());
        lenient().when(anniversaryMapper.findBySpace("s1")).thenReturn(List.of());
    }

    /** 毫秒时间戳按系统时区落到某天，用于造「同一天双方都贴过」的数据。 */
    private long atOffsetDays(int back) {
        return LocalDate.now().minusDays(back)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 3_600_000L;
    }

    @Test
    void scoreWeightsComeOnlyFromRetainedCards() {
        // 心情 3 条 ×1
        List<CoupleMood> moods = new ArrayList<>();
        moods.add(CoupleMood.of("s1", "alice", DAY, "HAPPY", null));
        moods.add(CoupleMood.of("s1", "alice", LocalDate.now().minusDays(1).toString(), "CALM", null));
        moods.add(CoupleMood.of("s1", "bob", DAY, "SAD", null));
        when(moodMapper.findBySpace("s1")).thenReturn(moods);

        // 贴贴双向往来 1 天 ×2：今天两人都贴了，昨天只有 alice 贴（单向不计）
        List<CoupleAction> actions = new ArrayList<>();
        actions.add(CoupleAction.of("s1", "alice", "HUG"));
        actions.get(0).setCreated(atOffsetDays(0));
        CoupleAction bobToday = CoupleAction.of("s1", "bob", "POKE");
        bobToday.setCreated(atOffsetDays(0));
        actions.add(bobToday);
        CoupleAction aliceOnly = CoupleAction.of("s1", "alice", "KISS");
        aliceOnly.setCreated(atOffsetDays(3));
        actions.add(aliceOnly);
        when(actionMapper.findBySpace("s1")).thenReturn(actions);

        // 好事簿 2 条 ×2
        when(deedMapper.findBySpace("s1")).thenReturn(List.of(
                CoupleEchoDeed.of("s1", "alice", "接我下班", DAY),
                CoupleEchoDeed.of("s1", "bob", "帮我吹头", DAY)));

        // 留灯 1 次 ×3（另一行只预报了加班没留灯，不算）
        CoupleQuestOvertime lit = CoupleQuestOvertime.of("s1", DAY, "alice", 22, "赶年结");
        lit.leaveLamp("bob", "灯给你留着");
        when(overtimeMapper.findBySpace("s1")).thenReturn(List.of(
                lit, CoupleQuestOvertime.of("s1", LocalDate.now().minusDays(1).toString(), "bob", 20, "")));

        // 安全词复盘 1 次 ×2（另一次没补复盘，不算）
        CoupleCatchSafewordUse reflected = CoupleCatchSafewordUse.of("s1", DAY, "alice");
        reflected.setReflect("当时是怕被丢下");
        when(safewordUseMapper.findBySpace("s1")).thenReturn(List.of(reflected,
                CoupleCatchSafewordUse.of("s1", LocalDate.now().minusDays(1).toString(), "bob")));

        // 台账赚分 ×1：SPEND 不参与进账
        when(ledgerMapper.findBySpace("s1")).thenReturn(List.of(
                CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_EARN, "好事簿：接我下班", 9),
                CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_SPEND, "发出愿望券：看一次海", 10)));

        CoupleService.IntimacyVO vo = service.intimacy("alice");
        CoupleService.IntimacyBreakdown d = vo.breakdown();

        assertThat(d.moodDays()).isEqualTo(3);
        assertThat(d.bondDays()).isEqualTo(1);
        assertThat(d.deedCount()).isEqualTo(2);
        assertThat(d.lampCount()).isEqualTo(1);
        assertThat(d.reflectCount()).isEqualTo(1);
        assertThat(d.pointEarned()).isEqualTo(9);
        // 3 + 1×2 + 2×2 + 1×3 + 1×2 + 9 = 23
        assertThat(vo.score()).isEqualTo(23);
        assertThat(vo.level()).isEqualTo(1);
        assertThat(vo.title()).isEqualTo("怦然心动");
        assertThat(vo.nextLevelAt()).isEqualTo(50);
        assertThat(vo.levelProgress()).isEqualTo(46);
    }

    @Test
    void oneSidedBondDayDoesNotCount() {
        List<CoupleAction> actions = new ArrayList<>();
        CoupleAction a = CoupleAction.of("s1", "alice", "HUG");
        a.setCreated(atOffsetDays(0));
        actions.add(a);
        CoupleAction b = CoupleAction.of("s1", "alice", "POKE");
        b.setCreated(atOffsetDays(1));
        actions.add(b);
        when(actionMapper.findBySpace("s1")).thenReturn(actions);

        CoupleService.IntimacyVO vo = service.intimacy("alice");

        // 两天都只有 alice 贴：双向天数必须是 0，否则心动值会靠一个人刷满
        assertThat(vo.breakdown().bondDays()).isZero();
        assertThat(vo.score()).isZero();
    }

    @Test
    void levelLadderKeepsThePreTrimThresholds() {
        when(ledgerMapper.findBySpace("s1")).thenReturn(List.of(
                CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_EARN, "凑数", 50)));
        assertThat(service.intimacy("alice").level()).isEqualTo(2);

        when(ledgerMapper.findBySpace("s1")).thenReturn(List.of(
                CouplePointLedger.of("s1", "alice", CouplePointLedger.TYPE_EARN, "凑数", 1300)));
        CoupleService.IntimacyVO top = service.intimacy("alice");
        assertThat(top.level()).isEqualTo(7);
        assertThat(top.title()).isEqualTo("相守一生");
        assertThat(top.nextLevelAt()).isNull();
        assertThat(top.levelProgress()).isEqualTo(100);
    }
}
