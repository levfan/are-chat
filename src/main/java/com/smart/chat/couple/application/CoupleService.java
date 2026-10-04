package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.content.CoupleTermBank;
import com.smart.chat.couple.domain.intimacy.IntimacyCalculator;
import com.smart.chat.couple.domain.intimacy.IntimacySource;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.bond.BondAction;
import com.smart.chat.couple.domain.bond.ActionRepository;
import com.smart.chat.couple.domain.anniversary.Anniversary;
import com.smart.chat.couple.domain.anniversary.AnniversaryRepository;
import com.smart.chat.couple.domain.safeword.SafewordUseRepository;
import com.smart.chat.couple.domain.bond.ActionRepository;
import com.smart.chat.couple.domain.bond.ActionRepository;
import com.smart.chat.couple.domain.deed.DeedRepository;
import com.smart.chat.couple.domain.mood.MoodRepository;
import com.smart.chat.couple.domain.quest.QuestOvertimeRepository;
import com.smart.chat.couple.domain.safeword.SafewordUseRepository;
import com.smart.chat.couple.domain.mood.MoodRepository;
import com.smart.chat.couple.domain.quest.QuestOvertimeRepository;
import com.smart.chat.couple.domain.safeword.SafewordUseRepository;
import com.smart.chat.couple.domain.invite.Invite;
import com.smart.chat.couple.domain.invite.InviteRepository;
import com.smart.chat.couple.domain.mood.Mood;
import com.smart.chat.couple.domain.mood.MoodRepository;
import com.smart.chat.couple.domain.points.PointEntry;
import com.smart.chat.couple.domain.points.PointLedgerRepository;
import com.smart.chat.couple.domain.quest.QuestOvertimeRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.sharedkernel.web.BusinessException;
import static com.smart.chat.couple.application.DomainRules.guard;
import static com.smart.chat.couple.application.DomainRules.rule;

import com.smart.chat.messaging.domain.FriendshipChecker;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.messaging.domain.PeerProfileReader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 情侣空间：好友邀请建立 → 双向约定（承诺卡/兑现打卡/逾期提醒）→ 每日小仪式
 * （早安晚安打卡解锁背景贴纸、今日一问、晚安连续天数）→ 共享空间（共同待办清单、共同日历）。
 */
@Service
public class CoupleService {

    // ========== VO ==========

    public record InviteVO(String id, String fromUser, String toUser, String message, String status, Long created) {
        public static InviteVO of(Invite invite) {
            return new InviteVO(invite.id(), invite.fromUser(), invite.toUser(),
                    invite.message() == null ? "" : invite.message(), invite.status(), invite.created());
        }
    }

    public record PartnerVO(String username, String nickname, String avatar, boolean online, String petName) {
    }

    public record SpaceVO(String id, PartnerVO partner, Long created, String anniversary, long days,
                          String slogan, String theme, String stickers) {
    }

    public record OverviewVO(SpaceVO space, List<InviteVO> incoming, List<InviteVO> outgoing,
                             MoodVO todayMine, MoodVO todayPartner) {
    }

    public record AnniversaryVO(String id, String title, String date, boolean yearly, String kind,
                                String createdBy, Long created) {
    }

    public record MoodVO(String id, String username, String moodDay, String mood, String note,
                         Long createdAt, Long updatedAt) {
        public static MoodVO of(Mood row) {
            return new MoodVO(row.id(), row.username(), row.moodDay(), row.mood(),
                    row.note() == null ? "" : row.note(), row.created(), row.updatedAt());
        }
    }

    /** 一天里双方的心情（谁没记录就是 null）。 */
    public record MoodDayVO(String day, MoodVO mine, MoodVO partner) {
    }

    /**
     * 心动值明细：心情条数、贴贴双向往来天数、好事簿条数、留灯次数、安全词复盘次数、
     * 积分台账累计赚分。六项全部来自保留的 10 张卡——早晚安打卡与每日一问下线后，
     * 原来的两个乘数项会永远停在 0，header 数字就再也不动了。
     */
    public record IntimacyBreakdown(long moodDays, long bondDays, long deedCount, long lampCount,
                                    long reflectCount, long pointEarned) {
    }

