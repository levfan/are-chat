package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 纪念与回忆：恋爱里程碑徽章、恋爱成就系统、那年今天、时光胶囊、倒数日期待清单。
 * 情绪价值设计：把「在一起的日子」变成看得见的勋章；成就系统让每一步都被记住；
 * 那年今天给回忆一个回来的入口；胶囊和倒数日把期待变成具象的等待。
 */
@Service
public class CoupleMemoryService {

    // ========== VO ==========

    /** 里程碑徽章：按在一起天数自动点亮。 */
    public record BadgeVO(String id, String title, String emoji, int targetDays, boolean achieved, int progress) {
    }

    /** 行为成就：由互动数据实时推导。 */
    public record AchievementVO(String id, String title, String desc, String emoji, boolean achieved,
                                int current, int target) {
    }

    public record OnThisDayEvent(String day, String type, String title, String detail, Long at) {
    }

    public record CapsuleVO(String id, String sender, String content, String openDay, String status,
                            Long openedAt, boolean locked, Long remainDays, boolean mine, Long created) {
    }

    public record CountdownVO(String id, String title, String targetDay, String note, boolean done,
                              Long doneAt, Long daysLeft, String createdBy, Long created) {
    }

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleCheckinMapper checkinMapper;
    private final CoupleAnswerMapper answerMapper;
    private final CouplePromiseMapper promiseMapper;
    private final CoupleItemMapper itemMapper;
    private final CoupleAnniversaryMapper anniversaryMapper;
    private final CoupleLetterMapper letterMapper;
    private final CouplePactMapper pactMapper;
    private final CoupleFundMapper fundMapper;
    private final CoupleTacitMapper tacitMapper;
    private final CoupleActionMapper actionMapper;
    private final CouplePraiseMapper praiseMapper;
    private final CoupleReconcileMapper reconcileMapper;
    private final CoupleMoodMapper moodMapper;
    private final CoupleTaskMapper taskMapper;
    private final CoupleCapsuleMapper capsuleMapper;
    private final CoupleCountdownMapper countdownMapper;
    private final CoupleFirstMapper firstMapper;
    private final ImPushService push;

    @SuppressWarnings("java:S107")
    public CoupleMemoryService(CoupleSpaceMapper spaceMapper, CoupleCheckinMapper checkinMapper,
                               CoupleAnswerMapper answerMapper, CouplePromiseMapper promiseMapper,
                               CoupleItemMapper itemMapper, CoupleAnniversaryMapper anniversaryMapper,
                               CoupleLetterMapper letterMapper, CouplePactMapper pactMapper,
                               CoupleFundMapper fundMapper, CoupleTacitMapper tacitMapper,
                               CoupleActionMapper actionMapper, CouplePraiseMapper praiseMapper,
                               CoupleReconcileMapper reconcileMapper, CoupleMoodMapper moodMapper,
                               CoupleTaskMapper taskMapper, CoupleCapsuleMapper capsuleMapper,
                               CoupleCountdownMapper countdownMapper, CoupleFirstMapper firstMapper,
                               ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.checkinMapper = checkinMapper;
        this.answerMapper = answerMapper;
        this.promiseMapper = promiseMapper;
        this.itemMapper = itemMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.letterMapper = letterMapper;
        this.pactMapper = pactMapper;
        this.fundMapper = fundMapper;
        this.tacitMapper = tacitMapper;
        this.actionMapper = actionMapper;
        this.praiseMapper = praiseMapper;
        this.reconcileMapper = reconcileMapper;
        this.moodMapper = moodMapper;
        this.taskMapper = taskMapper;
        this.capsuleMapper = capsuleMapper;
        this.countdownMapper = countdownMapper;
        this.firstMapper = firstMapper;
        this.push = push;
    }

    // ========== F16+F17 徽章墙 ==========

    /** 里程碑徽章（按天数）+ 行为成就，一次全量返回。 */
    public record BadgeWallVO(List<BadgeVO> milestones, List<AchievementVO> achievements,
                              long achievedCount, long total) {
    }

