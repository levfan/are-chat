package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.content.CoupleTermBank;
import com.smart.chat.couple.domain.intimacy.IntimacyCalculator;
import com.smart.chat.couple.domain.intimacy.IntimacySource;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.infrastructure.persistence.CoupleActionPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleActionMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleAnniversaryPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleAnniversaryMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleCatchSafewordUseMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleEchoDeedMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleInvitePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleInviteMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleMoodPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleMoodMapper;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerPO;
import com.smart.chat.couple.infrastructure.persistence.CouplePointLedgerMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleQuestOvertimeMapper;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpacePO;
import com.smart.chat.couple.infrastructure.persistence.CoupleSpaceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.sharedkernel.web.BusinessException;
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
        public static InviteVO of(CoupleInvitePO invite) {
            return new InviteVO(invite.getId(), invite.getFromUser(), invite.getToUser(),
                    invite.getMessage() == null ? "" : invite.getMessage(), invite.getStatus(), invite.getCreated());
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
        public static MoodVO of(CoupleMoodPO row) {
            return new MoodVO(row.getId(), row.getUsername(), row.getMoodDay(), row.getMood(),
                    row.getNote() == null ? "" : row.getNote(), row.getCreated(), row.getUpdatedAt());
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

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleSpaceRepository spaceRepository;
    private final CoupleInviteMapper inviteMapper;
    private final CoupleAnniversaryMapper anniversaryMapper;
    private final CoupleMoodMapper moodMapper;
    private final CoupleActionMapper actionMapper;
    private final CoupleEchoDeedMapper deedMapper;
    private final CoupleQuestOvertimeMapper overtimeMapper;
    private final CoupleCatchSafewordUseMapper safewordUseMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final FriendshipChecker friendships;
    private final PeerProfileReader profiles;
    private final AccountDirectory accounts;
    private final CoupleEventPublisher push;

    @SuppressWarnings("java:S107")
    public CoupleService(CoupleSpaceMapper spaceMapper, CoupleSpaceRepository spaceRepository,
                         CoupleInviteMapper inviteMapper,
                         CoupleAnniversaryMapper anniversaryMapper, CoupleMoodMapper moodMapper,
                         CoupleActionMapper actionMapper, CoupleEchoDeedMapper deedMapper,
                         CoupleQuestOvertimeMapper overtimeMapper,
                         CoupleCatchSafewordUseMapper safewordUseMapper,
                         CouplePointLedgerMapper ledgerMapper,
                         FriendshipChecker friendships, PeerProfileReader profiles,
                         AccountDirectory accounts, CoupleEventPublisher push) {
        this.spaceMapper = spaceMapper;
        this.spaceRepository = spaceRepository;
        this.inviteMapper = inviteMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.moodMapper = moodMapper;
        this.actionMapper = actionMapper;
        this.deedMapper = deedMapper;
        this.overtimeMapper = overtimeMapper;
        this.safewordUseMapper = safewordUseMapper;
        this.ledgerMapper = ledgerMapper;
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
        if (spaceMapper.findActiveByUser(me).isPresent()) {
            throw new BusinessException(409, "你已经在情侣空间里啦，先解除才能发起新邀请");
        }
        if (spaceMapper.findActiveByUser(targetName).isPresent()) {
            throw new BusinessException(409, "对方已经在别的情侣空间里了");
        }
        if (inviteMapper.findPendingBetween(me, targetName).isPresent()) {
            throw new BusinessException(409, "你们之间已有待处理的情侣邀请，等对方处理吧");
        }
        String note = message == null ? "" : message.trim();
        if (note.length() > 100) {
            throw new BusinessException(400, "邀请留言最长 100 个字");
        }
        CoupleInvitePO invite = CoupleInvitePO.of(me, targetName, note.isEmpty() ? null : note);
        inviteMapper.insert(invite);
        String detail = "TA 邀请你开启情侣空间 💕" + (note.isEmpty() ? "" : ("：“" + note + "”"));
        push.pushCoupleEvent("invite", me, targetName, detail);
        return InviteVO.of(invite);
    }

    /** B 同意 → 情侣空间开启。 */
    @Transactional
    public SpaceVO accept(String me, String inviteId) {
        CoupleInvitePO invite = requireInvite(inviteId);
        if (!invite.getToUser().equals(me)) {
            throw new BusinessException(403, "只能处理发给自己的邀请");
        }
        if (!CoupleInvitePO.STATUS_PENDING.equals(invite.getStatus())) {
            throw new BusinessException(409, "该邀请已经处理过了");
        }
        String from = invite.getFromUser();
        if (spaceMapper.findActiveByUser(me).isPresent() || spaceMapper.findActiveByUser(from).isPresent()) {
            throw new BusinessException(409, "无法同意：有一方已经进入其他情侣空间");
        }
        invite.setStatus(CoupleInvitePO.STATUS_ACCEPTED);
        invite.setUpdatedAt(System.currentTimeMillis());
        inviteMapper.updateById(invite);

        String[] pair = CoupleSpacePO.ordered(from, me);
        // 建立走聚合工厂：字典序规范化与 ACTIVE 初值只在一处定义（couple.domain.space.CoupleSpace）
        CoupleSpace opened = CoupleSpace.open(pair[0], pair[1], System.currentTimeMillis());
        spaceRepository.save(opened);
        CoupleSpacePO space = requireSpace(me);
        push.pushCoupleEvent("invite-accepted", me, from, "对方同意啦！你们的情侣空间已开启 🎉");
        return toSpaceVO(space, me);
    }

    /** B 拒绝。 */
    public void reject(String me, String inviteId) {
        CoupleInvitePO invite = requireInvite(inviteId);
        if (!invite.getToUser().equals(me)) {
            throw new BusinessException(403, "只能处理发给自己的邀请");
        }
        if (!CoupleInvitePO.STATUS_PENDING.equals(invite.getStatus())) {
            throw new BusinessException(409, "该邀请已经处理过了");
        }
        invite.setStatus(CoupleInvitePO.STATUS_REJECTED);
        invite.setUpdatedAt(System.currentTimeMillis());
        inviteMapper.updateById(invite);
        push.pushCoupleEvent("invite-rejected", me, invite.getFromUser(), "TA 婉拒了情侣空间邀请，做朋友也很好");
    }

    /** A 撤回自己发出的待处理邀请。 */
    public void cancel(String me, String inviteId) {
        CoupleInvitePO invite = requireInvite(inviteId);
        if (!invite.getFromUser().equals(me)) {
            throw new BusinessException(403, "只能撤回自己发出的邀请");
        }
        if (!CoupleInvitePO.STATUS_PENDING.equals(invite.getStatus())) {
            throw new BusinessException(409, "该邀请已经处理过了");
        }
        invite.setStatus(CoupleInvitePO.STATUS_CANCELED);
        invite.setUpdatedAt(System.currentTimeMillis());
        inviteMapper.updateById(invite);
    }

    /** 解除情侣空间：双方历史数据保留，但不再互相可见，各自可发起新邀请。 */
    public void dissolve(String me) {
        CoupleSpacePO space = requireSpace(me);
        space.setStatus(CoupleSpacePO.STATUS_DISSOLVED);
        space.setDissolvedAt(System.currentTimeMillis());
        spaceMapper.updateById(space);
        push.pushCoupleEvent("dissolved", me, space.partnerOf(me), "对方解除了情侣空间 😢");
    }

    // ========== 总览 ==========

    public OverviewVO overview(String me) {
        // 待处理邀请返回全部（可能同时收到多人邀请），按时间新→旧
        List<InviteVO> incoming = inviteMapper.findPendingTo(me).stream().map(InviteVO::of).toList();
        List<InviteVO> outgoing = inviteMapper.findPendingFrom(me).stream().map(InviteVO::of).toList();
        CoupleSpacePO space = spaceMapper.findActiveByUser(me).orElse(null);
        if (space == null) {
            return new OverviewVO(null, incoming, outgoing, null, null);
        }
        // 首页只带今天双方的心情：它是「TA 今天怎么样」唯一的即时信号
        String day = today();
        MoodVO mine = moodMapper.find(space.getId(), me, day).map(MoodVO::of).orElse(null);
        MoodVO partner = moodMapper.find(space.getId(), space.partnerOf(me), day).map(MoodVO::of).orElse(null);
        return new OverviewVO(toSpaceVO(space, me), incoming, outgoing, mine, partner);
    }

    public SpaceVO setAnniversary(String me, String date) {
        CoupleSpacePO space = requireSpace(me);
        String normalized = normalizeDate(date, "纪念日格式应为 yyyy-MM-dd");
        space.setAnniversary(normalized);
        spaceMapper.updateById(space);
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
        return spaceMapper.findActiveByUser(name)
                .<RelationshipVO>map(space -> new RelationshipVO(true, daysTogether(space), space.getAnniversary()))
                .orElse(new RelationshipVO(false, null, null));
    }

    // ========== 空间个性化：宣言 / 主题 / 贴纸墙（F26 / F27 / F28） ==========

    /**
     * 更新空间个性化：宣言（可空，≤60 字）、主题（白名单）、贴纸墙佩戴（≤6 枚 key，逗号分隔）。
     * 任一传 null 表示该项不修改；全部字段校验后一次性保存，双方推送 space-themed。
     */
    public SpaceVO updateProfile(String me, String slogan, String theme, String stickers) {
        CoupleSpacePO space = requireSpace(me);
        if (slogan != null) {
            String text = slogan.trim();
            if (text.length() > 60) {
                throw new BusinessException(400, "宣言最多 60 字，留白也很美");
            }
            space.setSlogan(text.isEmpty() ? null : text);
        }
        if (theme != null) {
            if (!CoupleSpacePO.THEMES.contains(theme)) {
                throw new BusinessException(400, "这个主题还没上架哦");
            }
            space.setTheme(theme);
        }
        if (stickers != null) {
            String normalized = stickers.trim();
            if (!normalized.isEmpty()) {
                String[] keys = normalized.split(",");
                if (keys.length > CoupleSpacePO.STICKER_MAX) {
                    throw new BusinessException(400, "贴纸墙最多佩戴 " + CoupleSpacePO.STICKER_MAX + " 枚");
                }
                for (String key : keys) {
                    if (key.isBlank() || key.length() > 30) {
                        throw new BusinessException(400, "贴纸选择有误，刷新后再试试");
                    }
                }
            }
            space.setStickers(normalized.isEmpty() ? null : normalized);
        }
        spaceMapper.updateById(space);
        push.pushCoupleEvent("space-themed", me, space.partnerOf(me), "TA 打扮了你们的小空间 ✨ 快去看看");
        return toSpaceVO(space, me);
    }

    // ========== 1. 双向待办 / 约定（承诺卡） ==========

    // ========== 2. 每日小仪式 ==========

    // ========== F48 一问互评 ==========

    // ========== 3. 共享空间 ==========

    public List<AnniversaryVO> listAnniversaries(String me) {
        CoupleSpacePO space = requireSpace(me);
        return anniversaryMapper.findBySpace(space.getId()).stream().map(CoupleService::toAnniversaryVO).toList();
    }

    public AnniversaryVO createAnniversary(String me, String title, String date, Boolean yearly, String kind) {
        return createAnniversary(me, title, date, yearly, kind, null, null);
    }

    /** F253 支持农历：calendarType=LUNAR 时 lunarMd（MMDD）为真源，eventDate 存首次换算出的公历日（date 可缺省）。 */
    public AnniversaryVO createAnniversary(String me, String title, String date, Boolean yearly, String kind,
                                           String calendarType, String lunarMd) {
        CoupleSpacePO space = requireSpace(me);
        boolean lunar = CoupleAnniversaryPO.CALENDAR_LUNAR.equals(calendarType);
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
        CoupleAnniversaryPO row = CoupleAnniversaryPO.of(space.getId(),
                requireText(title, "纪念日名称不能为空（最多 60 字）", CoupleAnniversaryPO.TITLE_MAX),
                normalized, yearly == null || yearly, me);
        row.setKind(normalizeAnniversaryKind(kind));
        if (lunar) {
            row.setCalendarType(CoupleAnniversaryPO.CALENDAR_LUNAR);
            row.setLunarMd(lunarMd.trim());
        }
        anniversaryMapper.insert(row);
        push.pushCoupleEvent("anniversaries-changed", me, space.partnerOf(me), "共同日历有更新 📅：" + row.getTitle());
        return toAnniversaryVO(row);
    }

    /** F127 大日子类型校验，空或非法时回退 NORMAL。 */
    private String normalizeAnniversaryKind(String kind) {
        if (kind == null || kind.isBlank()) {
            return CoupleAnniversaryPO.KIND_NORMAL;
        }
        return switch (kind) {
            case CoupleAnniversaryPO.KIND_LOVE, CoupleAnniversaryPO.KIND_FAMILY,
                 CoupleAnniversaryPO.KIND_FRIEND, CoupleAnniversaryPO.KIND_WORK -> kind;
            default -> CoupleAnniversaryPO.KIND_NORMAL;
        };
    }

    public void deleteAnniversary(String me, String anniversaryId) {
        CoupleSpacePO space = requireSpace(me);
        CoupleAnniversaryPO row = anniversaryMapper.selectById(anniversaryId);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "纪念日不存在");
        }
        anniversaryMapper.deleteById(row.getId());
        push.pushCoupleEvent("anniversaries-changed", me, space.partnerOf(me),
                "共同日历删除了一项：" + row.getTitle());
    }

    // ========== 4. 心情日记 ==========

    /**
     * 记录/修改今天的心情：每人每天一条，重复提交视为修改（key: 空间+人+自然日）。
     * mood 为 8 个固定键之一，note 为一句话心情（可空）。
     */
    public MoodVO saveMood(String me, String mood, String note) {
        CoupleSpacePO space = requireSpace(me);
        if (mood == null || !CoupleMoodPO.MOOD_KEYS.contains(mood)) {
            throw new BusinessException(400, "心情不在可选范围内哦");
        }
        String text = requireOptional(note, "一句话心情最多 200 字", CoupleMoodPO.NOTE_MAX);
        String day = today();
        CoupleMoodPO row = moodMapper.find(space.getId(), me, day).orElse(null);
        if (row != null) {
            row.setMood(mood);
            row.setNote(text);
            row.setUpdatedAt(System.currentTimeMillis());
            moodMapper.updateById(row);
        } else {
            row = CoupleMoodPO.of(space.getId(), me, day, mood, text);
            moodMapper.insert(row);
        }
        push.pushCoupleEvent("mood-changed", me, space.partnerOf(me),
                "TA 记录了今天的心情 " + CoupleMoodPO.emojiOf(mood) + "，快去看看吧");
        return MoodVO.of(row);
    }

    /** 双方最近 N 天（1-90，默认 14）的心情，按日期新→旧，只返回至少有一方记录的日子。 */
    public List<MoodDayVO> listMoods(String me, int days) {
        CoupleSpacePO space = requireSpace(me);
        String partner = space.partnerOf(me);
        int limit = clampDays(days, 14);
        String startDay = LocalDate.now().minusDays(limit - 1L).toString();
        Map<String, CoupleMoodPO> mine = new HashMap<>();
        Map<String, CoupleMoodPO> theirs = new HashMap<>();
        for (CoupleMoodPO row : moodMapper.findBySpace(space.getId())) {
            if (row.getMoodDay().compareTo(startDay) < 0) {
                continue;
            }
            if (row.getUsername().equals(me)) {
                mine.put(row.getMoodDay(), row);
            } else if (row.getUsername().equals(partner)) {
                theirs.put(row.getMoodDay(), row);
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
        CoupleSpacePO space = requireSpace(me);
        long moodDays = moodMapper.findBySpace(space.getId()).size();
        long bondDays = bondBothDays(space);
        long deedCount = deedMapper.findBySpace(space.getId()).size();
        long lampCount = overtimeMapper.findBySpace(space.getId()).stream()
                .filter(o -> o.getLampBy() != null && !o.getLampBy().isBlank())
                .count();
        long reflectCount = safewordUseMapper.findBySpace(space.getId()).stream()
                .filter(u -> u.getReflect() != null && !u.getReflect().isBlank())
                .count();
        long pointEarned = ledgerMapper.findBySpace(space.getId()).stream()
                .filter(l -> CouplePointLedgerPO.TYPE_EARN.equals(l.getType()))
                .mapToLong(l -> l.getPoints() == null ? 0 : l.getPoints()).sum();

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
        CoupleSpacePO space = spaceMapper.findActiveByUser(username).orElse(null);
        if (space != null) {
            space.setStatus(CoupleSpacePO.STATUS_DISSOLVED);
            space.setDissolvedAt(System.currentTimeMillis());
            spaceMapper.updateById(space);
            push.pushCoupleEvent("dissolved", username, space.partnerOf(username), "对方账号已注销，情侣空间自动解除 😢");
        }
        inviteMapper.deleteAllInvolving(username);
    }

    // ========== 内部工具 ==========

    /** 贴贴双向往来的天数：同一天里两个人都发过动作才算一天（单向不计）。 */
    private long bondBothDays(CoupleSpacePO space) {
        Map<String, Set<String>> byDay = new HashMap<>();
        for (CoupleActionPO action : actionMapper.findBySpace(space.getId())) {
            if (action.getCreated() == null) {
                continue;
            }
            String day = java.time.Instant.ofEpochMilli(action.getCreated())
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
            byDay.computeIfAbsent(day, k -> new HashSet<>()).add(action.getUsername());
        }
        return byDay.values().stream().filter(users -> users.size() >= 2).count();
    }

    private CoupleSpacePO requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    /** 日期范围钳制：1-90，给默认值。 */
    private int clampDays(int days, int def) {
        if (days <= 0) {
            return def;
        }
        return Math.min(days, 90);
    }

    private CoupleInvitePO requireInvite(String inviteId) {
        CoupleInvitePO invite = inviteMapper.selectById(inviteId);
        if (invite == null) {
            throw new BusinessException(404, "邀请不存在");
        }
        return invite;
    }

    private SpaceVO toSpaceVO(CoupleSpacePO space, String me) {
        String partner = space.partnerOf(me);
        PeerProfileReader.PeerProfile profile = profiles.read(partner).orElse(null);
        String nickname = profile == null || profile.nickname() == null || profile.nickname().isBlank()
                ? partner : profile.nickname();
        String avatar = profile == null || profile.avatar() == null ? "" : profile.avatar();
        PartnerVO partnerVO = new PartnerVO(partner, nickname, avatar, push.isOnline(partner), space.nickOf(partner));
        return new SpaceVO(space.getId(), partnerVO, space.getCreated(), space.getAnniversary(),
                daysTogether(space), space.getSlogan(),
                space.getTheme() == null ? "classic" : space.getTheme(), space.getStickers());
    }

    /** 在一起天数：从纪念日（缺省取建立日）算到今天，含当天（建立当天 = 第 1 天）。 */
    private long daysTogether(CoupleSpacePO space) {
        LocalDate start;
        try {
            start = LocalDate.parse(space.getAnniversary());
        } catch (Exception e) {
            start = Instant.ofEpochMilli(space.getCreated()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        long days = ChronoUnit.DAYS.between(start, LocalDate.now()) + 1;
        return Math.max(days, 1);
    }

    private static AnniversaryVO toAnniversaryVO(CoupleAnniversaryPO row) {
        return new AnniversaryVO(row.getId(), row.getTitle(), row.getEventDate(), row.yearlyFlag(),
                row.getKind() == null ? CoupleAnniversaryPO.KIND_NORMAL : row.getKind(),
                row.getCreatedBy(), row.getCreated());
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
