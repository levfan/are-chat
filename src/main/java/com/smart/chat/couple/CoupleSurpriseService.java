package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 惊喜与期待（F50-F58）：爱情刮刮乐、恋爱盲盒、心动闹钟、思念速递、藏宝图任务、告白重现。
 * 情绪价值设计：把「我想对 TA 好」变成一个个会准时发生的小惊喜——
 * 券能兑现、盒子有开箱日、闹钟会准时响、思念会在几分钟后的某个时刻突然抵达。
 */
@Service
public class CoupleSurpriseService {

    /** 积分口径：刮刮乐的券面被送券人真兑现了才计分，一次 +5。 */
    static final int SCRATCH_POINTS = 5;
    static final String SCRATCH_REASON_PREFIX = "刮刮乐兑现：";

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleScratchMapper scratchMapper;
    private final CoupleMysteryBoxMapper boxMapper;
    private final CoupleSweetAlarmMapper alarmMapper;
    private final CoupleMissExpressMapper missMapper;
    private final CoupleTreasureMapper treasureMapper;
    private final CoupleConfessionMapper confessionMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final ImPushService push;

    public CoupleSurpriseService(CoupleSpaceMapper spaceMapper, CoupleScratchMapper scratchMapper,
                                 CoupleMysteryBoxMapper boxMapper, CoupleSweetAlarmMapper alarmMapper,
                                 CoupleMissExpressMapper missMapper, CoupleTreasureMapper treasureMapper,
                                 CoupleConfessionMapper confessionMapper, CouplePointLedgerMapper ledgerMapper,
                                 ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.scratchMapper = scratchMapper;
        this.boxMapper = boxMapper;
        this.alarmMapper = alarmMapper;
        this.missMapper = missMapper;
        this.treasureMapper = treasureMapper;
        this.confessionMapper = confessionMapper;
        this.ledgerMapper = ledgerMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record ScratchVO(String id, String weekKey, String fromUser, String prizeKind,
                            /** 未刮开时对收券人隐藏券面 */
                            String prizeText, boolean scratched, boolean redeemed, Long scratchedAt) {
    }

    public record BoxVO(String id, String fromUser, String kind, String content, String openDay,
                        boolean opened, boolean canOpen, Long created) {
    }

    public record AlarmVO(String id, String message, Long fireAt, boolean fired, Long firedAt) {
    }

    /** 思念速递概览：我的记录 + 双方累计思念值 + 在途数量。 */
    public record MissVO(String id, Long deliverAt, boolean delivered, Long deliveredAt) {
    }

    public record MissBoardVO(long myTotal, long partnerTotal, long inTransit, List<MissVO> recent) {
    }

    public record TreasureVO(String id, String fromUser, String taskText,
                             /** 未揭晓时对埋宝人以外的 TA 隐藏 */
                             String prizeText, String status, Long doneAt, Long created) {
    }

    public record ConfessionVO(String id, String content, String confessDay, String createdBy, Long created) {
    }

    // ========== 爱情刮刮乐（F50） ==========

    /** 我的刮刮乐列表（自动补发本周的卡：双方各一张来自对方的券）。 */
    public List<ScratchVO> myScratches(String me) {
        CoupleSpace space = requireSpace(me);
        ensureWeekCards(space, weekKey(LocalDate.now()));
        return scratchMapper.findByOwner(space.getId(), me).stream()
                .map(c -> toScratchVO(c, me))
                .toList();
    }

    /** 刮开我的券：只有收券人能刮，刮开后券面对双方可见。 */
    public ScratchVO scratch(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleScratch card = scratchMapper.selectById(id);
        if (card == null || !card.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这张刮刮乐哦");
        }
        if (!card.getOwner().equals(me)) {
            throw new BusinessException(403, "这是 TA 的刮刮乐，不能替 TA 刮哦");
        }
        if (card.isScratched()) {
            return toScratchVO(card, me);
        }
        card.setScratched(true);
        card.setScratchedAt(System.currentTimeMillis());
        scratchMapper.updateById(card);
        push.pushCoupleEvent("scratch-scratched", me, card.getFromUser(),
                "TA 刮开了你送的刮刮乐，抽中了「" + card.getPrizeText() + "」🎟️ 快准备兑现吧！");
        return toScratchVO(card, me);
    }

    /** 核销：只有送券人能点「已兑现」，让承诺闭环。 */
    public ScratchVO redeemScratch(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleScratch card = scratchMapper.selectById(id);
        if (card == null || !card.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这张刮刮乐哦");
        }
        if (!card.getFromUser().equals(me)) {
            throw new BusinessException(403, "只有送券的人才能点核销哦");
        }
        if (!card.isScratched()) {
            throw new BusinessException(400, "TA 还没刮开这张券呢");
        }
        if (card.getRedeemedAt() == null) {
            card.setRedeemedAt(System.currentTimeMillis());
            scratchMapper.updateById(card);
            // 积分重接三个入口之三：券是送的人兑现的，分记在送券人头上；
            // 闸门就是这一层的 redeemedAt==null，重复点核销不会再补分
            ledgerMapper.insert(CouplePointLedger.of(space.getId(), me,
                    CouplePointLedger.TYPE_EARN, SCRATCH_REASON_PREFIX + card.getPrizeText(), SCRATCH_POINTS));
            push.pushCoupleEvent("scratch-redeemed", me, card.getOwner(),
                    "你抽中的「" + card.getPrizeText() + "」已兑现 🎫 承诺 +1，甜度 +1！");
        }
        return toScratchVO(card, card.getOwner());
    }

    /** 本周没有卡就补发：A→B、B→A 各一张，券面按周稳定抽取。 */
    private void ensureWeekCards(CoupleSpace space, String weekKey) {
        List<CoupleScratch> existing = scratchMapper.findByWeek(space.getId(), weekKey);
        for (String owner : List.of(space.getUserA(), space.getUserB())) {
            if (existing.stream().noneMatch(c -> c.getOwner().equals(owner))) {
                String from = space.partnerOf(owner);
                String[] prize = CoupleSurpriseBank.pickScratchPrize(space.getId(), weekKey, owner);
                scratchMapper.insert(CoupleScratch.of(space.getId(), weekKey, from, owner, prize[0], prize[1]));
            }
        }
    }

    /** ISO 周标识（如 2026-W40）。 */
    private String weekKey(LocalDate date) {
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int year = date.get(IsoFields.WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    private ScratchVO toScratchVO(CoupleScratch card, String viewer) {
        // 没刮开时，只有送券人自己能看到券面（其实券面内容双方都能管中窥豹，这里对收券人隐藏以保留惊喜）
        String prizeText = card.isScratched() || card.getFromUser().equals(viewer) ? card.getPrizeText() : null;
        return new ScratchVO(card.getId(), card.getWeekKey(), card.getFromUser(), card.getPrizeKind(),
                prizeText, card.isScratched(), card.getRedeemedAt() != null, card.getScratchedAt());
    }

    // ========== 恋爱盲盒（F51） ==========

    public List<BoxVO> boxes(String me) {
        CoupleSpace space = requireSpace(me);
        return boxMapper.findBySpace(space.getId()).stream()
                .map(b -> toBoxVO(b, me))
                .toList();
    }

    /** 装一个盲盒：最早明天开箱，装好后对方立刻知道「有个盒子在等 TA」。 */
    public BoxVO createBox(String me, String kind, String content, String openDay) {
        if (!CoupleMysteryBox.isValidKind(kind)) {
            throw new BusinessException(400, "盲盒只能是悄悄话或小任务哦");
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(400, "盒子里总要放点什么吧～");
        }
        if (content.length() > CoupleMysteryBox.CONTENT_MAX) {
            throw new BusinessException(400, "盒子太小啦，最多装 " + CoupleMysteryBox.CONTENT_MAX + " 个字");
        }
        LocalDate open;
        try {
            open = LocalDate.parse(openDay);
        } catch (Exception e) {
            throw new BusinessException(400, "开箱日期不认识，选一个明天以后的日子吧");
        }
        if (!open.isAfter(LocalDate.now())) {
            throw new BusinessException(400, "盲盒最早明天才能拆哦，期待感要留足 ✨");
        }
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        CoupleMysteryBox box = CoupleMysteryBox.of(space.getId(), me, kind, content.trim(), open);
        boxMapper.insert(box);
        push.pushCoupleEvent("box-received", me, partner,
                "🎁 TA 给你塞了一个神秘盲盒，" + open.getMonthValue() + " 月 " + open.getDayOfMonth()
                        + " 日开箱！期待值已拉满");
        return toBoxVO(box, me);
    }

    /** 开盲盒：只有 TA 能拆，且要到开箱日。 */
    public BoxVO openBox(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleMysteryBox box = boxMapper.selectById(id);
        if (box == null || !box.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这个盲盒哦");
        }
        if (box.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己装的盒子自己拆，就不惊喜了呀 😝");
        }
        String today = LocalDate.now().toString();
        if (box.getOpenDay().compareTo(today) > 0) {
            long waitDays = LocalDate.parse(box.getOpenDay()).toEpochDay() - LocalDate.now().toEpochDay();
            throw new BusinessException(400, "还没到开箱日！再等 " + waitDays + " 天，让期待多飞一会儿 🎈");
        }
        if (!box.isOpened()) {
            box.setOpened(true);
            box.setOpenedAt(System.currentTimeMillis());
            boxMapper.updateById(box);
            push.pushCoupleEvent("box-opened", me, box.getFromUser(),
                    "TA 拆开了你装的盲盒 🎉 快去看看 TA 的反应吧");
        }
        return toBoxVO(box, me);
    }

    private boolean canOpen(CoupleMysteryBox box, String today, String me) {
        return !box.getFromUser().equals(me) && !box.isOpened() && box.getOpenDay().compareTo(today) <= 0;
    }

    private BoxVO toBoxVO(CoupleMysteryBox box, String viewer) {
        String today = LocalDate.now().toString();
        String content = box.isOpened() || box.getFromUser().equals(viewer) || box.getOpenDay().compareTo(today) <= 0
                ? box.getContent() : null;
        return new BoxVO(box.getId(), box.getFromUser(), box.getKind(), content, box.getOpenDay(),
                box.isOpened(), canOpen(box, today, viewer), box.getCreated());
    }

    // ========== 心动闹钟（F52） ==========

    public List<AlarmVO> alarms(String me) {
        CoupleSpace space = requireSpace(me);
        return alarmMapper.findByUser(space.getId(), me).stream()
                .map(a -> new AlarmVO(a.getId(), a.getMessage(), a.getFireAt(), a.isFired(), a.getFiredAt()))
                .toList();
    }

    /** 设一个心动闹钟：未来 24 小时内的某个时刻，替你说那句话。 */
    public AlarmVO createAlarm(String me, String message, Long fireAt) {
        if (message == null || message.isBlank()) {
            throw new BusinessException(400, "闹钟想说的话不能为空哦");
        }
        if (message.length() > CoupleSweetAlarm.MESSAGE_MAX) {
            throw new BusinessException(400, "一句话就好，最多 " + CoupleSweetAlarm.MESSAGE_MAX + " 个字");
        }
        long now = System.currentTimeMillis();
        if (fireAt == null || fireAt <= now) {
            throw new BusinessException(400, "闹钟时间要定在未来哦");
        }
        if (fireAt - now > CoupleSweetAlarm.HORIZON_MS) {
            throw new BusinessException(400, "心动闹钟最多提前 24 小时设定，惊喜要新鲜的上");
        }
        CoupleSpace space = requireSpace(me);
        CoupleSweetAlarm alarm = CoupleSweetAlarm.of(space.getId(), me, message.trim(), fireAt);
        alarmMapper.insert(alarm);
        return new AlarmVO(alarm.getId(), alarm.getMessage(), alarm.getFireAt(), false, null);
    }

    /** 取消还没响的闹钟。 */
    public void cancelAlarm(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSweetAlarm alarm = alarmMapper.selectById(id);
        if (alarm == null || !alarm.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这个闹钟哦");
        }
        if (!alarm.getFromUser().equals(me)) {
            throw new BusinessException(403, "只能取消自己设的闹钟哦");
        }
        if (alarm.isFired()) {
            throw new BusinessException(400, "已经响过的闹钟不能撤回啦");
        }
        alarmMapper.deleteById(id);
    }

    // ========== 思念速递（F53） ==========

    public MissBoardVO missBoard(String me) {
        CoupleSpace space = requireSpace(me);
        String partner = space.partnerOf(me);
        List<CoupleMissExpress> mine = missMapper.findByUser(space.getId(), me);
        long partnerTotal = missMapper.findByUser(space.getId(), partner).size();
        long inTransit = mine.stream().filter(m -> !m.isDelivered()).count();
        List<MissVO> recent = mine.stream()
                .limit(20)
                .map(m -> new MissVO(m.getId(), m.getDeliverAt(), m.isDelivered(), m.getDeliveredAt()))
                .toList();
        return new MissBoardVO(mine.size(), partnerTotal, inTransit, recent);
    }

    /** 寄出一份思念：5~30 分钟后的随机时刻送达，一次只能有一份在路上。 */
    public MissBoardVO sendMiss(String me) {
        CoupleSpace space = requireSpace(me);
        boolean inTransit = missMapper.findByUser(space.getId(), me).stream()
                .anyMatch(m -> !m.isDelivered());
        if (inTransit) {
            throw new BusinessException(400, "你有一份思念还在路上，先等它送达再寄下一份吧 📮");
        }
        long delay = ThreadLocalRandom.current().nextLong(
                CoupleMissExpress.DELAY_MIN_MS, CoupleMissExpress.DELAY_MAX_MS);
        CoupleMissExpress miss = CoupleMissExpress.of(space.getId(), me, System.currentTimeMillis() + delay);
        missMapper.insert(miss);
        return missBoard(me);
    }

    // ========== 藏宝图任务（F58） ==========

    public List<TreasureVO> treasures(String me) {
        CoupleSpace space = requireSpace(me);
        return treasureMapper.findBySpace(space.getId()).stream()
                .map(t -> toTreasureVO(t, me))
                .toList();
    }

    /** 埋一个宝藏：布置现实小任务 + 藏好奖品；每人同时只能埋一个。 */
    public TreasureVO createTreasure(String me, String taskText, String prizeText) {
        if (taskText == null || taskText.isBlank() || prizeText == null || prizeText.isBlank()) {
            throw new BusinessException(400, "任务和宝藏都要写清楚哦");
        }
        if (taskText.length() > CoupleTreasure.TASK_MAX || prizeText.length() > CoupleTreasure.PRIZE_MAX) {
            throw new BusinessException(400, "任务和宝藏各最多 " + CoupleTreasure.TASK_MAX + " 字");
        }
        CoupleSpace space = requireSpace(me);
        if (treasureMapper.findPendingByUser(space.getId(), me) != null) {
            throw new BusinessException(400, "你还有一个宝藏没被找到，先等 TA 完成吧 🗺️");
        }
        String partner = space.partnerOf(me);
        CoupleTreasure treasure = CoupleTreasure.of(space.getId(), me, taskText.trim(), prizeText.trim());
        treasureMapper.insert(treasure);
        push.pushCoupleEvent("treasure-sent", me, partner,
                "🗺️ TA 给你发了一张藏宝图！完成上面的小任务就能挖到宝藏");
        return toTreasureVO(treasure, me);
    }

    /** 完成任务挖宝：只有 TA 能点，完成后宝藏揭晓。 */
    public TreasureVO completeTreasure(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleTreasure treasure = treasureMapper.selectById(id);
        if (treasure == null || !treasure.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这张藏宝图哦");
        }
        if (treasure.getFromUser().equals(me)) {
            throw new BusinessException(403, "自己埋的宝藏不能自己挖哦");
        }
        if (CoupleTreasure.STATUS_PENDING.equals(treasure.getStatus())) {
            treasure.setStatus(CoupleTreasure.STATUS_DONE);
            treasure.setDoneAt(System.currentTimeMillis());
            treasureMapper.updateById(treasure);
            push.pushCoupleEventBoth("treasure-done", me, space.getUserA(), space.getUserB(),
                    "🏆 宝藏挖到啦！「" + treasure.getPrizeText() + "」已到手，快去兑现吧");
        }
        return toTreasureVO(treasure, me);
    }

    private TreasureVO toTreasureVO(CoupleTreasure treasure, String viewer) {
        boolean revealed = CoupleTreasure.STATUS_DONE.equals(treasure.getStatus())
                || treasure.getFromUser().equals(viewer);
        return new TreasureVO(treasure.getId(), treasure.getFromUser(), treasure.getTaskText(),
                revealed ? treasure.getPrizeText() : null, treasure.getStatus(), treasure.getDoneAt(),
                treasure.getCreated());
    }

    // ========== 告白重现（F57） ==========

    public List<ConfessionVO> confessions(String me) {
        CoupleSpace space = requireSpace(me);
        return confessionMapper.findBySpace(space.getId()).stream()
                .map(c -> new ConfessionVO(c.getId(), c.getContent(), c.getConfessDay(), c.getCreatedBy(), c.getCreated()))
                .toList();
    }

    /** 存下当年的告白：每年这一天，小助手会替你重播一遍。 */
    public ConfessionVO createConfession(String me, String content, String confessDay) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(400, "把当年那句话写下来吧，一字一句都值得");
        }
        if (content.length() > CoupleConfession.CONTENT_MAX) {
            throw new BusinessException(400, "告白最多 " + CoupleConfession.CONTENT_MAX + " 字，精髓要浓缩");
        }
        LocalDate day;
        try {
            day = LocalDate.parse(confessDay);
        } catch (Exception e) {
            throw new BusinessException(400, "告白日期不认识哦");
        }
        if (day.isAfter(LocalDate.now())) {
            throw new BusinessException(400, "告白只能发生在过去，未来的告白先藏心里");
        }
        CoupleSpace space = requireSpace(me);
        CoupleConfession confession = CoupleConfession.of(space.getId(), content.trim(), confessDay, me);
        confessionMapper.insert(confession);
        String monthDay = confessDay.substring(5);
        push.pushCoupleEventBoth("confession-kept", me, space.getUserA(), space.getUserB(),
                "💌 一段告白被永久收藏！每年 " + monthDay + " 它都会被重新读一遍");
        return new ConfessionVO(confession.getId(), confession.getContent(), confession.getConfessDay(),
                me, confession.getCreated());
    }

    public void deleteConfession(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleConfession confession = confessionMapper.selectById(id);
        if (confession == null || !confession.getSpaceId().equals(space.getId())) {
            throw new BusinessException(404, "没有找到这条告白哦");
        }
        if (!confession.getCreatedBy().equals(me)) {
            throw new BusinessException(403, "只有录入的人才能删除哦");
        }
        confessionMapper.deleteById(id);
    }

    /** 定时任务：重播今天的告白（每年 MM-dd 匹配）。 */
    public void replayTodaysConfessions() {
        LocalDate today = LocalDate.now();
        String monthDay = today.format(DateTimeFormatter.ofPattern("MM-dd"));
        List<CoupleConfession> due = new ArrayList<>();
        for (CoupleConfession confession : confessionMapper.findByMonthDay(monthDay)) {
            // 告白当年本身不重播（还没到「重现」的时候）
            if (!confession.replayed(today.getYear())
                    && !confession.getConfessDay().equals(today.toString())) {
                due.add(confession);
            }
        }
        for (CoupleConfession confession : due) {
            CoupleSpace space = spaceMapper.selectById(confession.getSpaceId());
            if (space == null || !CoupleSpace.STATUS_ACTIVE.equals(space.getStatus())) {
                continue;
            }
            long years = today.getYear() - LocalDate.parse(confession.getConfessDay()).getYear();
            String detail = "⏳ 「告白重现」：" + years + " 年前的今天，TA 说过——「" + confession.getContent() + "」"
                    + " 这句话现在听，还是很动人呢 💘";
            push.pushCoupleEventBoth("confession-replay", "system", space.getUserA(), space.getUserB(), detail);
            confession.markReplayed(today.getYear());
            confessionMapper.updateById(confession);
        }
    }

    // ========== 内部工具 ==========

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