    public BadgeWallVO badgeWall(String me) {
        CoupleSpace space = requireSpace(me);
        long days = daysTogether(space);
        int[] targets = {1, 100, 200, 365, 520, 666, 888, 1000, 1314, 2000};
        String[] emojis = {"🌱", "💯", "💐", "🎂", "💗", "🌈", "🍀", "🏆", "💍", "🛡️"};
        String[] titles = {"在一起", "百日纪念", "200 天", "一周年", "520 天", "666 天", "888 天",
                "1000 天", "1314 天", "2000 天"};
        List<BadgeVO> milestones = new ArrayList<>();
        for (int i = 0; i < targets.length; i++) {
            int target = targets[i];
            boolean achieved = days >= target;
            int progress = achieved ? 100 : (int) Math.min(99, days * 100L / target);
            milestones.add(new BadgeVO("days-" + target, titles[i], emojis[i], target, achieved, progress));
        }
        List<AchievementVO> achievements = buildAchievements(space, me);
        long achieved = milestones.stream().filter(BadgeVO::achieved).count()
                + achievements.stream().filter(AchievementVO::achieved).count();
        return new BadgeWallVO(milestones, achievements, achieved, milestones.size() + achievements.size());
    }

    /** 由现有数据推导全部行为成就（32 个互动维度的浓缩）。 */
    private List<AchievementVO> buildAchievements(CoupleSpace space, String me) {
        String spaceId = space.getId();
        long letters = letterMapper.findBySpace(spaceId).size();
        long morningDays = bothDays(space, CoupleCheckin.KIND_MORNING);
        long nightDays = bothDays(space, CoupleCheckin.KIND_NIGHT);
        long questionDays = bothAnsweredDays(space);
        long promiseDone = promiseMapper.findBySpace(spaceId).stream()
                .filter(p -> CouplePromise.STATUS_DONE.equals(p.getStatus())).count();
        long itemDone = itemMapper.findBySpace(spaceId).stream().filter(CoupleItem::doneFlag).count();
        long pactDone = pactMapper.findBySpace(spaceId).stream().filter(CouplePact::isAccepted).count();
        long fundReached = fundMapper.findBySpace(spaceId).stream().filter(CoupleFund::isReached).count();
        long tacitMatched = tacitMapper.countMatched(spaceId);
        long bondTotal = actionMapper.findBySpace(spaceId).size();
        long missTotal = actionMapper.countByKind(spaceId, CoupleAction.KIND_MISS);
        long praiseReceived = praiseMapper.findBySpace(spaceId).stream()
                .filter(p -> CouplePraise.STATUS_RECEIVED.equals(p.getStatus())).count();
        long reconcileDone = reconcileMapper.countAccepted(spaceId);
        long moodRows = moodMapper.findBySpace(spaceId).size();
        long taskDone = taskMapper.countDone(spaceId);

        List<AchievementVO> list = new ArrayList<>();
        list.add(new AchievementVO("first-letter", "第一封情书", "写下第一封悄悄话", "💌", letters >= 1, (int) letters, 1));
        list.add(new AchievementVO("ten-letters", "笔耕不辍", "悄悄话写到 10 封", "✍️", letters >= 10, (int) letters, 10));
        list.add(new AchievementVO("first-morning", "第一次心动早安", "互道早安 1 天", "🌅", morningDays >= 1, (int) morningDays, 1));
        list.add(new AchievementVO("morning-30", "晨光守护者", "互道早安 30 天", "☀️", morningDays >= 30, (int) morningDays, 30));
        list.add(new AchievementVO("night-7", "七日晚安", "互道晚安 7 天", "🌙", nightDays >= 7, (int) nightDays, 7));
        list.add(new AchievementVO("night-100", "百日晚安", "互道晚安 100 天", "🌜", nightDays >= 100, (int) nightDays, 100));
        list.add(new AchievementVO("question-10", "十日谈", "今日一问拼成 10 天", "💬", questionDays >= 10, (int) questionDays, 10));
        list.add(new AchievementVO("question-50", "灵魂共振", "今日一问拼成 50 天", "🧠", questionDays >= 50, (int) questionDays, 50));
        list.add(new AchievementVO("promise-1", "言出必行", "兑现第一个承诺", "🤙", promiseDone >= 1, (int) promiseDone, 1));
        list.add(new AchievementVO("promise-10", "诚信标兵", "兑现 10 个承诺", "🎖️", promiseDone >= 10, (int) promiseDone, 10));
        list.add(new AchievementVO("item-10", "清单达人", "一起完成 10 件小事", "✅", itemDone >= 10, (int) itemDone, 10));
        list.add(new AchievementVO("tacit-1", "心有灵犀", "默契考验答对 1 次", "🎯", tacitMatched >= 1, (int) tacitMatched, 1));
        list.add(new AchievementVO("tacit-10", "双生花", "默契考验答对 10 次", "💠", tacitMatched >= 10, (int) tacitMatched, 10));
        list.add(new AchievementVO("bond-50", "贴贴达人", "累计贴贴 50 次", "🫶", bondTotal >= 50, (int) bondTotal, 50));
        list.add(new AchievementVO("miss-100", "想念成瘾", "说「在想你」100 次", "🌠", missTotal >= 100, (int) missTotal, 100));
        list.add(new AchievementVO("fund-1", "心愿启航", "达成第一个心愿", "⛵", fundReached >= 1, (int) fundReached, 1));
        list.add(new AchievementVO("fund-3", "攒钱小能手", "达成 3 个心愿", "💰", fundReached >= 3, (int) fundReached, 3));
        list.add(new AchievementVO("praise-10", "夸夸艺术家", "夸夸卡被签收 10 张", "🌟", praiseReceived >= 10, (int) praiseReceived, 10));
        list.add(new AchievementVO("reconcile-1", "和平大使", "第一次和好", "🕊️", reconcileDone >= 1, (int) reconcileDone, 1));
        list.add(new AchievementVO("pact-1", "条约签署人", "第一条恋爱条约生效", "📜", pactDone >= 1, (int) pactDone, 1));
        list.add(new AchievementVO("mood-14", "心情笔友", "心情日记写到 14 条", "📔", moodRows >= 14, (int) moodRows, 14));
        list.add(new AchievementVO("task-10", "甜蜜任务大师", "完成 10 张甜蜜任务卡", "🍬", taskDone >= 10, (int) taskDone, 10));
        return list;
    }

