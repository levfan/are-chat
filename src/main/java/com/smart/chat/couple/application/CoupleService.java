package com.smart.chat.couple.application;

import com.smart.chat.couple.domain.invite.Invite;
import com.smart.chat.couple.domain.invite.InviteRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
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

import static com.smart.chat.couple.application.DomainRules.guard;
import static com.smart.chat.couple.application.DomainRules.rule;

/**
 * 情侣空间地基：邀请建立 → 空间本体（在一起的日子）。
 * <p>
 * 2026-10-20 第三轮裁剪后本类只剩「建立 / 纪念日 / 解除 / 总览」这一条地基线——每日一问、连续打卡、
 * 愿望清单三张卡连同它们供养的心动值一起下线，空间个性化（宣言 / 主题 / 爱称）也随之收口：
 * 这三样原先各占一个端点与一份推送，如今既没有承载卡片，也没有读它的人。
 * 现役功能卡只剩亲密日历一张，见 {@link CoupleCalendarService}。
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

    public record PartnerVO(String username, String nickname, String avatar, boolean online) {
    }

    public record SpaceVO(String id, PartnerVO partner, Long created, String anniversary, long days) {
    }

    public record OverviewVO(SpaceVO space, List<InviteVO> incoming, List<InviteVO> outgoing) {
    }

    // ========== 依赖 ==========

    private final CoupleSpaceRepository spaceRepository;
    private final InviteRepository inviteRepository;
    private final FriendshipChecker friendships;
    private final PeerProfileReader profiles;
    private final AccountDirectory accounts;
    private final CoupleEventPublisher push;

    public CoupleService(CoupleSpaceRepository spaceRepository, InviteRepository inviteRepository,
                         FriendshipChecker friendships, PeerProfileReader profiles,
                         AccountDirectory accounts, CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.inviteRepository = inviteRepository;
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
        push.pushCoupleEvent("invite-accepted", me, from, "对方同意啦！你们的情侣空间已开启 🎉");
        return toSpaceVO(opened, me);
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

    /** 绑定「在一起」的日子：在一起天数与纪念日倒数提醒共同的起点。 */
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
        PartnerVO partnerVO = new PartnerVO(partner, nickname, avatar, push.isOnline(partner));
        return new SpaceVO(space.id(), partnerVO, space.created(), space.anniversary(), daysTogether(space));
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