    public record IntimacyVO(int score, int level, String title, String icon, Integer nextLevelAt,
                             /** 距下一级进度 0-100（满级=100） */
                             int levelProgress, IntimacyBreakdown breakdown) {
    }

    // ========== 依赖 ==========

    private final CoupleSpaceRepository spaceRepository;
    private final InviteRepository inviteRepository;
    private final AnniversaryRepository anniversaryRepository;
    private final MoodRepository moodRepository;
    private final ActionRepository actionRepository;
    private final DeedRepository deedRepository;
    private final QuestOvertimeRepository overtimeRepository;
    private final SafewordUseRepository safewordUseRepository;
    private final PointLedgerRepository ledgerRepository;
    private final FriendshipChecker friendships;
    private final PeerProfileReader profiles;
    private final AccountDirectory accounts;
    private final CoupleEventPublisher push;

    @SuppressWarnings("java:S107")
    public CoupleService(CoupleSpaceRepository spaceRepository,
                         InviteRepository inviteRepository,
                         AnniversaryRepository anniversaryRepository, MoodRepository moodRepository,
                         ActionRepository actionRepository, DeedRepository deedRepository,
                         QuestOvertimeRepository overtimeRepository,
                         SafewordUseRepository safewordUseRepository,
                         PointLedgerRepository ledgerRepository,
                         FriendshipChecker friendships, PeerProfileReader profiles,
                         AccountDirectory accounts, CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.inviteRepository = inviteRepository;
        this.anniversaryRepository = anniversaryRepository;
        this.moodRepository = moodRepository;
        this.actionRepository = actionRepository;
        this.deedRepository = deedRepository;
        this.overtimeRepository = overtimeRepository;
        this.safewordUseRepository = safewordUseRepository;
        this.ledgerRepository = ledgerRepository;
        this.friendships = friendships;
        this.profiles = profiles;
        this.accounts = accounts;
        this.push = push;
    }

    // ========== 邀请建立流程 ==========

    /** A 选择一位好友发起情侣空间邀请。 */
    public InviteVO invite(String me, String target, String message) {
        String raw = target == null ? "" : target.trim();
        if (raw.isEmpty()) {
            throw new BusinessException(400, "想邀请谁？请先选择一位好友");
        }
        String targetName = accounts.normalizeUsername(raw);
        if (targetName.equals(me)) {
            throw new BusinessException(400, "不能和自己建立情侣空间哦");
        }
        if (!accounts.exists(targetName)) {
            throw new BusinessException(400, "查无此人：对方还没注册或已注销");
        }
        if (!friendships.areFriends(me, targetName)) {
            throw new BusinessException(400, "只能邀请自己的好友，先去通讯录加个好友吧");
        }
        if (spaceRepository.findActiveByMember(me).isPresent()) {
            throw new BusinessException(409, "你已经在情侣空间里啦，先解除才能发起新邀请");
        }
        if (spaceRepository.findActiveByMember(targetName).isPresent()) {
            throw new BusinessException(409, "对方已经在别的情侣空间里了");
        }
        if (inviteRepository.findPendingBetween(me, targetName).isPresent()) {
            throw new BusinessException(409, "你们之间已有待处理的情侣邀请，等对方处理吧");
        }
        // 留言的清洗与 100 字上限在 Invite.send 里，违规文案就是用户看到的那句原话
        Invite invite = rule(() -> Invite.send(me, targetName, message));
        inviteRepository.save(invite);
        String note = invite.message() == null ? "" : invite.message();
        String detail = "TA 邀请你开启情侣空间 💕" + (note.isEmpty() ? "" : ("：“" + note + "”"));
        push.pushCoupleEvent("invite", me, targetName, detail);
        return InviteVO.of(invite);
    }