    // ========== F18 那年今天 ==========

    /** 那年今天：全部历史里「同月同日」发生的事（不含今天），按时间倒序。 */
    public List<OnThisDayEvent> onThisDay(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        LocalDate today = LocalDate.now();
        String monthDay = String.format("%02d-%02d", today.getMonthValue(), today.getDayOfMonth());
        List<OnThisDayEvent> events = new ArrayList<>();

        // 空间建立
        LocalDate createdDay = Instant.ofEpochMilli(space.getCreated()).atZone(ZoneId.systemDefault()).toLocalDate();
        addIfMatches(events, createdDay, today, new OnThisDayEvent(createdDay.toString(), "space",
                "我们的情侣空间在这一天建立 💕", null, space.getCreated()));

        // 承诺兑现
        for (CouplePromise p : promiseMapper.findBySpace(space.getId())) {
            if (!CouplePromise.STATUS_DONE.equals(p.getStatus()) || p.getDoneAt() == null) {
                continue;
            }
            LocalDate day = Instant.ofEpochMilli(p.getDoneAt()).atZone(ZoneId.systemDefault()).toLocalDate();
            addIfMatches(events, day, today, new OnThisDayEvent(day.toString(), "promise",
                    "兑现了承诺 🎉：" + p.getContent(), null, p.getDoneAt()));
        }

        // 共享清单完成
        for (CoupleItem item : itemMapper.findBySpace(space.getId())) {
            if (!item.doneFlag() || item.getDoneAt() == null) {
                continue;
            }
            LocalDate day = Instant.ofEpochMilli(item.getDoneAt()).atZone(ZoneId.systemDefault()).toLocalDate();
            addIfMatches(events, day, today, new OnThisDayEvent(day.toString(), "item",
                    "一起完成了 ✅：" + item.getTitle(), null, item.getDoneAt()));
        }

        // 一问完成（双方都答）
        Map<String, Map<String, CoupleAnswer>> byDay = new HashMap<>();
        for (CoupleAnswer row : answerMapper.findBySpace(space.getId())) {
            byDay.computeIfAbsent(row.getAnswerDay(), k -> new HashMap<>()).put(row.getUsername(), row);
        }
        for (Map.Entry<String, Map<String, CoupleAnswer>> entry : byDay.entrySet()) {
            Map<String, CoupleAnswer> users = entry.getValue();
            if (!users.containsKey(space.getUserA()) || !users.containsKey(space.getUserB())) {
                continue;
            }
            LocalDate day;
            try {
                day = LocalDate.parse(entry.getKey());
            } catch (Exception e) {
                continue;
            }
            long at = Math.max(users.get(space.getUserA()).getCreated(), users.get(space.getUserB()).getCreated());
            addIfMatches(events, day, today, new OnThisDayEvent(entry.getKey(), "question",
                    "那天我们回答了今日一问 💬", CoupleQuestions.pick(entry.getKey()).text(), at));
        }

        // 互道早晚安
        for (String kind : new String[]{CoupleCheckin.KIND_MORNING, CoupleCheckin.KIND_NIGHT}) {
            Set<String> mine = new HashSet<>();
            Set<String> theirs = new HashSet<>();
            for (CoupleCheckin row : checkinMapper.findBySpaceAndKind(space.getId(), kind)) {
                (row.getUsername().equals(me) ? mine : theirs).add(row.getCheckinDay());
            }
            boolean morning = CoupleCheckin.KIND_MORNING.equals(kind);
            for (String day : mine) {
                if (!theirs.contains(day)) {
                    continue;
                }
                LocalDate date;
                try {
                    date = LocalDate.parse(day);
                } catch (Exception e) {
                    continue;
                }
                addIfMatches(events, date, today, new OnThisDayEvent(day, "ritual",
                        morning ? "那天我们互道了早安 ☀️" : "那天我们互道了晚安 🌙", null, null));
            }
        }

        // 共同日历纪念日（yearly 的按年回看）
        for (CoupleAnniversary row : anniversaryMapper.findBySpace(space.getId())) {
            if (!row.yearlyFlag()) {
                continue;
            }
            LocalDate date;
            try {
                date = LocalDate.parse(row.getEventDate());
            } catch (Exception e) {
                continue;
            }
            if (date.getMonthValue() == today.getMonthValue() && date.getDayOfMonth() == today.getDayOfMonth()
                    && date.getYear() < today.getYear()) {
                events.add(new OnThisDayEvent(date.toString(), "anniversary",
                        "这一天是我们的「" + row.getTitle() + "」🎊", null, null));
            }
        }

        events.sort((a, b) -> b.day().compareTo(a.day()));
        return events;
    }

