package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.persistence.CoupleActionPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleActionMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleAnniversaryMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleCatchSafewordUsePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleCatchSafewordUseMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleEchoDeedPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleEchoDeedMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleInviteMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleMoodPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleMoodMapper;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerPO;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestOvertimePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestOvertimeMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpacePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.smart.chat.identity.application.AppUserService;
import com.smart.chat.messaging.infrastructure.persistence.FriendMapper;
import com.smart.chat.messaging.infrastructure.transport.ImPushService;
import com.smart.chat.messaging.infrastructure.persistence.UserProfileMapper;
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
        CoupleSpacePO space = new CoupleSpacePO();
        space.setId("s1");
        space.setUserA("alice");
        space.setUserB("bob");
        space.setStatus(CoupleSpacePO.STATUS_ACTIVE);
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
        List<CoupleMoodPO> moods = new ArrayList<>();
        moods.add(CoupleMoodPO.of("s1", "alice", DAY, "HAPPY", null));
        moods.add(CoupleMoodPO.of("s1", "alice", LocalDate.now().minusDays(1).toString(), "CALM", null));
        moods.add(CoupleMoodPO.of("s1", "bob", DAY, "SAD", null));
        when(moodMapper.findBySpace("s1")).thenReturn(moods);

        // 贴贴双向往来 1 天 ×2：今天两人都贴了，昨天只有 alice 贴（单向不计）
        List<CoupleActionPO> actions = new ArrayList<>();
        actions.add(CoupleActionPO.of("s1", "alice", "HUG"));
        actions.get(0).setCreated(atOffsetDays(0));
        CoupleActionPO bobToday = CoupleActionPO.of("s1", "bob", "POKE");
        bobToday.setCreated(atOffsetDays(0));
        actions.add(bobToday);
        CoupleActionPO aliceOnly = CoupleActionPO.of("s1", "alice", "KISS");
        aliceOnly.setCreated(atOffsetDays(3));
        actions.add(aliceOnly);
        when(actionMapper.findBySpace("s1")).thenReturn(actions);

        // 好事簿 2 条 ×2
        when(deedMapper.findBySpace("s1")).thenReturn(List.of(
                CoupleEchoDeedPO.of("s1", "alice", "接我下班", DAY),
                CoupleEchoDeedPO.of("s1", "bob", "帮我吹头", DAY)));

        // 留灯 1 次 ×3（另一行只预报了加班没留灯，不算）
        CoupleQuestOvertimePO lit = CoupleQuestOvertimePO.of("s1", DAY, "alice", 22, "赶年结");
        lit.leaveLamp("bob", "灯给你留着");
        when(overtimeMapper.findBySpace("s1")).thenReturn(List.of(
                lit, CoupleQuestOvertimePO.of("s1", LocalDate.now().minusDays(1).toString(), "bob", 20, "")));

        // 安全词复盘 1 次 ×2（另一次没补复盘，不算）
        CoupleCatchSafewordUsePO reflected = CoupleCatchSafewordUsePO.of("s1", DAY, "alice");
        reflected.setReflect("当时是怕被丢下");
        when(safewordUseMapper.findBySpace("s1")).thenReturn(List.of(reflected,
                CoupleCatchSafewordUsePO.of("s1", LocalDate.now().minusDays(1).toString(), "bob")));

        // 台账赚分 ×1：SPEND 不参与进账
        when(ledgerMapper.findBySpace("s1")).thenReturn(List.of(
                CouplePointLedgerPO.of("s1", "alice", CouplePointLedgerPO.TYPE_EARN, "好事簿：接我下班", 9),
                CouplePointLedgerPO.of("s1", "alice", CouplePointLedgerPO.TYPE_SPEND, "发出愿望券：看一次海", 10)));

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
        List<CoupleActionPO> actions = new ArrayList<>();
        CoupleActionPO a = CoupleActionPO.of("s1", "alice", "HUG");
        a.setCreated(atOffsetDays(0));
        actions.add(a);
        CoupleActionPO b = CoupleActionPO.of("s1", "alice", "POKE");
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
                CouplePointLedgerPO.of("s1", "alice", CouplePointLedgerPO.TYPE_EARN, "凑数", 50)));
        assertThat(service.intimacy("alice").level()).isEqualTo(2);

        when(ledgerMapper.findBySpace("s1")).thenReturn(List.of(
                CouplePointLedgerPO.of("s1", "alice", CouplePointLedgerPO.TYPE_EARN, "凑数", 1300)));
        CoupleService.IntimacyVO top = service.intimacy("alice");
        assertThat(top.level()).isEqualTo(7);
        assertThat(top.title()).isEqualTo("相守一生");
        assertThat(top.nextLevelAt()).isNull();
        assertThat(top.levelProgress()).isEqualTo(100);
    }
}
