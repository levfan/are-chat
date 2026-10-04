package com.smart.chat.couple.application;


import com.smart.chat.couple.domain.bond.ActionRepository;
import com.smart.chat.couple.domain.bond.BondAction;
import com.smart.chat.couple.domain.anniversary.AnniversaryRepository;

import com.smart.chat.couple.domain.safeword.SafewordUse;
import com.smart.chat.couple.domain.safeword.SafewordUseRepository;
import com.smart.chat.couple.domain.deed.Deed;
import com.smart.chat.couple.domain.deed.DeedRepository;
import com.smart.chat.couple.domain.invite.InviteRepository;

import com.smart.chat.couple.domain.mood.Mood;
import com.smart.chat.couple.domain.mood.MoodRepository;
import com.smart.chat.couple.domain.points.PointEntry;
import com.smart.chat.couple.domain.points.PointLedgerRepository;

import com.smart.chat.couple.domain.quest.QuestOvertime;
import com.smart.chat.couple.domain.quest.QuestOvertimeRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
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
    private CoupleSpaceRepository spaceRepository;
    @Mock
    private InviteRepository inviteRepository;
    @Mock
    private AnniversaryRepository anniversaryRepository;
    @Mock
    private MoodRepository moodRepository;
    @Mock
    private ActionRepository actionRepository;
    @Mock
    private DeedRepository deedRepository;
    @Mock
    private QuestOvertimeRepository overtimeRepository;
    @Mock
    private SafewordUseRepository safewordUseRepository;
    @Mock
    private PointLedgerRepository ledgerRepository;
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
        CoupleSpace space = CoupleSpace.restore("s1", "alice", "bob", CoupleSpace.STATUS_ACTIVE, 0L, null, null, null, null, null, null, null);
        lenient().when(spaceRepository.findActiveByMember(any())).thenReturn(Optional.of(space));
        lenient().when(moodRepository.listBySpace("s1")).thenReturn(List.of());
        lenient().when(actionRepository.listBySpace("s1")).thenReturn(List.of());
        lenient().when(deedRepository.findBySpace("s1")).thenReturn(List.of());
        lenient().when(overtimeRepository.listBySpace("s1")).thenReturn(List.of());
        lenient().when(safewordUseRepository.listBySpace("s1")).thenReturn(List.of());
        lenient().when(ledgerRepository.findBySpace("s1")).thenReturn(List.of());
        lenient().when(anniversaryRepository.findBySpace("s1")).thenReturn(List.of());
    }

    /** 毫秒时间戳按系统时区落到某天，用于造「同一天双方都贴过」的数据。 */
    private long atOffsetDays(int back) {
        return LocalDate.now().minusDays(back)
                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() + 3_600_000L;
    }

    @Test
    void scoreWeightsComeOnlyFromRetainedCards() {
        // 心情 3 条 ×1
        List<Mood> moods = new ArrayList<>();
        moods.add(Mood.restore("m1", "s1", "alice", DAY, "HAPPY", null, System.currentTimeMillis(), null));
        moods.add(Mood.restore("m2", "s1", "alice", LocalDate.now().minusDays(1).toString(), "CALM", null,
                System.currentTimeMillis(), null));
        moods.add(Mood.restore("m3", "s1", "bob", DAY, "SAD", null, System.currentTimeMillis(), null));
        when(moodRepository.listBySpace("s1")).thenReturn(moods);

        // 贴贴双向往来 1 天 ×2：今天两人都贴了，昨天只有 alice 贴（单向不计）
        List<BondAction> actions = new ArrayList<>();
        actions.add(BondAction.restore("a1", "s1", "alice", "HUG", atOffsetDays(0)));
        actions.add(BondAction.restore("a2", "s1", "bob", "POKE", atOffsetDays(0)));
        actions.add(BondAction.restore("a3", "s1", "alice", "KISS", atOffsetDays(3)));
        when(actionRepository.listBySpace("s1")).thenReturn(actions);

        // 好事簿 2 条 ×2
        when(deedRepository.findBySpace("s1")).thenReturn(List.of(
                Deed.restore("d1", "s1", "alice", "接我下班", DAY, 0, System.currentTimeMillis(), null),
                Deed.restore("d2", "s1", "bob", "帮我吹头", DAY, 0, System.currentTimeMillis(), null)));

        // 留灯 1 次 ×3（另一行只预报了加班没留灯，不算）
        QuestOvertime lit = QuestOvertime.restore("q1", "s1", DAY, "alice", 22, "赶年结", "灯给你留着", "bob",
                System.currentTimeMillis(), null);
        when(overtimeRepository.listBySpace("s1")).thenReturn(List.of(lit,
                QuestOvertime.restore("q2", "s1", LocalDate.now().minusDays(1).toString(), "bob", 20, "", null, null,
                        System.currentTimeMillis(), null)));

        // 安全词复盘 1 次 ×2（另一次没补复盘，不算）
        SafewordUse reflected = SafewordUse.restore("u1", DAY, "alice", "当时是怕被丢下");
        when(safewordUseRepository.listBySpace("s1")).thenReturn(List.of(reflected,
                SafewordUse.restore("u2", LocalDate.now().minusDays(1).toString(), "bob", null)));

        // 台账赚分 ×1：SPEND 不参与进账
        when(ledgerRepository.findBySpace("s1")).thenReturn(List.of(
                PointEntry.earn("s1", "alice", "好事簿：接我下班", 9),
                PointEntry.spend("s1", "alice", "发出愿望券：看一次海", 10)));

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
        List<BondAction> actions = new ArrayList<>();
        actions.add(BondAction.restore("a1", "s1", "alice", "HUG", atOffsetDays(0)));
        actions.add(BondAction.restore("a2", "s1", "alice", "POKE", atOffsetDays(1)));
        when(actionRepository.listBySpace("s1")).thenReturn(actions);

        CoupleService.IntimacyVO vo = service.intimacy("alice");

        // 两天都只有 alice 贴：双向天数必须是 0，否则心动值会靠一个人刷满
        assertThat(vo.breakdown().bondDays()).isZero();
        assertThat(vo.score()).isZero();
    }

    @Test
    void levelLadderKeepsThePreTrimThresholds() {
        when(ledgerRepository.findBySpace("s1")).thenReturn(List.of(
                PointEntry.earn("s1", "alice", "凑数", 50)));
        assertThat(service.intimacy("alice").level()).isEqualTo(2);

        when(ledgerRepository.findBySpace("s1")).thenReturn(List.of(
                PointEntry.earn("s1", "alice", "凑数", 1300)));
        CoupleService.IntimacyVO top = service.intimacy("alice");
        assertThat(top.level()).isEqualTo(7);
        assertThat(top.title()).isEqualTo("相守一生");
        assertThat(top.nextLevelAt()).isNull();
        assertThat(top.levelProgress()).isEqualTo(100);
    }
}