    private void addIfMatches(List<OnThisDayEvent> events, LocalDate day, LocalDate today, OnThisDayEvent event) {
        boolean sameMonthDay = day.getMonthValue() == today.getMonthValue()
                && day.getDayOfMonth() == today.getDayOfMonth();
        if (sameMonthDay && day.isBefore(today)) {
            events.add(event);
        }
    }

    // ========== F19 时光胶囊 ==========

    /** 封一枚胶囊：30~365 天后才能开启。 */
    public CapsuleVO sealCapsule(String me, String content, String openDay) {
        CoupleSpace space = requireSpace(me);
        String text = requireText(content, "写下想对未来的 TA 说的话（1-500 字）", CoupleCapsule.CONTENT_MAX);
        LocalDate target;
        try {
            target = LocalDate.parse(openDay.trim());
        } catch (Exception e) {
            throw new BusinessException(400, "开启日期格式应为 yyyy-MM-dd");
        }
        LocalDate today = LocalDate.now();
        long days = ChronoUnit.DAYS.between(today, target);
        if (days < CoupleCapsule.OPEN_MIN_DAYS) {
            throw new BusinessException(400, "胶囊至少封存 30 天，急的话写慢递悄悄话就好～");
        }
        if (days > CoupleCapsule.OPEN_MAX_DAYS) {
            throw new BusinessException(400, "胶囊最多封存 365 天，一年后见 ⏳");
        }
        CoupleCapsule capsule = CoupleCapsule.of(space.getId(), me, space.partnerOf(me), text, target.toString());
        capsuleMapper.insert(capsule);
        push.pushCoupleEvent("capsule-sealed", me, space.partnerOf(me),
                "TA 封存了一枚时光胶囊 ⏳ 到 " + target + " 才能打开，一起等待那一天吧");
        return toCapsuleVO(capsule, me, today);
    }

