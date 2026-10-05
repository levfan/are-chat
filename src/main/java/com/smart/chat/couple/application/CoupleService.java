package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.intimacy.IntimacyCalculator;
import com.smart.chat.couple.domain.intimacy.IntimacySource;
import com.smart.chat.couple.domain.invite.Invite;
import com.smart.chat.couple.domain.invite.InviteRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.couple.domain.streak.StreakDays;
import com.smart.chat.couple.domain.wish.WishRepository;
import com.smart.chat.identity.domain.AccountDirectory;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import com.smart.chat.messaging.domain.FriendshipChecker;
import com.smart.chat.messaging.domain.PeerProfileReader;
import com.smart.chat.sharedkernel.web.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import static com.smart.chat.couple.application.DomainRules.guard;
import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 情侣空间地基：邀请建立 → 空间本体（纪念日/爱称/宣言/主题）→ 恋爱等级（心动值）。
 * <p>
 * 2026-10-05 二轮裁剪后本类只剩地基，四张功能卡各自成服务（streak/question/wish/memory）；
 * 心动值的五项供数也全部改由现役功能供给，推导与阈值重标定见
 * {@code docs/adr/0010-couple-trim-to-v8-features.md}。
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
                          String slogan, String theme) {
    }

    public record OverviewVO(SpaceVO space, List<InviteVO> incoming, List<InviteVO> outgoing) {
    }

    /**
     * 心动值明细：在一起天数、累计打卡天数、历史最长连续、双方都答完每日一问的天数、已实现愿望条数。
     * 五项全部来自现役功能——裁剪前那六项里的心情/贴贴/好事/留灯/复盘/积分已随功能一并下线。
     */
    public record IntimacyBreakdown(long daysTogether, long checkinDays, long longestStreak, long answerDays,
                                    long wishFulfilled) {
    }

    public record IntimacyVO(int score, int level, String title, String icon, Integer nextLevelAt,
                             /** 距下一级进度 0-100（满级=100） */
                             int levelProgress, IntimacyBreakdown breakdown) {
    }

    // ========== 依赖 ==========

    private final CoupleSpaceRepository spaceRepository;
    private final InviteRepository inviteRepository;
    private final WishRepository wishRepository;
    private final CoupleStreakService streakService;
    private final CoupleQuestionService questionService;
    private final FriendshipChecker friendships;
    private final PeerProfileReader profiles;
    private final AccountDirectory accounts;
    private final CoupleEventPublisher push;

    public CoupleService(CoupleSpaceRepository spaceRepository, InviteRepository inviteRepository,
                         WishRepository wishRepository, CoupleStreakService streakService,
                         CoupleQuestionService questionService,
                         FriendshipChecker friendships, PeerProfileReader profiles,
                         AccountDirectory accounts, CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.inviteRepository = inviteRepository;
        this.wishRepository = wishRepository;
        this.streakService = streakService;
        this.questionService = questionService;
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

    /** B 同意 → 情侣空间开启，并立刻确认第 1 天打卡（点头本身就是一次互动）。 */
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
        streakService.confirmCreationDay(space);
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
            return new OverviewVO(null, incoming, outgoing);
        }
        return new OverviewVO(toSpaceVO(space, me), incoming, outgoing);
    }

    /** 绑定「在一起」的日子：它是在一起天数、百日回顾与倒数提醒共同的起点。 */
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

    // ========== 空间个性化：宣言 / 主题 / 爱称 ==========

    /**
     * 更新空间个性化：宣言（≤60 字）、主题（白名单）、给对方起的爱称（≤30 字）。
     * 任一传 null 表示该项不修改；爱称只能由对方改（{@code renamePartner} 守这条），
     * 传空串表示清除。全部字段校验后一次性保存。
     * <p>
     * 推送按<b>真正变了什么</b>分流：爱称要给对方看到那句「TA 给你起了新爱称」，
     * 装扮走另一条事件；只改爱称却推「打扮了小空间」等于把一件心事说成一次装修。
     */
    public SpaceVO updateProfile(String me, String slogan, String theme, String petName) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String sloganBefore = space.slogan();
        String themeBefore = space.theme();
        String petBefore = space.nickOf(partner);
        DomainRules.guard(() -> {
            space.decorate(slogan, theme);
            if (petName != null) {
                space.renamePartner(me, petName);
            }
        });
        spaceRepository.save(space);
        String petNow = space.nickOf(partner);
        if (!Objects.equals(petBefore, petNow)) {
            push.pushCoupleEvent("pet-name-changed", me, partner, petNow == null
                    ? "TA 把给你的爱称收回了 🏷️ 想起来了再喊我"
                    : "TA 给你起了新爱称「" + petNow + "」🏷️");
        }
        if (!Objects.equals(sloganBefore, space.slogan()) || !Objects.equals(themeBefore, space.theme())) {
            push.pushCoupleEvent("space-themed", me, partner, "TA 打扮了你们的小空间 ✨ 快去看看");
        }
        return toSpaceVO(space, me);
    }

    // ========== 心动值 & 恋爱等级 ==========

    /**
     * 心动值 = 在一起天数×1 + 累计打卡天数×2 + 历史最长连续×3 + 双方都答完每日一问的天数×3
     *          + 已实现愿望数×5；累计分数映射七级恋爱等级。
     * 取数只负责「攒了多少」，加权与定级在 couple.domain.intimacy。
     */
    public IntimacyVO intimacy(String me) {
        CoupleSpace space = requireSpace(me);
        StreakDays streak = streakService.streakOf(space.id());
        long daysTogether = daysTogether(space);
        long checkinDays = streak.confirmedDays();
        long longestStreak = streak.longestStreak();
        long answerDays = questionService.bothAnsweredDays(space.id());
        long wishFulfilled = wishRepository.countFulfilled(space.id());

        IntimacyCalculator.Intimacy result = IntimacyCalculator.evaluate(
                new IntimacySource(daysTogether, checkinDays, longestStreak, answerDays, wishFulfilled));
        IntimacyCalculator.IntimacyLevel level = result.level();
        IntimacyBreakdown breakdown = new IntimacyBreakdown(daysTogether, checkinDays, longestStreak,
                answerDays, wishFulfilled);
        return new IntimacyVO(result.score(), level.level(), level.title(), level.icon(),
                level.nextLevelAt(), level.progress(), breakdown);
    }

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

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
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
                space.theme() == null ? "classic" : space.theme());
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

    private String normalizeDate(String value, String message) {
        try {
            return LocalDate.parse(value.trim()).toString();
        } catch (Exception e) {
            throw new BusinessException(400, message);
        }
    }
}