    /** B 同意 → 情侣空间开启。 */
    @Transactional
    public SpaceVO accept(String me, String inviteId) {
        Invite invite = requireInvite(inviteId);
        String from = invite.fromUser();
        guard(() -> invite.acceptBy(me, System.currentTimeMillis()));
        if (spaceRepository.findActiveByMember(me).isPresent() || spaceRepository.findActiveByMember(from).isPresent()) {
            throw new BusinessException(409, "无法同意：有一方已经进入其他情侣空间");
        }
        inviteRepository.save(invite);

        // 建立走聚合工厂：字典序规范化与 ACTIVE 初值只在一处定义（couple.domain.space.CoupleSpace）
        CoupleSpace opened = CoupleSpace.open(from, me, System.currentTimeMillis());
        spaceRepository.save(opened);
        CoupleSpace space = requireSpace(me);
        push.pushCoupleEvent("invite-accepted", me, from, "对方同意啦！你们的情侣空间已开启 🎉");
        return toSpaceVO(space, me);
    }

    /** B 拒绝。 */
    public void reject(String me, String inviteId) {
        Invite invite = requireInvite(inviteId);
        guard(() -> invite.rejectBy(me, System.currentTimeMillis()));
        inviteRepository.save(invite);
        push.pushCoupleEvent("invite-rejected", me, invite.fromUser(), "TA 婉拒了情侣空间邀请，做朋友也很好");
    }

    /** A 撤回自己发出的待处理邀请。 */
    public void cancel(String me, String inviteId) {
        Invite invite = requireInvite(inviteId);
        guard(() -> invite.cancelBy(me, System.currentTimeMillis()));
        inviteRepository.save(invite);
    }

    /** 解除情侣空间：双方历史数据保留，但不再互相可见，各自可发起新邀请。 */
    public void dissolve(String me) {
        CoupleSpace space = requireSpace(me);
        space.dissolve(System.currentTimeMillis());
        spaceRepository.save(space);
        push.pushCoupleEvent("dissolved", me, space.partnerOf(me), "对方解除了情侣空间 😢");
    }

    // ========== 总览 ==========

    public OverviewVO overview(String me) {
        // 待处理邀请返回全部（可能同时收到多人邀请），按时间新→旧
        List<InviteVO> incoming = inviteRepository.findPendingTo(me).stream().map(InviteVO::of).toList();
        List<InviteVO> outgoing = inviteRepository.findPendingFrom(me).stream().map(InviteVO::of).toList();
        CoupleSpace space = spaceRepository.findActiveByMember(me).orElse(null);
        if (space == null) {
            return new OverviewVO(null, incoming, outgoing, null, null);
        }
        // 首页只带今天双方的心情：它是「TA 今天怎么样」唯一的即时信号
        String day = today();
        MoodVO mine = moodRepository.findBySpaceAndUserOn(space.id(), me, day).map(MoodVO::of).orElse(null);
        MoodVO partner = moodRepository.findBySpaceAndUserOn(space.id(), space.partnerOf(me), day).map(MoodVO::of).orElse(null);
        return new OverviewVO(toSpaceVO(space, me), incoming, outgoing, mine, partner);
    }

    public SpaceVO setAnniversary(String me, String date) {
        CoupleSpace space = requireSpace(me);
        String normalized = normalizeDate(date, "纪念日格式应为 yyyy-MM-dd");
        space.bindAnniversary(normalized);
        spaceRepository.save(space);
        push.pushCoupleEvent("anniversary-updated", me, space.partnerOf(me), "TA 更新了你们「在一起」的日子 📅");
        return toSpaceVO(space, me);
    }

    /** F44 恋爱中徽章：某人是否在恋爱中（只有其好友可查）。 */
    public record RelationshipVO(boolean inRelationship, Long days, String anniversary) {
    }

    public RelationshipVO relationshipOf(String me, String target) {
        String name = target == null ? "" : target.trim();
        if (name.isEmpty()) {
            throw new BusinessException(400, "用户名不能为空");
        }
        // 只有对方好友可以查看（保护隐私）
        if (!friendships.areFriends(me, name)) {
            throw new BusinessException(403, "只有好友才能查看恋爱状态");
        }
        return spaceRepository.findActiveByMember(name)
                .<RelationshipVO>map(space -> new RelationshipVO(true, daysTogether(space), space.anniversary()))
                .orElse(new RelationshipVO(false, null, null));
    }