    /** 胶囊列表（新→旧；未到期对收件人隐藏内容）。 */
    public List<CapsuleVO> listCapsules(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate today = LocalDate.now();
        return capsuleMapper.findBySpace(space.getId()).stream()
                .map(c -> toCapsuleVO(c, me, today))
                .toList();
    }

    /** 开启胶囊：只有收件人能开，且要到点。 */
    public CapsuleVO openCapsule(String me, String capsuleId) {
        CoupleSpace space = requireSpace(me);
        CoupleCapsule capsule = capsuleMapper.selectById(capsuleId);
        if (capsule == null || !capsule.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这枚胶囊不存在");
        }
        if (!capsule.getRecipient().equals(me)) {
            throw new BusinessException(403, "只能由收件人开启哦");
        }
        if (CoupleCapsule.STATUS_OPENED.equals(capsule.getStatus())) {
            throw new BusinessException(409, "这枚胶囊已经开启过了");
        }
        LocalDate today = LocalDate.now();
        if (!capsule.openable(today)) {
            throw new BusinessException(400, "还没到开启的日子，让期待再飞一会儿 ⏳");
        }
        capsule.setStatus(CoupleCapsule.STATUS_OPENED);
        capsule.setOpenedAt(System.currentTimeMillis());
        capsuleMapper.updateById(capsule);
        push.pushCoupleEvent("capsule-opened", me, capsule.getSender(),
                "TA 开启了你封存的时光胶囊 ⏳ 快去看看 TA 现在的反应！");
        return toCapsuleVO(capsule, me, today);
    }

    // ========== F20 倒数日期待清单 ==========

    /** 添加一个倒数日（目标日期必须是今天或未来）。 */
    public CountdownVO addCountdown(String me, String title, String targetDay, String note) {
        CoupleSpace space = requireSpace(me);
        String name = requireText(title, "写下期待的事情（1-60 字）", CoupleCountdown.TITLE_MAX);
        LocalDate target;
        try {
            target = LocalDate.parse(targetDay.trim());
        } catch (Exception e) {
            throw new BusinessException(400, "目标日期格式应为 yyyy-MM-dd");
        }
        if (target.isBefore(LocalDate.now())) {
            throw new BusinessException(400, "期待的事要放在未来呀");
        }
        String memo = requireOptional(note, "备注最多 200 字", CoupleCountdown.NOTE_MAX);
        CoupleCountdown row = CoupleCountdown.of(space.getId(), me, name, target.toString(), memo);
        countdownMapper.insert(row);
        push.pushCoupleEvent("countdown-added", me, space.partnerOf(me),
                "TA 新增了一个期待：「" + name + "」，还有 " + ChronoUnit.DAYS.between(LocalDate.now(), target) + " 天 ⏳");
        return toCountdownVO(row);
    }

