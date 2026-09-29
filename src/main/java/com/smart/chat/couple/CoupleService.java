package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.auth.AppUserService;
import com.smart.chat.common.BusinessException;
import com.smart.chat.im.FriendMapper;
import com.smart.chat.im.ImPushService;
import com.smart.chat.im.UserProfile;
import com.smart.chat.im.UserProfileMapper;
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
        public static InviteVO of(CoupleInvite invite) {
            return new InviteVO(invite.getId(), invite.getFromUser(), invite.getToUser(),
                    invite.getMessage() == null ? "" : invite.getMessage(), invite.getStatus(), invite.getCreated());
        }
    }

    public record PartnerVO(String username, String nickname, String avatar, boolean online, String petName) {
    }

    public record SpaceVO(String id, PartnerVO partner, Long created, String anniversary, long days,
                          String slogan, String theme, String stickers) {
    }

    public record CheckinHalf(boolean morning, boolean night) {
    }

    public record CheckinStateVO(CheckinHalf me, CheckinHalf partner, int streak) {
    }

    public record OverviewVO(SpaceVO space, List<InviteVO> incoming, List<InviteVO> outgoing,
                             CheckinStateVO checkins, long overdueCount, long letterUnread) {
    }

    public record PromiseVO(String id, String promiser, String creditor, String content, Long dueAt,
                            String status, Long doneAt, boolean overdue, Long created) {
    }

    public record QuestionVO(String day, String topic, String question, String myAnswer, String partnerAnswer) {
    }

    public record ItemVO(String id, String kind, String title, String note, String dueDate, boolean done,
                         String doneBy, Long doneAt, String createdBy, Long created) {
    }

    public record AnniversaryVO(String id, String title, String date, boolean yearly, String createdBy, Long created) {
    }

    public record MoodVO(String id, String username, String moodDay, String mood, String note,
                         Long createdAt, Long updatedAt) {
        public static MoodVO of(CoupleMood row) {
            return new MoodVO(row.getId(), row.getUsername(), row.getMoodDay(), row.getMood(),
                    row.getNote() == null ? "" : row.getNote(), row.getCreated(), row.getUpdatedAt());
        }
    }

    /** 一天里双方的心情（谁没记录就是 null）。 */
    public record MoodDayVO(String day, MoodVO mine, MoodVO partner) {
    }

    /** 时光轴事件：type=space/ritual/question/promise/item/anniversary。 */
    public record TimelineEvent(String type, String title, String detail, String byUser, Long at) {
    }

    public record TimelineDay(String day, List<TimelineEvent> events) {
    }

    /** 心动值明细：互道早安/晚安天数、一问完成天数、兑现承诺数、清单完成数、心情记录数。 */
    public record IntimacyBreakdown(long morningDays, long nightDays, long questionDays,
                                    long promiseDone, long itemDone, long moodDays) {
    }

    public record IntimacyVO(int score, int level, String title, String icon, Integer nextLevelAt,
                             /** 距下一级进度 0-100（满级=100） */
                             int levelProgress, IntimacyBreakdown breakdown) {
    }

    /**
     * 悄悄话信件：locked=true 表示未到点的慢递——收件人视角 content 置空（前端显示 🔒），
     * 发件人始终可见自己写的内容。
     */
    public record LetterVO(String id, String sender, String content, Long deliverAt, String status,
                           Long openedAt, boolean locked, Long created) {
    }

    /** 今日一问历史：按天拼好的双方回答。 */
    public record QuestionHistoryVO(String day, String topic, String question, String myAnswer, String partnerAnswer) {
    }

    /** 恋爱条约：pending=true 表示等我盖章（对方提出的）。 */
    public record PactVO(String id, String content, String proposedBy, String acceptedBy,
                         Long acceptedAt, boolean pending, boolean mine, Long created) {
    }

    /** 异地恋助手卡片：任一方城市缺失或不在城市库时，hoursDiff/distanceKm 为 null（城市文本照常展示）。 */
    public record CityCardVO(String myCity, String partnerCity, Integer hoursDiff, Long distanceKm,
                             String partnerZoneId) {
    }

    /** 心愿基金存入流水。 */
    public record FundDepositVO(String id, String username, Long amount, String note, Long created) {
    }

    /** 心愿基金：amount 单位为分；progress 0-100（超过 100 按 100 封顶展示）。 */
    public record FundVO(String id, String title, Long targetAmount, Long savedAmount, String status,
                         boolean reached, int progress, String createdBy,
                         List<FundDepositVO> deposits, Long created) {
    }

    // ========== 依赖 ==========

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleInviteMapper inviteMapper;
    private final CouplePromiseMapper promiseMapper;
    private final CoupleCheckinMapper checkinMapper;
    private final CoupleAnswerMapper answerMapper;
    private final CoupleAnswerReactionMapper answerReactionMapper;
    private final CoupleItemMapper itemMapper;
    private final CoupleAnniversaryMapper anniversaryMapper;
    private final CoupleMoodMapper moodMapper;
    private final CoupleLetterMapper letterMapper;
    private final CouplePactMapper pactMapper;
    private final CoupleFundMapper fundMapper;
    private final CoupleFundDepositMapper fundDepositMapper;
    private final FriendMapper friendMapper;
    private final UserProfileMapper profileMapper;
    private final AppUserService userService;
    private final ImPushService push;

    public CoupleService(CoupleSpaceMapper spaceMapper, CoupleInviteMapper inviteMapper,
                         CouplePromiseMapper promiseMapper, CoupleCheckinMapper checkinMapper,
                         CoupleAnswerMapper answerMapper, CoupleAnswerReactionMapper answerReactionMapper,
                         CoupleItemMapper itemMapper,
                         CoupleAnniversaryMapper anniversaryMapper, CoupleMoodMapper moodMapper,
                         CoupleLetterMapper letterMapper, CouplePactMapper pactMapper,
                         CoupleFundMapper fundMapper, CoupleFundDepositMapper fundDepositMapper,
                         FriendMapper friendMapper, UserProfileMapper profileMapper,
                         AppUserService userService, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.inviteMapper = inviteMapper;
        this.promiseMapper = promiseMapper;
        this.checkinMapper = checkinMapper;
        this.answerMapper = answerMapper;
        this.answerReactionMapper = answerReactionMapper;
        this.itemMapper = itemMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.moodMapper = moodMapper;
        this.letterMapper = letterMapper;
        this.pactMapper = pactMapper;
        this.fundMapper = fundMapper;
        this.fundDepositMapper = fundDepositMapper;
        this.friendMapper = friendMapper;
        this.profileMapper = profileMapper;
        this.userService = userService;
        this.push = push;
    }

    // ========== 邀请建立流程 ==========

    /** A 选择一位好友发起情侣空间邀请。 */
    public InviteVO invite(String me, String target, String message) {
        String raw = target == null ? "" : target.trim();
        if (raw.isEmpty()) {
            throw new BusinessException(400, "想邀请谁？请先选择一位好友");
        }
        String targetName = userService.normalizeUsername(raw);
        if (targetName.equals(me)) {
            throw new BusinessException(400, "不能和自己建立情侣空间哦");
        }
        if (!userService.exists(targetName)) {
            throw new BusinessException(400, "查无此人：对方还没注册或已注销");
        }
        if (friendMapper.findByOwnerAndFriend(me, targetName).isEmpty()) {
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
        CoupleInvite invite = CoupleInvite.of(me, targetName, note.isEmpty() ? null : note);
        inviteMapper.insert(invite);
        String detail = "TA 邀请你开启情侣空间 💕" + (note.isEmpty() ? "" : ("：“" + note + "”"));
        push.pushCoupleEvent("invite", me, targetName, detail);
        return InviteVO.of(invite);
    }

    /** B 同意 → 情侣空间开启。 */
    @Transactional
    public SpaceVO accept(String me, String inviteId) {
        CoupleInvite invite = requireInvite(inviteId);
        if (!invite.getToUser().equals(me)) {
            throw new BusinessException(403, "只能处理发给自己的邀请");
        }
        if (!CoupleInvite.STATUS_PENDING.equals(invite.getStatus())) {
            throw new BusinessException(409, "该邀请已经处理过了");
        }
        String from = invite.getFromUser();
        if (spaceMapper.findActiveByUser(me).isPresent() || spaceMapper.findActiveByUser(from).isPresent()) {
            throw new BusinessException(409, "无法同意：有一方已经进入其他情侣空间");
        }
        invite.setStatus(CoupleInvite.STATUS_ACCEPTED);
        invite.setUpdatedAt(System.currentTimeMillis());
        inviteMapper.updateById(invite);

        String[] pair = CoupleSpace.ordered(from, me);
        CoupleSpace space = CoupleSpace.of(pair[0], pair[1]);
        spaceMapper.insert(space);
        push.pushCoupleEvent("invite-accepted", me, from, "对方同意啦！你们的情侣空间已开启 🎉");
        return toSpaceVO(space, me);
    }

    /** B 拒绝。 */
    public void reject(String me, String inviteId) {
        CoupleInvite invite = requireInvite(inviteId);
        if (!invite.getToUser().equals(me)) {
            throw new BusinessException(403, "只能处理发给自己的邀请");
        }
        if (!CoupleInvite.STATUS_PENDING.equals(invite.getStatus())) {
            throw new BusinessException(409, "该邀请已经处理过了");
        }
        invite.setStatus(CoupleInvite.STATUS_REJECTED);
        invite.setUpdatedAt(System.currentTimeMillis());
        inviteMapper.updateById(invite);
        push.pushCoupleEvent("invite-rejected", me, invite.getFromUser(), "TA 婉拒了情侣空间邀请，做朋友也很好");
    }

    /** A 撤回自己发出的待处理邀请。 */
    public void cancel(String me, String inviteId) {
        CoupleInvite invite = requireInvite(inviteId);
        if (!invite.getFromUser().equals(me)) {
            throw new BusinessException(403, "只能撤回自己发出的邀请");
        }
        if (!CoupleInvite.STATUS_PENDING.equals(invite.getStatus())) {
            throw new BusinessException(409, "该邀请已经处理过了");
        }
        invite.setStatus(CoupleInvite.STATUS_CANCELED);
        invite.setUpdatedAt(System.currentTimeMillis());
        inviteMapper.updateById(invite);
    }

    /** 解除情侣空间：双方历史数据保留，但不再互相可见，各自可发起新邀请。 */
    public void dissolve(String me) {
        CoupleSpace space = requireSpace(me);
        space.setStatus(CoupleSpace.STATUS_DISSOLVED);
        space.setDissolvedAt(System.currentTimeMillis());
        spaceMapper.updateById(space);
        push.pushCoupleEvent("dissolved", me, space.partnerOf(me), "对方解除了情侣空间 😢");
    }

    // ========== 总览 ==========

    public OverviewVO overview(String me) {
        // 待处理邀请返回全部（可能同时收到多人邀请），按时间新→旧
        List<InviteVO> incoming = inviteMapper.findPendingTo(me).stream().map(InviteVO::of).toList();
        List<InviteVO> outgoing = inviteMapper.findPendingFrom(me).stream().map(InviteVO::of).toList();
        CoupleSpace space = spaceMapper.findActiveByUser(me).orElse(null);
        if (space == null) {
            return new OverviewVO(null, incoming, outgoing, null, 0, 0);
        }
        long now = System.currentTimeMillis();
        // 我还没兑现的逾期承诺数（给「还有 N 件事你没做到哦~」提醒条用）
        long overdue = promiseMapper.findBySpace(space.getId()).stream()
                .filter(p -> p.getPromiser().equals(me) && p.isOverdue(now))
                .count();
        // 我可以拆但还没拆的悄悄话数（信箱 tab 红点）
        long letterUnread = letterMapper.countOpenable(space.getId(), me, now);
        return new OverviewVO(toSpaceVO(space, me), incoming, outgoing, checkinState(space, me), overdue, letterUnread);
    }

    public SpaceVO setAnniversary(String me, String date) {
        CoupleSpace space = requireSpace(me);
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
        if (friendMapper.findByOwnerAndFriend(me, name).isEmpty()) {
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
        CoupleSpace space = requireSpace(me);
        if (slogan != null) {
            String text = slogan.trim();
            if (text.length() > 60) {
                throw new BusinessException(400, "宣言最多 60 字，留白也很美");
            }
            space.setSlogan(text.isEmpty() ? null : text);
        }
        if (theme != null) {
            if (!CoupleSpace.THEMES.contains(theme)) {
                throw new BusinessException(400, "这个主题还没上架哦");
            }
            space.setTheme(theme);
        }
        if (stickers != null) {
            String normalized = stickers.trim();
            if (!normalized.isEmpty()) {
                String[] keys = normalized.split(",");
                if (keys.length > CoupleSpace.STICKER_MAX) {
                    throw new BusinessException(400, "贴纸墙最多佩戴 " + CoupleSpace.STICKER_MAX + " 枚");
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

    public List<PromiseVO> listPromises(String me) {
        CoupleSpace space = requireSpace(me);
        long now = System.currentTimeMillis();
        return promiseMapper.findBySpace(space.getId()).stream()
                .map(p -> toPromiseVO(p, now))
                .toList();
    }

    /**
     * 新建承诺卡：side=me 表示「我答应 TA」，side=partner 表示「TA 答应我」。
     * 口头承诺在这里变成可追踪的甜蜜记录。
     */
    public PromiseVO createPromise(String me, String side, String content, Long dueAt) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String text = content == null ? "" : content.trim();
        if (text.isEmpty() || text.length() > CouplePromise.CONTENT_MAX) {
            throw new BusinessException(400, "先写下承诺内容（1-200 字）");
        }
        if (dueAt != null && dueAt <= System.currentTimeMillis()) {
            throw new BusinessException(400, "截止时间要晚于现在哦");
        }
        boolean promiserIsPartner = "partner".equalsIgnoreCase(side);
        String promiser = promiserIsPartner ? partner : me;
        String creditor = promiserIsPartner ? me : partner;
        CouplePromise promise = CouplePromise.of(space.getId(), promiser, creditor, text, dueAt);
        promiseMapper.insert(promise);
        String detail = promiser.equals(me)
                ? "对方记下了你的承诺：" + text + "，兑现后记得打卡 ✅"
                : "你答应对方的事被记下来啦：" + text + " 😉";
        push.pushCoupleEvent("promise-created", me, partner, detail);
        return toPromiseVO(promise, System.currentTimeMillis());
    }

    /** 承诺人打卡兑现 → 对方收到推送「TA 兑现了对你的承诺 🎉」。 */
    public PromiseVO donePromise(String me, String promiseId) {
        CoupleSpace space = requireSpace(me);
        CouplePromise promise = requirePromise(promiseId, space);
        if (!promise.getPromiser().equals(me)) {
            throw new BusinessException(403, "只有承诺人能打卡兑现哦");
        }
        if (CouplePromise.STATUS_DONE.equals(promise.getStatus())) {
            throw new BusinessException(409, "这条约定已经兑现过了");
        }
        promise.setStatus(CouplePromise.STATUS_DONE);
        promise.setDoneAt(System.currentTimeMillis());
        promise.setLastRemindDay(null);
        promiseMapper.updateById(promise);
        push.pushCoupleEvent("promise-done", me, promise.getCreditor(),
                "TA 兑现了对你的承诺 🎉：" + promise.getContent());
        return toPromiseVO(promise, System.currentTimeMillis());
    }

    /** 打错卡了：撤销兑现，约定重新生效。 */
    public PromiseVO undonePromise(String me, String promiseId) {
        CoupleSpace space = requireSpace(me);
        CouplePromise promise = requirePromise(promiseId, space);
        if (!promise.getPromiser().equals(me)) {
            throw new BusinessException(403, "只有承诺人能操作哦");
        }
        if (!CouplePromise.STATUS_DONE.equals(promise.getStatus())) {
            throw new BusinessException(409, "这条约定还没兑现");
        }
        promise.setStatus(CouplePromise.STATUS_PENDING);
        promise.setDoneAt(null);
        promiseMapper.updateById(promise);
        push.pushCoupleEvent("promise-undone", me, promise.getCreditor(),
                "TA 取消了一条兑现打卡，约定重新生效啦：" + promise.getContent());
        return toPromiseVO(promise, System.currentTimeMillis());
    }

    public void deletePromise(String me, String promiseId) {
        CoupleSpace space = requireSpace(me);
        CouplePromise promise = requirePromise(promiseId, space);
        promiseMapper.deleteById(promise.getId());
        push.pushCoupleEvent("promise-deleted", me, space.partnerOf(me),
                "有一条约定被删除了：" + promise.getContent());
    }

    // ========== 2. 每日小仪式 ==========

    /** 早安/晚安打卡：双方都打卡后解锁当日专属背景/贴纸。 */
    public CheckinStateVO checkin(String me, String kind) {
        if (!CoupleCheckin.KIND_MORNING.equals(kind) && !CoupleCheckin.KIND_NIGHT.equals(kind)) {
            throw new BusinessException(400, "打卡类型只支持 MORNING / NIGHT");
        }
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String day = today();
        boolean partnerDone = checkinMapper.find(space.getId(), partner, kind, day).isPresent();
        if (checkinMapper.find(space.getId(), me, kind, day).isEmpty()) {
            try {
                checkinMapper.insert(CoupleCheckin.of(space.getId(), me, kind, day));
            } catch (Exception e) {
                // 唯一键兜底：同日重复打卡视为幂等
            }
        }
        String eventDetail = CoupleCheckin.KIND_MORNING.equals(kind) ? "TA 跟你说早安啦 ☀️" : "TA 跟你说晚安啦 🌙";
        push.pushCoupleEvent("checkin", me, partner, eventDetail);
        if (partnerDone) {
            // 这一次打卡凑齐了双方 → 解锁当日专属
            String unlockDetail = CoupleCheckin.KIND_MORNING.equals(kind)
                    ? "早安仪式达成！今日专属背景已解锁 🎉"
                    : "晚安仪式达成！今日专属贴纸已解锁 🎉";
            push.pushCoupleEventBoth("ritual-unlocked", me, me, partner, unlockDetail);
        }
        return checkinState(space, me);
    }

    public QuestionVO todayQuestion(String me) {
        CoupleSpace space = requireSpace(me);
        String day = today();
        CoupleQuestions.BankQuestion picked = CoupleQuestions.pick(day);
        String my = answerMapper.find(space.getId(), day, me).map(CoupleAnswer::getAnswer).orElse(null);
        String partnerAnswer = answerMapper.find(space.getId(), day, space.partnerOf(me))
                .map(CoupleAnswer::getAnswer).orElse(null);
        return new QuestionVO(day, picked.topic(), picked.text(), my, partnerAnswer);
    }

    /** 回答今日一问：双方回答后拼在一起看；重复提交视为修改。 */
    public QuestionVO answerQuestion(String me, String answer) {
        CoupleSpace space = requireSpace(me);
        String text = answer == null ? "" : answer.trim();
        if (text.isEmpty() || text.length() > CoupleAnswer.ANSWER_MAX) {
            throw new BusinessException(400, "写下你的回答（1-300 字）");
        }
        String day = today();
        CoupleAnswer existing = answerMapper.find(space.getId(), day, me).orElse(null);
        if (existing != null) {
            existing.setAnswer(text);
            answerMapper.updateById(existing);
        } else {
            answerMapper.insert(CoupleAnswer.of(space.getId(), day, me, text));
        }
        push.pushCoupleEvent("question-answered", me, space.partnerOf(me), "TA 已经回答了今日一问，快去看看吧 💬");
        return todayQuestion(me);
    }

    // ========== F48 一问互评 ==========

    /** 互评条目：谁给的什么反应。 */
    public record AnswerReactionVO(String fromUser, String emoji, Long created) {
    }

    /** 对某天 TA 的一问回答点一个反应（每人每天一条，可改）。 */
    public List<AnswerReactionVO> reactAnswer(String me, String day, String emoji) {
        CoupleSpace space = requireSpace(me);
        String cleanDay = day == null ? "" : day.trim();
        try {
            LocalDate.parse(cleanDay);
        } catch (Exception e) {
            throw new BusinessException(400, "日期格式应为 yyyy-MM-dd");
        }
        String cleanEmoji = emoji == null ? "" : emoji.trim();
        if (cleanEmoji.isEmpty() || cleanEmoji.length() > CoupleAnswerReaction.EMOJI_MAX) {
            throw new BusinessException(400, "选一个表情送给 TA 的回答吧");
        }
        // 只有对方回答过才能互评
        if (answerMapper.find(space.getId(), cleanDay, space.partnerOf(me)).isEmpty()) {
            throw new BusinessException(409, "TA 还没有回答这一问，先等等吧");
        }
        CoupleAnswerReaction existing = answerReactionMapper.find(space.getId(), cleanDay, me).orElse(null);
        if (existing != null) {
            existing.setEmoji(cleanEmoji);
            answerReactionMapper.updateById(existing);
        } else {
            answerReactionMapper.insert(CoupleAnswerReaction.of(space.getId(), cleanDay, me, cleanEmoji));
        }
        push.pushCoupleEvent("answer-reacted", me, space.partnerOf(me),
                "TA 看了你的回答，回了你一个 " + cleanEmoji);
        return listAnswerReactions(me, cleanDay);
    }

    /** 某天双方对彼此回答的反应列表。 */
    public List<AnswerReactionVO> listAnswerReactions(String me, String day) {
        CoupleSpace space = requireSpace(me);
        String cleanDay = day == null ? "" : day.trim();
        return answerReactionMapper.findByDay(space.getId(), cleanDay).stream()
                .map(r -> new AnswerReactionVO(r.getFromUser(), r.getEmoji(), r.getCreated()))
                .toList();
    }

    // ========== 3. 共享空间 ==========

    public List<ItemVO> listItems(String me) {
        CoupleSpace space = requireSpace(me);
        return itemMapper.findBySpace(space.getId()).stream().map(CoupleService::toItemVO).toList();
    }

    public ItemVO createItem(String me, String kind, String title, String note, String dueDate) {
        CoupleSpace space = requireSpace(me);
        validateKind(kind);
        CoupleItem item = CoupleItem.of(space.getId(), kind,
                requireText(title, "事项标题不能为空（最多 100 字）", CoupleItem.TITLE_MAX), me);
        item.setNote(requireOptional(note, "补充说明最多 300 字", CoupleItem.NOTE_MAX));
        item.setDueDate(normalizeDateOrNull(dueDate, "计划日期格式应为 yyyy-MM-dd"));
        itemMapper.insert(item);
        push.pushCoupleEvent("items-changed", me, space.partnerOf(me), "共享清单有更新 ✨：" + item.getTitle());
        return toItemVO(item);
    }

    public ItemVO updateItem(String me, String itemId, String title, String note, String dueDate, Boolean done) {
        CoupleSpace space = requireSpace(me);
        CoupleItem item = requireItem(itemId, space);
        if (title != null) {
            item.setTitle(requireText(title, "事项标题不能为空（最多 100 字）", CoupleItem.TITLE_MAX));
        }
        if (note != null) {
            item.setNote(requireOptional(note, "补充说明最多 300 字", CoupleItem.NOTE_MAX));
        }
        if (dueDate != null) {
            item.setDueDate(normalizeDateOrNull(dueDate, "计划日期格式应为 yyyy-MM-dd"));
        }
        if (done != null) {
            if (done) {
                item.setDone(1);
                item.setDoneBy(me);
                item.setDoneAt(System.currentTimeMillis());
            } else {
                item.setDone(0);
                item.setDoneBy(null);
                item.setDoneAt(null);
            }
        }
        itemMapper.updateById(item);
        push.pushCoupleEvent("items-changed", me, space.partnerOf(me), "共享清单有更新 ✨：" + item.getTitle());
        return toItemVO(item);
    }

    public void deleteItem(String me, String itemId) {
        CoupleSpace space = requireSpace(me);
        CoupleItem item = requireItem(itemId, space);
        itemMapper.deleteById(item.getId());
        push.pushCoupleEvent("items-changed", me, space.partnerOf(me), "共享清单删掉了一项：" + item.getTitle());
    }

    public List<AnniversaryVO> listAnniversaries(String me) {
        CoupleSpace space = requireSpace(me);
        return anniversaryMapper.findBySpace(space.getId()).stream().map(CoupleService::toAnniversaryVO).toList();
    }

    public AnniversaryVO createAnniversary(String me, String title, String date, Boolean yearly) {
        CoupleSpace space = requireSpace(me);
        String normalized = normalizeDate(date, "日期格式应为 yyyy-MM-dd");
        CoupleAnniversary row = CoupleAnniversary.of(space.getId(),
                requireText(title, "纪念日名称不能为空（最多 60 字）", CoupleAnniversary.TITLE_MAX),
                normalized, yearly == null || yearly, me);
        anniversaryMapper.insert(row);
        push.pushCoupleEvent("anniversaries-changed", me, space.partnerOf(me), "共同日历有更新 📅：" + row.getTitle());
        return toAnniversaryVO(row);
    }

    public void deleteAnniversary(String me, String anniversaryId) {
        CoupleSpace space = requireSpace(me);
        CoupleAnniversary row = anniversaryMapper.selectById(anniversaryId);
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
        CoupleSpace space = requireSpace(me);
        if (mood == null || !CoupleMood.MOOD_KEYS.contains(mood)) {
            throw new BusinessException(400, "心情不在可选范围内哦");
        }
        String text = requireOptional(note, "一句话心情最多 200 字", CoupleMood.NOTE_MAX);
        String day = today();
        CoupleMood row = moodMapper.find(space.getId(), me, day).orElse(null);
        if (row != null) {
            row.setMood(mood);
            row.setNote(text);
            row.setUpdatedAt(System.currentTimeMillis());
            moodMapper.updateById(row);
        } else {
            row = CoupleMood.of(space.getId(), me, day, mood, text);
            moodMapper.insert(row);
        }
        push.pushCoupleEvent("mood-changed", me, space.partnerOf(me),
                "TA 记录了今天的心情 " + CoupleMood.emojiOf(mood) + "，快去看看吧");
        return MoodVO.of(row);
    }

    /** 双方最近 N 天（1-90，默认 14）的心情，按日期新→旧，只返回至少有一方记录的日子。 */
    public List<MoodDayVO> listMoods(String me, int days) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        int limit = clampDays(days, 14);
        String startDay = LocalDate.now().minusDays(limit - 1L).toString();
        Map<String, CoupleMood> mine = new HashMap<>();
        Map<String, CoupleMood> theirs = new HashMap<>();
        for (CoupleMood row : moodMapper.findBySpace(space.getId())) {
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

    // ========== 5. 恋爱时光轴 ==========
    /**
     * 最近 N 天（1-90，默认 30）的「我们的故事」：自动聚合空间建立、互道早晚安、
     * 今日一问完成、承诺兑现、清单打卡、纪念日，按天分组新→旧。
     */
    public List<TimelineDay> timeline(String me, int days) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        int limit = clampDays(days, 30);
        LocalDate startDate = LocalDate.now().minusDays(limit - 1L);
        String startDay = startDate.toString();
        Map<String, List<TimelineEvent>> byDay = new TreeMap<>(Comparator.reverseOrder());

        // 空间建立
        LocalDate createdDay = Instant.ofEpochMilli(space.getCreated()).atZone(ZoneId.systemDefault()).toLocalDate();
        if (!createdDay.isBefore(startDate)) {
            byDay.computeIfAbsent(createdDay.toString(), k -> new ArrayList<>())
                    .add(new TimelineEvent("space", "我们的情侣空间建立啦 💕", null, null, space.getCreated()));
        }

        // 双方互道早晚安的日期（有具体打卡时间取较晚的一条）
        for (String kind : new String[]{CoupleCheckin.KIND_MORNING, CoupleCheckin.KIND_NIGHT}) {
            Map<String, Long> mine = new HashMap<>();
            Map<String, Long> theirs = new HashMap<>();
            for (CoupleCheckin row : checkinMapper.findBySpaceAndKind(space.getId(), kind)) {
                if (row.getCheckinDay().compareTo(startDay) < 0) {
                    continue;
                }
                Map<String, Long> target = row.getUsername().equals(me) ? mine : theirs;
                target.merge(row.getCheckinDay(), row.getCreated(), Math::max);
            }
            boolean morning = CoupleCheckin.KIND_MORNING.equals(kind);
            for (String day : mine.keySet()) {
                Long theirsAt = theirs.get(day);
                if (theirsAt == null) {
                    continue;
                }
                byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(new TimelineEvent("ritual",
                        morning ? "互道早安 ☀️" : "互道晚安 🌙", null, null, Math.max(mine.get(day), theirsAt)));
            }
        }

        // 今日一问：双方都回答的日子（题目按日期复算）
        Map<String, Map<String, CoupleAnswer>> answersByDay = new HashMap<>();
        for (CoupleAnswer row : answerMapper.findBySpace(space.getId())) {
            if (row.getAnswerDay().compareTo(startDay) < 0) {
                continue;
            }
            answersByDay.computeIfAbsent(row.getAnswerDay(), k -> new HashMap<>()).put(row.getUsername(), row);
        }
        for (Map.Entry<String, Map<String, CoupleAnswer>> entry : answersByDay.entrySet()) {
            Map<String, CoupleAnswer> users = entry.getValue();
            CoupleAnswer mine = users.get(me);
            CoupleAnswer theirs = users.get(partner);
            if (mine == null || theirs == null) {
                continue;
            }
            byDay.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(new TimelineEvent("question",
                    "今日一问完成 💬", CoupleQuestions.pick(entry.getKey()).text(), null,
                    Math.max(mine.getCreated(), theirs.getCreated())));
        }

        // 承诺兑现
        for (CouplePromise p : promiseMapper.findBySpace(space.getId())) {
            if (!CouplePromise.STATUS_DONE.equals(p.getStatus()) || p.getDoneAt() == null) {
                continue;
            }
            String day = Instant.ofEpochMilli(p.getDoneAt()).atZone(ZoneId.systemDefault()).toLocalDate().toString();
            if (day.compareTo(startDay) < 0) {
                continue;
            }
            byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(new TimelineEvent("promise",
                    "兑现了承诺 🎉：" + p.getContent(), null, p.getPromiser(), p.getDoneAt()));
        }

        // 共享清单完成
        for (CoupleItem item : itemMapper.findBySpace(space.getId())) {
            if (!item.isDone() || item.getDoneAt() == null) {
                continue;
            }
            String day = Instant.ofEpochMilli(item.getDoneAt()).atZone(ZoneId.systemDefault()).toLocalDate().toString();
            if (day.compareTo(startDay) < 0) {
                continue;
            }
            byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(new TimelineEvent("item",
                    "一起完成了 ✅：" + item.getTitle(), null, item.getDoneBy(), item.getDoneAt()));
        }

        // 纪念日（yearly 的按本年度落位）
        for (CoupleAnniversary row : anniversaryMapper.findBySpace(space.getId())) {
            LocalDate date = anniversaryOccurrence(row, startDate);
            if (date == null) {
                continue;
            }
            byDay.computeIfAbsent(date.toString(), k -> new ArrayList<>()).add(new TimelineEvent("anniversary",
                    row.getTitle() + " 🎊", null, row.getCreatedBy(),
                    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        }

        // 天内事件按时间正序，天与天之间新→旧
        List<TimelineDay> result = new ArrayList<>();
        for (Map.Entry<String, List<TimelineEvent>> entry : byDay.entrySet()) {
            entry.getValue().sort(Comparator.comparingLong(e -> e.at() == null ? 0L : e.at()));
            result.add(new TimelineDay(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    // ========== 6. 心动值 & 恋爱等级 ==========

    /**
     * 心动值：双方互道早安 +1/天、互道晚安 +2/天、一问双方都答 +2/天、
     * 兑现承诺 +5/条、清单完成 +3/条、心情记录 +1/条；累计分数映射恋爱等级。
     */
    public IntimacyVO intimacy(String me) {
        CoupleSpace space = requireSpace(me);
        long morning = ritualBothDays(space, CoupleCheckin.KIND_MORNING);
        long night = ritualBothDays(space, CoupleCheckin.KIND_NIGHT);
        long questionDays = bothAnsweredDays(space);
        long promiseDone = promiseMapper.findBySpace(space.getId()).stream()
                .filter(p -> CouplePromise.STATUS_DONE.equals(p.getStatus())).count();
        long itemDone = itemMapper.findBySpace(space.getId()).stream().filter(CoupleItem::isDone).count();
        long moodDays = moodMapper.findBySpace(space.getId()).size();
        int score = (int) (morning + night * 2 + questionDays * 2 + promiseDone * 5 + itemDone * 3 + moodDays);
        IntimacyBreakdown breakdown = new IntimacyBreakdown(morning, night, questionDays, promiseDone, itemDone, moodDays);

        // 等级阶梯：L1 怦然心动(0) → L2 心动初启(50) → L3 甜甜热恋(150) → L4 形影不离(300)
        //          → L5 心有灵犀(500) → L6 相依相伴(800) → L7 相守一生(1300)
        int level;
        if (score >= 1300) {
            level = 7;
        } else if (score >= 800) {
            level = 6;
        } else if (score >= 500) {
            level = 5;
        } else if (score >= 300) {
            level = 4;
        } else if (score >= 150) {
            level = 3;
        } else if (score >= 50) {
            level = 2;
        } else {
            level = 1;
        }
        int[] nextAt = {50, 150, 300, 500, 800, 1300, 0};
        String[] titles = {"", "怦然心动", "心动初启", "甜甜热恋", "形影不离", "心有灵犀", "相依相伴", "相守一生"};
        String[] icons = {"", "✨", "💫", "🍬", "🧡", "💞", "🌷", "💍"};
        Integer next = level >= 7 ? null : nextAt[level - 1];
        // 距下一级进度：以本级起点与下一级阈值插值（满级恒为 100）
        int progress;
        if (level >= 7) {
            progress = 100;
        } else {
            int floor = level == 1 ? 0 : nextAt[level - 2];
            int span = nextAt[level - 1] - floor;
            progress = span <= 0 ? 100 : (int) Math.min(99, (score - floor) * 100L / span);
        }
        return new IntimacyVO(score, level, titles[level], icons[level], next, progress, breakdown);
    }

    // ========== 7. 悄悄话信箱 ==========

    /**
     * 写一封悄悄话给 TA：content 1-300 字；deliverAt 为空 = 立即可拆，
     * 非空 = 慢递（必须是未来时间且不超过 7 天），到点前收件人拆不了。
     */
    public LetterVO saveLetter(String me, String content, Long deliverAt) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        String text = requireText(content, "写下你想说的话（1-300 字）", CoupleLetter.CONTENT_MAX);
        long now = System.currentTimeMillis();
        if (deliverAt != null && deliverAt <= now) {
            throw new BusinessException(400, "慢递时间要晚于现在哦");
        }
        if (deliverAt != null && deliverAt > now + CoupleLetter.DELIVER_MAX_MILLIS) {
            throw new BusinessException(400, "慢递最长 7 天，再多就等不及啦");
        }
        CoupleLetter letter = CoupleLetter.of(space.getId(), me, partner, text, deliverAt);
        letterMapper.insert(letter);
        String detail = deliverAt == null
                ? "TA 给你写了一封悄悄话 💌，快去拆开看看"
                : "TA 给你写了一封慢递悄悄话 💌，到点才能拆开哦";
        push.pushCoupleEvent("letter-created", me, partner, detail);
        return toLetterVO(letter, me, now);
    }

    /** 信箱列表：发件 + 收件都在一个列表里（新→旧），未到期慢递对收件人隐藏内容。 */
    public List<LetterVO> listLetters(String me) {
        CoupleSpace space = requireSpace(me);
        long now = System.currentTimeMillis();
        return letterMapper.findBySpace(space.getId()).stream()
                .map(letter -> toLetterVO(letter, me, now))
                .toList();
    }

    /** 拆信：只有收件人能拆，且要到了可拆时间；拆开推送给发件人。 */
    public LetterVO openLetter(String me, String letterId) {
        CoupleSpace space = requireSpace(me);
        CoupleLetter letter = requireLetter(letterId, space);
        long now = System.currentTimeMillis();
        if (!letter.getRecipient().equals(me)) {
            throw new BusinessException(403, "只能由收件人拆开哦");
        }
        if (CoupleLetter.STATUS_OPENED.equals(letter.getStatus())) {
            throw new BusinessException(409, "这封信已经拆过了");
        }
        if (letter.locked(now)) {
            throw new BusinessException(400, "慢递还没到点，再等等哦 ⏳");
        }
        letter.setStatus(CoupleLetter.STATUS_OPENED);
        letter.setOpenedAt(now);
        letterMapper.updateById(letter);
        push.pushCoupleEvent("letter-opened", me, letter.getSender(), "TA 拆开了你的悄悄话 💌");
        return toLetterVO(letter, me, now);
    }

    /** 撤回：只有发件人、且还没被拆开时可以撤。 */
    public void deleteLetter(String me, String letterId) {
        CoupleSpace space = requireSpace(me);
        CoupleLetter letter = requireLetter(letterId, space);
        if (!letter.getSender().equals(me)) {
            throw new BusinessException(403, "只能撤回自己写的悄悄话");
        }
        if (CoupleLetter.STATUS_OPENED.equals(letter.getStatus())) {
            throw new BusinessException(409, "已经拆开的信不能再撤回了");
        }
        letterMapper.deleteById(letter.getId());
    }

    // ========== 8. 今日一问历史回顾 ==========

    /** 双方都回答过的一问存档（最近 N 天，1-90 默认 30，新→旧），题目按日期复算。 */
    public List<QuestionHistoryVO> questionHistory(String me, int days) {
        CoupleSpace space = requireSpace(me);
        int limit = clampDays(days, 30);
        String startDay = LocalDate.now().minusDays(limit - 1L).toString();
        Map<String, Map<String, CoupleAnswer>> byDay = new HashMap<>();
        for (CoupleAnswer row : answerMapper.findBySpace(space.getId())) {
            if (row.getAnswerDay().compareTo(startDay) < 0) {
                continue;
            }
            byDay.computeIfAbsent(row.getAnswerDay(), k -> new HashMap<>()).put(row.getUsername(), row);
        }
        List<QuestionHistoryVO> result = new ArrayList<>();
        for (String day : byDay.keySet().stream().sorted(Comparator.reverseOrder()).toList()) {
            Map<String, CoupleAnswer> users = byDay.get(day);
            CoupleAnswer mine = users.get(me);
            CoupleAnswer partner = users.get(space.partnerOf(me));
            if (mine == null || partner == null) {
                continue;
            }
            CoupleQuestions.BankQuestion picked = CoupleQuestions.pick(day);
            result.add(new QuestionHistoryVO(day, picked.topic(), picked.text(),
                    mine.getAnswer(), partner.getAnswer()));
        }
        return result;
    }

    // ========== 9. 恋爱条约 ==========

    /** 提出一条恋爱条约（待对方盖章）。 */
    public PactVO createPact(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String text = requireText(content, "写下条约内容（1-100 字）", CouplePact.CONTENT_MAX);
        CouplePact pact = CouplePact.of(space.getId(), me, text);
        pactMapper.insert(pact);
        push.pushCoupleEvent("pact-created", me, space.partnerOf(me),
                "TA 提出了一条恋爱条约等你盖章：「" + text + "」🤝");
        return toPactVO(pact, me);
    }

    public List<PactVO> listPacts(String me) {
        CoupleSpace space = requireSpace(me);
        return pactMapper.findBySpace(space.getId()).stream().map(p -> toPactVO(p, me)).toList();
    }

    /** 盖章生效：只有对方（非提出人）能盖。 */
    public PactVO acceptPact(String me, String pactId) {
        CoupleSpace space = requireSpace(me);
        CouplePact pact = requirePact(pactId, space);
        if (pact.getProposedBy().equals(me)) {
            throw new BusinessException(400, "自己提的条约要等 TA 来盖章哦");
        }
        if (pact.isAccepted()) {
            throw new BusinessException(409, "这条条约已经生效过了");
        }
        pact.setAcceptedBy(me);
        pact.setAcceptedAt(System.currentTimeMillis());
        pactMapper.updateById(pact);
        push.pushCoupleEvent("pact-accepted", me, space.partnerOf(me),
                "TA 盖章通过了恋爱条约：「" + pact.getContent() + "」💕 从今天起一起遵守");
        return toPactVO(pact, me);
    }

    /** 废除条约：双方都可以删。 */
    public void deletePact(String me, String pactId) {
        CoupleSpace space = requireSpace(me);
        CouplePact pact = requirePact(pactId, space);
        pactMapper.deleteById(pact.getId());
        push.pushCoupleEvent("pact-deleted", me, space.partnerOf(me),
                "有一条恋爱条约被移除了：" + pact.getContent());
    }

    // ========== 10. 异地恋助手 ==========

    /** 设置我的城市（手填，最长 20 字；匹配内置城市库才能算时差/距离）。 */
    public CityCardVO setCity(String me, String city) {
        CoupleSpace space = requireSpace(me);
        String name = requireOptional(city, "城市名最长 20 个字", 20);
        if (me.equals(space.getUserA())) {
            space.setCityA(name);
        } else {
            space.setCityB(name);
        }
        spaceMapper.updateById(space);
        push.pushCoupleEvent("city-changed", me, space.partnerOf(me),
                "TA 更新了所在城市：" + (name == null ? "清空" : name) + " 📍");
        return cityCard(space, me);
    }

    public CityCardVO cityCard(String me) {
        CoupleSpace space = requireSpace(me);
        return cityCard(space, me);
    }

    // ========== 11. 心愿基金 ==========

    /** 建一个共同存钱目标（金额单位：分）。 */
    public FundVO createFund(String me, String title, Long targetAmount) {
        CoupleSpace space = requireSpace(me);
        String name = requireText(title, "写下心愿名称（1-60 字）", CoupleFund.TITLE_MAX);
        if (targetAmount == null || targetAmount <= 0) {
            throw new BusinessException(400, "目标金额要大于 0 哦");
        }
        if (targetAmount > 9_999_999_999L) {
            throw new BusinessException(400, "目标金额太大了，先立个小目标 💰");
        }
        CoupleFund fund = CoupleFund.of(space.getId(), me, name, targetAmount);
        fundMapper.insert(fund);
        push.pushCoupleEvent("fund-created", me, space.partnerOf(me),
                "TA 发起了一个共同心愿：「" + name + "」，一起攒钱实现它 💰");
        return toFundVO(fund, me);
    }

    public List<FundVO> listFunds(String me) {
        CoupleSpace space = requireSpace(me);
        return fundMapper.findBySpace(space.getId()).stream().map(f -> toFundVO(f, me)).toList();
    }

    /** 往目标里存一笔钱（金额单位：分，>0）；攒够自动标记达成并推送庆祝。 */
    public FundVO depositFund(String me, String fundId, Long amount, String note) {
        CoupleSpace space = requireSpace(me);
        CoupleFund fund = requireFund(fundId, space);
        if (amount == null || amount <= 0) {
            throw new BusinessException(400, "存入金额要大于 0 哦");
        }
        if (fund.isReached()) {
            throw new BusinessException(409, "这个心愿已经达成啦，换下一个目标吧 🎉");
        }
        String memo = requireOptional(note, "存钱留言最多 100 字", CoupleFundDeposit.NOTE_MAX);
        long saved = fund.getSavedAmount() + amount;
        fund.setSavedAmount(saved);
        boolean justReached = saved >= fund.getTargetAmount();
        if (justReached) {
            fund.setStatus(CoupleFund.STATUS_REACHED);
            fund.setDoneAt(System.currentTimeMillis());
        }
        fundMapper.updateById(fund);
        fundDepositMapper.insert(CoupleFundDeposit.of(space.getId(), fund.getId(), me, amount, memo));
        if (justReached) {
            push.pushCoupleEventBoth("fund-reached", me, me, space.partnerOf(me),
                    "共同心愿达成 🎉：「" + fund.getTitle() + "」攒够啦，准备实现它吧！");
        } else {
            push.pushCoupleEvent("fund-deposit", me, space.partnerOf(me),
                    "TA 往共同心愿「" + fund.getTitle() + "」存了一笔钱，进度又近了一点 💰");
        }
        return toFundVO(fund, me);
    }

    /** 删除心愿（连流水一起删）：双方都可以操作。 */
    public void deleteFund(String me, String fundId) {
        CoupleSpace space = requireSpace(me);
        CoupleFund fund = requireFund(fundId, space);
        fundMapper.deleteById(fund.getId());
        fundDepositMapper.delete(new LambdaQueryWrapper<CoupleFundDeposit>()
                .eq(CoupleFundDeposit::getFundId, fund.getId()));
        push.pushCoupleEvent("fund-deleted", me, space.partnerOf(me),
                "共同心愿被移除了：" + fund.getTitle());
    }

    // ========== 84 注销清理（AppUserService.deactivate 调用） ==========

    /** 注销：解散所在空间并清理相关邀请。 */
    @Transactional
    public void purgeUser(String username) {
        CoupleSpace space = spaceMapper.findActiveByUser(username).orElse(null);
        if (space != null) {
            space.setStatus(CoupleSpace.STATUS_DISSOLVED);
            space.setDissolvedAt(System.currentTimeMillis());
            spaceMapper.updateById(space);
            push.pushCoupleEvent("dissolved", username, space.partnerOf(username), "对方账号已注销，情侣空间自动解除 😢");
        }
        inviteMapper.deleteAllInvolving(username);
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
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

    /** 双方都完成某类打卡的自然日数量（互道早安/晚安天数）。 */
    private long ritualBothDays(CoupleSpace space, String kind) {
        Set<String> mine = new HashSet<>();
        Set<String> theirs = new HashSet<>();
        for (CoupleCheckin row : checkinMapper.findBySpaceAndKind(space.getId(), kind)) {
            (row.getUsername().equals(space.getUserA()) ? mine : theirs).add(row.getCheckinDay());
        }
        return mine.stream().filter(theirs::contains).count();
    }

    /** 双方都回答了今日一问的自然日数量。 */
    private long bothAnsweredDays(CoupleSpace space) {
        Map<String, Set<String>> byDay = new HashMap<>();
        for (CoupleAnswer row : answerMapper.findBySpace(space.getId())) {
            byDay.computeIfAbsent(row.getAnswerDay(), k -> new HashSet<>()).add(row.getUsername());
        }
        return byDay.values().stream()
                .filter(users -> users.contains(space.getUserA()) && users.contains(space.getUserB()))
                .count();
    }

    /** 纪念日在查询区间内（startDate ~ 今天）的落位日期：yearly 取本年度，非 yearly 取当年；不在区间返回 null。 */
    private LocalDate anniversaryOccurrence(CoupleAnniversary row, LocalDate startDate) {
        LocalDate date;
        try {
            date = LocalDate.parse(row.getEventDate());
        } catch (Exception e) {
            return null;
        }
        LocalDate today = LocalDate.now();
        if (row.isYearly() && date.isBefore(startDate)) {
            // 每年重复：落到今年（2/29 在平年跳过）
            try {
                date = date.withYear(today.getYear());
            } catch (Exception e) {
                return null;
            }
        }
        if (date.isBefore(startDate) || date.isAfter(today)) {
            return null;
        }
        return date;
    }

    private CoupleInvite requireInvite(String inviteId) {
        CoupleInvite invite = inviteMapper.selectById(inviteId);
        if (invite == null) {
            throw new BusinessException(404, "邀请不存在");
        }
        return invite;
    }

    private CouplePromise requirePromise(String promiseId, CoupleSpace space) {
        CouplePromise promise = promiseMapper.selectById(promiseId);
        if (promise == null || !promise.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "约定不存在");
        }
        return promise;
    }

    private CoupleItem requireItem(String itemId, CoupleSpace space) {
        CoupleItem item = itemMapper.selectById(itemId);
        if (item == null || !item.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "清单事项不存在");
        }
        return item;
    }

    private CoupleLetter requireLetter(String letterId, CoupleSpace space) {
        CoupleLetter letter = letterMapper.selectById(letterId);
        if (letter == null || !letter.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这封信不存在");
        }
        return letter;
    }

    private CouplePact requirePact(String pactId, CoupleSpace space) {
        CouplePact pact = pactMapper.selectById(pactId);
        if (pact == null || !pact.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这条条约不存在");
        }
        return pact;
    }

    private CoupleFund requireFund(String fundId, CoupleSpace space) {
        CoupleFund fund = fundMapper.selectById(fundId);
        if (fund == null || !fund.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这个心愿不存在");
        }
        return fund;
    }

    private PactVO toPactVO(CouplePact pact, String me) {
        return new PactVO(pact.getId(), pact.getContent(), pact.getProposedBy(), pact.getAcceptedBy(),
                pact.getAcceptedAt(), !pact.isAccepted(), pact.getProposedBy().equals(me), pact.getCreated());
    }

    /** 异地恋卡片：双方城市文本 +（都在城市库时）时差与球面距离。 */
    private CityCardVO cityCard(CoupleSpace space, String me) {
        String myName = me.equals(space.getUserA()) ? space.getCityA() : space.getCityB();
        String partnerName = me.equals(space.getUserA()) ? space.getCityB() : space.getCityA();
        var my = CoupleCities.find(myName).orElse(null);
        var partner = CoupleCities.find(partnerName).orElse(null);
        Integer hoursDiff = null;
        Long distanceKm = null;
        String partnerZoneId = null;
        if (my != null && partner != null) {
            hoursDiff = Math.round((float) (CoupleCities.offsetSeconds(partner) - CoupleCities.offsetSeconds(my)) / 3600);
            distanceKm = CoupleCities.distanceKm(my, partner);
            partnerZoneId = partner.zoneId();
        }
        return new CityCardVO(myName, partnerName, hoursDiff, distanceKm, partnerZoneId);
    }

    private FundVO toFundVO(CoupleFund fund, String me) {
        List<FundDepositVO> deposits = fundDepositMapper.findByFund(fund.getId()).stream()
                .map(d -> new FundDepositVO(d.getId(), d.getUsername(), d.getAmount(),
                        d.getNote() == null ? "" : d.getNote(), d.getCreated()))
                .toList();
        int progress = fund.getTargetAmount() <= 0 ? 0
                : (int) Math.min(100, Math.round(fund.getSavedAmount() * 100.0 / fund.getTargetAmount()));
        return new FundVO(fund.getId(), fund.getTitle(), fund.getTargetAmount(), fund.getSavedAmount(),
                fund.getStatus(), fund.isReached(), progress, fund.getCreatedBy(), deposits, fund.getCreated());
    }

    /** 悄悄话 VO：未到点的慢递对非发件人（即收件人）隐藏内容。 */
    private LetterVO toLetterVO(CoupleLetter letter, String viewer, long now) {
        boolean locked = letter.locked(now) && !letter.getSender().equals(viewer);
        return new LetterVO(letter.getId(), letter.getSender(), locked ? null : letter.getContent(),
                letter.getDeliverAt(), letter.getStatus(), letter.getOpenedAt(), letter.locked(now),
                letter.getCreated());
    }

    private SpaceVO toSpaceVO(CoupleSpace space, String me) {
        String partner = space.partnerOf(me);
        UserProfile profile = profileMapper.selectById(partner);
        String nickname = profile == null || profile.getNickname() == null || profile.getNickname().isBlank()
                ? partner : profile.getNickname();
        String avatar = profile == null || profile.getAvatar() == null ? "" : profile.getAvatar();
        PartnerVO partnerVO = new PartnerVO(partner, nickname, avatar, push.isOnline(partner), space.nickOf(partner));
        return new SpaceVO(space.getId(), partnerVO, space.getCreated(), space.getAnniversary(),
                daysTogether(space), space.getSlogan(),
                space.getTheme() == null ? "classic" : space.getTheme(), space.getStickers());
    }

    /** 在一起天数：从纪念日（缺省取建立日）算到今天，含当天（建立当天 = 第 1 天）。 */
    private long daysTogether(CoupleSpace space) {
        LocalDate start;
        try {
            start = LocalDate.parse(space.getAnniversary());
        } catch (Exception e) {
            start = Instant.ofEpochMilli(space.getCreated()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        long days = ChronoUnit.DAYS.between(start, LocalDate.now()) + 1;
        return Math.max(days, 1);
    }

    private CheckinStateVO checkinState(CoupleSpace space, String me) {
        String day = today();
        String partner = space.partnerOf(me);
        CheckinHalf mine = new CheckinHalf(
                checkinMapper.find(space.getId(), me, CoupleCheckin.KIND_MORNING, day).isPresent(),
                checkinMapper.find(space.getId(), me, CoupleCheckin.KIND_NIGHT, day).isPresent());
        CheckinHalf theirs = new CheckinHalf(
                checkinMapper.find(space.getId(), partner, CoupleCheckin.KIND_MORNING, day).isPresent(),
                checkinMapper.find(space.getId(), partner, CoupleCheckin.KIND_NIGHT, day).isPresent());
        return new CheckinStateVO(mine, theirs, nightStreak(space));
    }

    /** 连续天数：双方互道晚安的连续自然天数，从今天往前数（今天还没互道则从昨天开始，避免白天清零）。 */
    private int nightStreak(CoupleSpace space) {
        Set<String> mine = nightDays(space, space.getUserA());
        Set<String> theirs = nightDays(space, space.getUserB());
        LocalDate cursor = LocalDate.now();
        if (!(mine.contains(cursor.toString()) && theirs.contains(cursor.toString()))) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        while (mine.contains(cursor.toString()) && theirs.contains(cursor.toString()) && streak < 3650) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private Set<String> nightDays(CoupleSpace space, String username) {
        Set<String> days = new HashSet<>();
        for (CoupleCheckin row : checkinMapper.findBySpaceAndKind(space.getId(), CoupleCheckin.KIND_NIGHT)) {
            if (row.getUsername().equals(username)) {
                days.add(row.getCheckinDay());
            }
        }
        return days;
    }

    private PromiseVO toPromiseVO(CouplePromise p, long now) {
        return new PromiseVO(p.getId(), p.getPromiser(), p.getCreditor(), p.getContent(), p.getDueAt(),
                p.getStatus(), p.getDoneAt(), p.isOverdue(now), p.getCreated());
    }

    private static ItemVO toItemVO(CoupleItem item) {
        return new ItemVO(item.getId(), item.getKind(), item.getTitle(),
                item.getNote() == null ? "" : item.getNote(), item.getDueDate(), item.isDone(),
                item.getDoneBy(), item.getDoneAt(), item.getCreatedBy(), item.getCreated());
    }

    private static AnniversaryVO toAnniversaryVO(CoupleAnniversary row) {
        return new AnniversaryVO(row.getId(), row.getTitle(), row.getEventDate(), row.isYearly(),
                row.getCreatedBy(), row.getCreated());
    }

    private void validateKind(String kind) {
        if (!CoupleItem.KIND_MOVIE.equals(kind) && !CoupleItem.KIND_FOOD.equals(kind)
                && !CoupleItem.KIND_TRIP.equals(kind) && !CoupleItem.KIND_TODO.equals(kind)) {
            throw new BusinessException(400, "清单类型只支持 MOVIE / FOOD / TRIP / TODO");
        }
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