    // ========== 空间个性化：宣言 / 主题 / 贴纸墙（F26 / F27 / F28） ==========

    /**
     * 更新空间个性化：宣言（可空，≤60 字）、主题（白名单）、贴纸墙佩戴（≤6 枚 key，逗号分隔）。
     * 任一传 null 表示该项不修改；全部字段校验后一次性保存，双方推送 space-themed。
     */
    public SpaceVO updateProfile(String me, String slogan, String theme, String stickers) {
        CoupleSpace space = requireSpace(me);
        DomainRules.guard(() -> space.decorate(slogan, theme, stickers));
        spaceRepository.save(space);
        push.pushCoupleEvent("space-themed", me, space.partnerOf(me), "TA 打扮了你们的小空间 ✨ 快去看看");
        return toSpaceVO(space, me);
    }

    // ========== 1. 双向待办 / 约定（承诺卡） ==========

    // ========== 2. 每日小仪式 ==========

    // ========== F48 一问互评 ==========

    // ========== 3. 共享空间 ==========

    public List<AnniversaryVO> listAnniversaries(String me) {
        CoupleSpace space = requireSpace(me);
        return anniversaryRepository.findBySpace(space.id()).stream().map(CoupleService::toAnniversaryVO).toList();
    }

    public AnniversaryVO createAnniversary(String me, String title, String date, Boolean yearly, String kind) {
        return createAnniversary(me, title, date, yearly, kind, null, null);
    }

    /** F253 支持农历：calendarType=LUNAR 时 lunarMd（MMDD）为真源，eventDate 存首次换算出的公历日（date 可缺省）。 */
    public AnniversaryVO createAnniversary(String me, String title, String date, Boolean yearly, String kind,
                                           String calendarType, String lunarMd) {
        CoupleSpace space = requireSpace(me);
        boolean lunar = Anniversary.CALENDAR_LUNAR.equals(calendarType);
        LocalDate firstSolar = null;
        if (lunar) {
            String md = lunarMd == null ? "" : lunarMd.trim();
            if (!md.matches("(0[1-9]|1[0-2])(0[1-9]|[12]\\d|30)")) {
                throw new BusinessException(400, "农历月日格式应为 MMDD（如腊月初八写 1208）");
            }
            firstSolar = CoupleTermBank.lunarToSolar(LocalDate.now().getYear(),
                    Integer.parseInt(md.substring(0, 2)), Integer.parseInt(md.substring(2)), false);
            if (firstSolar == null) {
                throw new BusinessException(400, "这个农历日子换算不了，检查下月日");
            }
        }
        String normalized = (date == null || date.isBlank()) && firstSolar != null
                ? firstSolar.toString() : normalizeDate(date, "日期格式应为 yyyy-MM-dd");
        Anniversary row = rule(() -> Anniversary.schedule(space.id(), title, normalized,
                yearly == null || yearly, me, kind, lunar, lunarMd));
        anniversaryRepository.save(row);
        push.pushCoupleEvent("anniversaries-changed", me, space.partnerOf(me), "共同日历有更新 📅：" + row.title());
        return toAnniversaryVO(row);
    }

    /** F127 大日子类型校验，空或非法时回退 NORMAL。 */
    private String normalizeAnniversaryKind(String kind) {
        if (kind == null || kind.isBlank()) {
            return Anniversary.KIND_NORMAL;
        }
        return switch (kind) {
            case Anniversary.KIND_LOVE, Anniversary.KIND_FAMILY,
                 Anniversary.KIND_FRIEND, Anniversary.KIND_WORK -> kind;
            default -> Anniversary.KIND_NORMAL;
        };
    }

    public void deleteAnniversary(String me, String anniversaryId) {
        CoupleSpace space = requireSpace(me);
        Anniversary row = anniversaryRepository.findById(anniversaryId)
                .filter(a -> a.spaceId().equals(space.id()))
                .orElseThrow(() -> new BusinessException(404, "纪念日不存在"));
        anniversaryRepository.deleteById(row.id());
        push.pushCoupleEvent("anniversaries-changed", me, space.partnerOf(me),
                "共同日历删除了一项：" + row.title());
    }

    // ========== 4. 心情日记 ==========