    /** 倒数日列表：期待中按日期升序，已实现归档在后。 */
    public List<CountdownVO> listCountdowns(String me) {
        CoupleSpace space = requireSpace(me);
        return countdownMapper.findBySpace(space.getId()).stream()
                .sorted((a, b) -> {
                    if (a.doneFlag() != b.doneFlag()) {
                        return a.doneFlag() ? 1 : -1;
                    }
                    return a.getTargetDay().compareTo(b.getTargetDay());
                })
                .map(CoupleMemoryService::toCountdownVO)
                .toList();
    }

    /** 标记实现/取消实现（归档进回忆）。 */
    public CountdownVO doneCountdown(String me, String countdownId, boolean done) {
        CoupleSpace space = requireSpace(me);
        CoupleCountdown row = requireCountdown(countdownId, space);
        row.setDone(done ? 1 : 0);
        row.setDoneAt(done ? System.currentTimeMillis() : null);
        countdownMapper.updateById(row);
        if (done) {
            push.pushCoupleEventBoth("countdown-done", me, space.getUserA(), space.getUserB(),
                    "期待成真 🎉：「" + row.getTitle() + "」已经实现，已存进你们的回忆里！");
        }
        return toCountdownVO(row);
    }

    /** 删除倒数日（双方都可）。 */
    public void deleteCountdown(String me, String countdownId) {
        CoupleSpace space = requireSpace(me);
        CoupleCountdown row = requireCountdown(countdownId, space);
        countdownMapper.deleteById(row.getId());
    }

    /** 每天由提醒任务调用：倒数 7/3/1/0 天时提醒双方。 */
    public void remindCountdowns() {
        String today = LocalDate.now().toString();
        for (CoupleSpace space : spaceMapper.findAllActive()) {
            for (CoupleCountdown row : countdownMapper.findBySpace(space.getId())) {
                if (row.doneFlag() || today.equals(row.getLastRemindDay())) {
                    continue;
                }
                long days = row.daysLeft(LocalDate.now());
                if (days != 7 && days != 3 && days != 1 && days != 0) {
                    continue;
                }
                row.setLastRemindDay(today);
                countdownMapper.updateById(row);
                String detail = days == 0
                        ? "今天就是「" + row.getTitle() + "」的日子！🎊 好好享受吧"
                        : "距离「" + row.getTitle() + "」还有 " + days + " 天 ⏳ 可以开始准备啦";
                push.pushCoupleEventBoth("countdown-reminder", "system", space.getUserA(), space.getUserB(), detail);
            }
        }
    }

    // ========== F29+F30 恋爱月报 / 数据总览 ==========

    /** 月报单项统计。 */
    public record ReportItem(String key, String label, String emoji, long value, String unit) {
    }

    /** 某个月中「双方都完成某类打卡」的自然日数量。 */
    private long monthBothCheckins(CoupleSpace space, String kind, String month) {
        Set<String> mine = new HashSet<>();
        Set<String> theirs = new HashSet<>();
        for (CoupleCheckin row : checkinMapper.findBySpaceAndKind(space.getId(), kind)) {
            if (row.getCheckinDay() != null && row.getCheckinDay().startsWith(month)) {
                (row.getUsername().equals(space.getUserA()) ? mine : theirs).add(row.getCheckinDay());
            }
        }
        return mine.stream().filter(theirs::contains).count();
    }

    /** 某个月中「双方都回答一问」的自然日数量。 */
    private long monthBothAnswers(CoupleSpace space, String month) {
        Map<String, Set<String>> byDay = new HashMap<>();
        for (CoupleAnswer row : answerMapper.findBySpace(space.getId())) {
            if (row.getAnswerDay() != null && row.getAnswerDay().startsWith(month)) {
                byDay.computeIfAbsent(row.getAnswerDay(), k -> new HashSet<>()).add(row.getUsername());
            }
        }
        return byDay.values().stream()
                .filter(users -> users.contains(space.getUserA()) && users.contains(space.getUserB()))
                .count();
    }

    /** 毫秒时间戳是否落在 yyyy-MM 内。 */
    private boolean inMonth(long at, String month) {
        String day = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate().toString();
        return day.startsWith(month);
    }

    // ========== F46 第一次清单 ==========

