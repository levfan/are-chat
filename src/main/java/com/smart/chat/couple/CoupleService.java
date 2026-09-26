package com.smart.chat.couple;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public record PartnerVO(String username, String nickname, String avatar, boolean online) {
    }

    public record SpaceVO(String id, PartnerVO partner, Long created, String anniversary, long days) {
    }

    public record CheckinHalf(boolean morning, boolean night) {
    }

    public record CheckinStateVO(CheckinHalf me, CheckinHalf partner, int streak) {
    }

    public record OverviewVO(SpaceVO space, InviteVO incoming, InviteVO outgoing, CheckinStateVO checkins,
                             long overdueCount) {
    }

    public record PromiseVO(String id, String promiser, String creditor, String content, Long dueAt,
                            String status, Long doneAt, boolean overdue, Long created) {
    }

    public record QuestionVO(String day, String question, String myAnswer, String partnerAnswer) {
    }

    public record ItemVO(String id, String kind, String title, String note, String dueDate, boolean done,
                         String doneBy, Long doneAt, String createdBy, Long created) {
    }

    public record AnniversaryVO(String id, String title, String date, boolean yearly, String createdBy, Long created) {
    }

    // ========== 依赖 ==========

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleInviteMapper inviteMapper;
    private final CouplePromiseMapper promiseMapper;
    private final CoupleCheckinMapper checkinMapper;
    private final CoupleAnswerMapper answerMapper;
    private final CoupleItemMapper itemMapper;
    private final CoupleAnniversaryMapper anniversaryMapper;
    private final FriendMapper friendMapper;
    private final UserProfileMapper profileMapper;
    private final AppUserService userService;
    private final ImPushService push;

    public CoupleService(CoupleSpaceMapper spaceMapper, CoupleInviteMapper inviteMapper,
                         CouplePromiseMapper promiseMapper, CoupleCheckinMapper checkinMapper,
                         CoupleAnswerMapper answerMapper, CoupleItemMapper itemMapper,
                         CoupleAnniversaryMapper anniversaryMapper, FriendMapper friendMapper,
                         UserProfileMapper profileMapper, AppUserService userService, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.inviteMapper = inviteMapper;
        this.promiseMapper = promiseMapper;
        this.checkinMapper = checkinMapper;
        this.answerMapper = answerMapper;
        this.itemMapper = itemMapper;
        this.anniversaryMapper = anniversaryMapper;
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
        List<CoupleInvite> incomingList = inviteMapper.findPendingTo(me);
        List<CoupleInvite> outgoingList = inviteMapper.findPendingFrom(me);
        InviteVO incoming = incomingList.isEmpty() ? null : InviteVO.of(incomingList.get(0));
        InviteVO outgoing = outgoingList.isEmpty() ? null : InviteVO.of(outgoingList.get(0));
        CoupleSpace space = spaceMapper.findActiveByUser(me).orElse(null);
        if (space == null) {
            return new OverviewVO(null, incoming, outgoing, null, 0);
        }
        long now = System.currentTimeMillis();
        // 我还没兑现的逾期承诺数（给「还有 N 件事你没做到哦~」提醒条用）
        long overdue = promiseMapper.findBySpace(space.getId()).stream()
                .filter(p -> p.getPromiser().equals(me) && p.isOverdue(now))
                .count();
        return new OverviewVO(toSpaceVO(space, me), incoming, outgoing, checkinState(space, me), overdue);
    }

    public SpaceVO setAnniversary(String me, String date) {
        CoupleSpace space = requireSpace(me);
        String normalized = normalizeDate(date, "纪念日格式应为 yyyy-MM-dd");
        space.setAnniversary(normalized);
        spaceMapper.updateById(space);
        push.pushCoupleEvent("anniversary-updated", me, space.partnerOf(me), "TA 更新了你们「在一起」的日子 📅");
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
        String question = CoupleQuestions.pick(day);
        String my = answerMapper.find(space.getId(), day, me).map(CoupleAnswer::getAnswer).orElse(null);
        String partnerAnswer = answerMapper.find(space.getId(), day, space.partnerOf(me))
                .map(CoupleAnswer::getAnswer).orElse(null);
        return new QuestionVO(day, question, my, partnerAnswer);
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

    private SpaceVO toSpaceVO(CoupleSpace space, String me) {
        String partner = space.partnerOf(me);
        UserProfile profile = profileMapper.selectById(partner);
        String nickname = profile == null || profile.getNickname() == null || profile.getNickname().isBlank()
                ? partner : profile.getNickname();
        String avatar = profile == null || profile.getAvatar() == null ? "" : profile.getAvatar();
        PartnerVO partnerVO = new PartnerVO(partner, nickname, avatar, push.isOnline(partner));
        return new SpaceVO(space.getId(), partnerVO, space.getCreated(), space.getAnniversary(),
                daysTogether(space));
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