    /**
     * 记录/修改今天的心情：每人每天一条，重复提交视为修改（key: 空间+人+自然日）。
     * mood 为 8 个固定键之一，note 为一句话心情（可空）。
     */
    public MoodVO saveMood(String me, String mood, String note) {
        CoupleSpace space = requireSpace(me);
        String text = requireOptional(note, "一句话心情最多 200 字", Mood.NOTE_MAX);
        String day = today();
        // 心情键白名单与「每人每天一条」的改写规则在 Mood 里，这里只决定是改写还是新建
        Mood today = moodRepository.findBySpaceAndUserOn(space.id(), me, day).orElse(null);
        if (today == null) {
            today = rule(() -> Mood.record(space.id(), me, day, mood, text));
        } else {
            Mood written = today;
            guard(() -> written.rewrite(mood, text));
        }
        moodRepository.save(today);
        Mood row = today;
        push.pushCoupleEvent("mood-changed", me, space.partnerOf(me),
                "TA 记录了今天的心情 " + Mood.emojiOf(row.mood()) + "，快去看看吧");
        return MoodVO.of(row);
    }

    /** 双方最近 N 天（1-90，默认 14）的心情，按日期新→旧，只返回至少有一方记录的日子。 */
    public List<MoodDayVO> listMoods(String me, int days) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        int limit = clampDays(days, 14);
        String startDay = LocalDate.now().minusDays(limit - 1L).toString();
        Map<String, Mood> mine = new HashMap<>();
        Map<String, Mood> theirs = new HashMap<>();
        for (Mood row : moodRepository.listBySpace(space.id())) {
            if (row.moodDay().compareTo(startDay) < 0) {
                continue;
            }
            if (row.username().equals(me)) {
                mine.put(row.moodDay(), row);
            } else if (row.username().equals(partner)) {
                theirs.put(row.moodDay(), row);
            }
        }
        List<MoodDayVO> result = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            String day = LocalDate.now().minusDays(i).toString();
            MoodVO a = mine.containsKey(day) ? MoodVO.of(mine.get(day)) : null;
            MoodVO b = theirs.containsKey(day) ? MoodVO.of(theirs.get(day)) : null;
            if (a != null || b != null) {
                result.add(new MoodDayVO(day, a, b));
            }
        }
        return result;
    }

    // ========== 6. 心动值 & 恋爱等级 ==========

    /**
     * 心动值 = 心情条数×1 + 贴贴双向往来天数×2 + 好事簿条数×2 + 留灯次数×3
     *          + 安全词复盘次数×2 + 积分台账累计赚分×1；累计分数映射恋爱等级。
     * 六项全部由保留的 10 张卡供数。早晚安打卡与每日一问下线后，原来那三个乘数项
     * 会永远停在 0，header 数字就不再增长，所以整体换成活的数据源。
     */
    public IntimacyVO intimacy(String me) {
        CoupleSpace space = requireSpace(me);
        long moodDays = moodRepository.listBySpace(space.id()).size();
        long bondDays = bondBothDays(space);
        long deedCount = deedRepository.findBySpace(space.id()).size();
        long lampCount = overtimeRepository.listBySpace(space.id()).stream()
                .filter(o -> o.lampBy() != null && !o.lampBy().isBlank())
                .count();
        long reflectCount = safewordUseRepository.listBySpace(space.id()).stream()
                .filter(u -> u.reflect() != null && !u.reflect().isBlank())
                .count();
        long pointEarned = ledgerRepository.findBySpace(space.id()).stream()
                .filter(PointEntry::earned)
                .mapToLong(PointEntry::points).sum();

        // 取数只负责「攒了多少」，加权与定级交回领域层（couple.domain.intimacy）
        IntimacyCalculator.Intimacy result = IntimacyCalculator.evaluate(
                new IntimacySource(moodDays, bondDays, deedCount, lampCount, reflectCount, pointEarned));
        IntimacyCalculator.IntimacyLevel level = result.level();
        IntimacyBreakdown breakdown = new IntimacyBreakdown(moodDays, bondDays, deedCount, lampCount,
                reflectCount, pointEarned);
        return new IntimacyVO(result.score(), level.level(), level.title(), level.icon(),
                level.nextLevelAt(), level.progress(), breakdown);
    }

    // ========== 7. 悄悄话信箱 ==========

    // ========== 8. 今日一问历史回顾 ==========

    // ========== 9. 恋爱条约 ==========

    // ========== 10. 异地恋助手 ==========

    // ========== 11. 心愿基金 ==========

    // ========== 84 注销清理（AppUserService.deactivate 调用） ==========

    /** 注销：解散所在空间并清理相关邀请。 */
    @Transactional
    public void purgeUser(String username) {
        CoupleSpace space = spaceRepository.findActiveByMember(username).orElse(null);
        if (space != null) {
            space.dissolve(System.currentTimeMillis());
            spaceRepository.save(space);
            push.pushCoupleEvent("dissolved", username, space.partnerOf(username), "对方账号已注销，情侣空间自动解除 😢");
        }
        inviteRepository.deleteAllInvolving(username);
    }

    // ========== 内部工具 ==========

    /** 贴贴双向往来的天数：同一天里两个人都发过动作才算一天（单向不计）。 */
    private long bondBothDays(CoupleSpace space) {
        Map<String, Set<String>> byDay = new HashMap<>();
        for (BondAction action : actionRepository.listBySpace(space.id())) {
            if (action.created() == null) {
                continue;
            }
            String day = java.time.Instant.ofEpochMilli(action.created())
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
            byDay.computeIfAbsent(day, k -> new HashSet<>()).add(action.username());
        }
        return byDay.values().stream().filter(users -> users.size() >= 2).count();
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    /** 日期范围钳制：1-90，给默认值。 */
    private int clampDays(int days, int def) {
        if (days <= 0) {
            return def;
        }
        return Math.min(days, 90);
    }

    private Invite requireInvite(String inviteId) {
        return inviteRepository.findById(inviteId).orElseThrow(() -> new BusinessException(404, "邀请不存在"));
    }

    private SpaceVO toSpaceVO(CoupleSpace space, String me) {
        String partner = space.partnerOf(me);
        PeerProfileReader.PeerProfile profile = profiles.read(partner).orElse(null);
        String nickname = profile == null || profile.nickname() == null || profile.nickname().isBlank()
                ? partner : profile.nickname();
        String avatar = profile == null || profile.avatar() == null ? "" : profile.avatar();
        PartnerVO partnerVO = new PartnerVO(partner, nickname, avatar, push.isOnline(partner), space.nickOf(partner));
        return new SpaceVO(space.id(), partnerVO, space.created(), space.anniversary(),
                daysTogether(space), space.slogan(),
                space.theme() == null ? "classic" : space.theme(), space.stickers());
    }

    /** 在一起天数：从纪念日（缺省取建立日）算到今天，含当天（建立当天 = 第 1 天）。 */
    private long daysTogether(CoupleSpace space) {
        LocalDate start;
        try {
            start = LocalDate.parse(space.anniversary());
        } catch (Exception e) {
            start = Instant.ofEpochMilli(space.created()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        long days = ChronoUnit.DAYS.between(start, LocalDate.now()) + 1;
        return Math.max(days, 1);
    }

    private static AnniversaryVO toAnniversaryVO(Anniversary row) {
        return new AnniversaryVO(row.id(), row.title(), row.eventDate(), row.repeatsYearly(),
                row.kind() == null ? Anniversary.KIND_NORMAL : row.kind(),
                row.createdBy(), row.created());
    }

    private String requireText(String value, String message, int max) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.length() > max) {
            throw new BusinessException(400, message);
        }
        return text;
    }

    private String requireOptional(String value, String message, int max) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.length() > max) {
            throw new BusinessException(400, message);
        }
        return text;
    }

    private String normalizeDateOrNull(String value, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalizeDate(value, message);
    }

    private String normalizeDate(String value, String message) {
        try {
            return LocalDate.parse(value.trim()).toString();
        } catch (Exception e) {
            throw new BusinessException(400, message);
        }
    }

    private String today() {
        return LocalDate.now().toString();
    }
}