    /** 第一次清单条目。 */
    public record FirstVO(String id, String title, String firstDay, String note, String createdBy, Long created) {
    }

    /** 第一次清单：按发生日期升序。 */
    public List<FirstVO> listFirsts(String me) {
        CoupleSpace space = requireSpace(me);
        return firstMapper.findBySpace(space.getId()).stream()
                .map(f -> new FirstVO(f.getId(), f.getTitle(), f.getFirstDay(), f.getNote(), f.getCreatedBy(), f.getCreated()))
                .toList();
    }

    /** 记录一个「我们的第一次」。 */
    public FirstVO createFirst(String me, String title, String firstDay, String note) {
        CoupleSpace space = requireSpace(me);
        String cleanTitle = title == null ? "" : title.trim();
        if (cleanTitle.isEmpty() || cleanTitle.length() > CoupleFirst.TITLE_MAX) {
            throw new BusinessException(400, "写下一个第一次（1-100 字）");
        }
        String cleanDay = firstDay == null ? "" : firstDay.trim();
        try {
            LocalDate.parse(cleanDay);
        } catch (Exception e) {
            throw new BusinessException(400, "日期格式应为 yyyy-MM-dd");
        }
        String cleanNote = note == null ? null : note.trim();
        if (cleanNote != null && cleanNote.isEmpty()) {
            cleanNote = null;
        }
        if (cleanNote != null && cleanNote.length() > CoupleFirst.NOTE_MAX) {
            throw new BusinessException(400, "心情补充最多 300 字");
        }
        CoupleFirst row = CoupleFirst.of(space.getId(), cleanTitle, cleanDay, cleanNote, me);
        firstMapper.insert(row);
        push.pushCoupleEvent("first-added", me, space.partnerOf(me),
                "TA 记下了一个「我们的第一次」：" + cleanTitle + " ✨");
        return new FirstVO(row.getId(), row.getTitle(), row.getFirstDay(), row.getNote(), row.getCreatedBy(), row.getCreated());
    }

    /** 删除第一次（记录人和对方都可以删，空间内共管）。 */
    public void deleteFirst(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleFirst row = firstMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这条记录不存在");
        }
        firstMapper.deleteById(id);
        push.pushCoupleEvent("first-removed", me, space.partnerOf(me), "TA 整理了第一次清单 🧾");
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }

    private CoupleCountdown requireCountdown(String id, CoupleSpace space) {
        CoupleCountdown row = countdownMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "这个倒数日不存在");
        }
        return row;
    }

    private long daysTogether(CoupleSpace space) {
        LocalDate start;
        try {
            start = LocalDate.parse(space.getAnniversary());
        } catch (Exception e) {
            start = java.time.Instant.ofEpochMilli(space.getCreated()).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        long days = ChronoUnit.DAYS.between(start, LocalDate.now()) + 1;
        return Math.max(days, 1);
    }

    /** 双方都完成某类打卡的自然日数量。 */
    private long bothDays(CoupleSpace space, String kind) {
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

    private CapsuleVO toCapsuleVO(CoupleCapsule capsule, String me, LocalDate today) {
        boolean locked = capsule.locked(today) && !capsule.getSender().equals(me);
        long remainDays = Math.max(0, ChronoUnit.DAYS.between(today, LocalDate.parse(capsule.getOpenDay())));
        return new CapsuleVO(capsule.getId(), capsule.getSender(),
                locked ? null : capsule.getContent(), capsule.getOpenDay(), capsule.getStatus(),
                capsule.getOpenedAt(), capsule.locked(today), remainDays, capsule.getSender().equals(me),
                capsule.getCreated());
    }

    private static CountdownVO toCountdownVO(CoupleCountdown row) {
        return new CountdownVO(row.getId(), row.getTitle(), row.getTargetDay(),
                row.getNote() == null ? "" : row.getNote(), row.doneFlag(), row.getDoneAt(),
                row.daysLeft(LocalDate.now()), row.getCreatedBy(), row.getCreated());
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
}
