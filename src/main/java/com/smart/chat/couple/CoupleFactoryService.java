package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 二人制造厂（F270-F279，批次二十三）：家务轮盘、采买清单、冰箱库存、代拿快递、
 * 叫醒服务、服药提醒链、久坐互拍、垫付本、战利品互猜、家安月检。
 * 情绪价值设计：把「谁干活/谁买米/谁拿快递」这些容易磨感情的碎事，
 * 变成转盘、接单、盖章的小游戏——分工有输赢，付出有感谢章。
 */
@Service
public class CoupleFactoryService {

    static final int SPIN_ITEMS_MAX = 8;
    static final int SPIN_ITEM_LEN = 40;
    static final int SHOP_NAME_LEN = 60;
    static final int PARCEL_NOTE_LEN = 60;
    static final int WAKE_LEN = 60;
    static final int MED_NAME_LEN = 40;
    static final int MED_TIMES_LEN = 60;
    static final int ADVANCE_ITEM_LEN = 60;
    static final int ADVANCE_MAX_CENTS = 100_000_000;
    static final int GROCERY_ITEMS_LEN = 300;
    static final int GROCERY_GUESS_LEN = 300;
    static final long STANDUP_PAIR_WINDOW_MS = 60 * 60 * 1000L;
    static final int STOCK_EXPIRE_WARN_DAYS = 3;
    static final int SHOP_MONTH_LOOKBACK = 0;
    static final int CHECK_MISS_LOOKBACK = 3;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleSpinTaskMapper spinMapper;
    private final CoupleShopItemMapper shopMapper;
    private final CoupleStockMapper stockMapper;
    private final CoupleParcelMapper parcelMapper;
    private final CoupleWakeWordMapper wakeMapper;
    private final CoupleMedicineMapper medMapper;
    private final CoupleStandupMapper standMapper;
    private final CoupleAdvanceMapper advanceMapper;
    private final CoupleGroceryMapper groceryMapper;
    private final CoupleHomeCheckMapper checkMapper;
    private final CouplePointLedgerMapper ledgerMapper;
    private final ImPushService push;

    public CoupleFactoryService(CoupleSpaceMapper spaceMapper, CoupleSpinTaskMapper spinMapper,
                                CoupleShopItemMapper shopMapper, CoupleStockMapper stockMapper,
                                CoupleParcelMapper parcelMapper, CoupleWakeWordMapper wakeMapper,
                                CoupleMedicineMapper medMapper, CoupleStandupMapper standMapper,
                                CoupleAdvanceMapper advanceMapper, CoupleGroceryMapper groceryMapper,
                                CoupleHomeCheckMapper checkMapper, CouplePointLedgerMapper ledgerMapper,
                                ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.spinMapper = spinMapper;
        this.shopMapper = shopMapper;
        this.stockMapper = stockMapper;
        this.parcelMapper = parcelMapper;
        this.wakeMapper = wakeMapper;
        this.medMapper = medMapper;
        this.standMapper = standMapper;
        this.advanceMapper = advanceMapper;
        this.groceryMapper = groceryMapper;
        this.checkMapper = checkMapper;
        this.ledgerMapper = ledgerMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record SpinVO(String id, String week, String item, String assignedUser, boolean mine,
                         boolean confirmed, boolean done) {
    }

    public record ShopVO(String id, String name, String qty, String fromUser, boolean mine, String doneBy) {
    }

    public record StockVO(String id, String item, String qty, String expireDay, boolean mine, boolean expiring) {
    }

    public record ParcelVO(String id, String note, String fromUser, boolean mine, String status, String grabber) {
    }

    public record WakeVO(String fromUser, String content, boolean mine, boolean givenToday) {
    }

    public record MedVO(String id, String name, String times, String fromUser, boolean mine,
                        boolean remindedToday, boolean takenToday, int streak) {
    }

    public record StandVO(boolean mineToday, boolean partnerToday, boolean pairedToday, int weekPairedDays) {
    }

    public record AdvanceVO(String id, String item, String payerUser, int amountCents, String note,
                            boolean mine, long daysOpen) {
    }

    public record GroceryVO(String id, String week, String fromUser, boolean mine, String items,
                            String guess, String guessBy, Integer score) {
    }

    public record CheckVO(String month, String mine, String partner, boolean bothIn) {
    }

    public record BoardVO(String day, String week, String month, List<SpinVO> spins, List<String> owed,
                          String spinLine, List<ShopVO> shop, String shopChampion,
                          List<StockVO> stock, List<String> expiring, List<ParcelVO> parcels,
                          List<WakeVO> wake, List<MedVO> meds, StandVO stand,
                          List<AdvanceVO> advances, int openTotalCents, List<GroceryVO> groceries,
                          CheckVO check, List<String> checkMiss) {
    }

    // ========== 读：本周车间总览 ==========

    /** 二人制造厂总览（十卡一次拉齐）。 */
    public BoardVO board(String me) {
        CoupleSpace space = requireSpace(me);
        LocalDate now = LocalDate.now();
        String day = now.toString();
        String week = monday(now);
        String month = day.substring(0, 7);
        String partner = space.partnerOf(me);

        List<SpinVO> spins = new ArrayList<>();
        for (CoupleSpinTask t : spinMapper.findByWeek(space.getId(), week)) {
            spins.add(new SpinVO(t.getId(), t.getWeek(), t.getItem(), t.getAssignedUser(),
                    t.getAssignedUser().equals(me), t.isConfirmed(), t.isDone()));
        }
        List<String> owed = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            for (CoupleSpinTask t : spinMapper.findByWeek(space.getId(), monday(now.minusWeeks(i)))) {
                if (!t.isDone()) {
                    owed.add(t.getWeek() + " · " + t.getItem() + "（" + t.getAssignedUser() + "）");
                }
            }
        }
        String spinLine = CoupleFactoryBank.spinOpenLine(
                CoupleRitualBank.stableHash(space.getId() + "|spin|" + week));

        List<ShopVO> shop = new ArrayList<>();
        for (CoupleShopItem s : shopMapper.findOpen(space.getId())) {
            shop.add(new ShopVO(s.getId(), s.getName(), s.getQty(), s.getFromUser(),
                    s.getFromUser().equals(me), s.getDoneBy()));
        }
        String shopChampion = shopChampion(space, now);

        List<StockVO> stock = new ArrayList<>();
        List<String> expiring = new ArrayList<>();
        String warnEnd = now.plusDays(STOCK_EXPIRE_WARN_DAYS).toString();
        for (CoupleStock s : stockMapper.findIn(space.getId())) {
            boolean exp = !s.getExpireDay().isEmpty() && s.getExpireDay().compareTo(warnEnd) <= 0;
            stock.add(new StockVO(s.getId(), s.getItem(), s.getQty(), s.getExpireDay(),
                    s.getFromUser().equals(me), exp));
            if (exp) {
                expiring.add(CoupleFactoryBank.expireLine(s.getItem()));
            }
        }

        List<ParcelVO> parcels = new ArrayList<>();
        for (CoupleParcel p : parcelMapper.findOpen(space.getId())) {
            parcels.add(new ParcelVO(p.getId(), p.getNote(), p.getFromUser(), p.getFromUser().equals(me),
                    p.getStatus(), p.getGrabber()));
        }

        List<WakeVO> wake = new ArrayList<>();
        for (CoupleWakeWord w : wakeMapper.findByWeek(space.getId(), week)) {
            wake.add(new WakeVO(w.getFromUser(), w.getContent(), w.getFromUser().equals(me),
                    day.equals(w.getGivenDay())));
        }

        List<MedVO> meds = new ArrayList<>();
        for (CoupleMedicine m : medMapper.findOngoing(space.getId())) {
            meds.add(new MedVO(m.getId(), m.getName(), m.getTimes(), m.getFromUser(),
                    m.getFromUser().equals(me), day.equals(m.getLastRemindDay()),
                    day.equals(m.getLastTakenDay()), m.getStreak() == null ? 0 : m.getStreak()));
        }

        CoupleStandup myTap = standMapper.find(space.getId(), day, me);
        CoupleStandup otherTap = standMapper.find(space.getId(), day, partner);
        boolean pairedToday = myTap != null && otherTap != null
                && Math.abs(myTap.getTappedAt() - otherTap.getTappedAt()) <= STANDUP_PAIR_WINDOW_MS;
        int weekPaired = 0;
        Map<String, CoupleStandup> byDayUser = new HashMap<>();
        for (CoupleStandup s : standMapper.findRecent(space.getId(), week)) {
            byDayUser.put(s.getDay() + "|" + s.getFromUser(), s);
        }
        for (String d : List.of(week, now.plusDays(1).toString(), now.plusDays(2).toString(),
                now.plusDays(3).toString(), now.plusDays(4).toString(),
                now.plusDays(5).toString(), now.plusDays(6).toString())) {
            if (d.compareTo(day) > 0) {
                break;
            }
            CoupleStandup a = byDayUser.get(d + "|" + space.getUserA());
            CoupleStandup b = byDayUser.get(d + "|" + space.getUserB());
            if (a != null && b != null && Math.abs(a.getTappedAt() - b.getTappedAt()) <= STANDUP_PAIR_WINDOW_MS) {
                weekPaired++;
            }
        }
        StandVO stand = new StandVO(myTap != null, otherTap != null, pairedToday, weekPaired);

        List<AdvanceVO> advances = new ArrayList<>();
        int openTotal = 0;
        for (CoupleAdvance a : advanceMapper.findOpen(space.getId())) {
            long days = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.parse(a.getCreated() == null ? day : msToDate(a.getCreated())), now));
            advances.add(new AdvanceVO(a.getId(), a.getItem(), a.getPayerUser(), a.getAmountCents(),
                    a.getNote(), a.getPayerUser().equals(me), days));
            openTotal += a.getAmountCents();
        }

        List<GroceryVO> groceries = new ArrayList<>();
        for (CoupleGrocery g : groceryMapper.findByWeek(space.getId(), week)) {
            groceries.add(new GroceryVO(g.getId(), g.getWeek(), g.getFromUser(), g.getFromUser().equals(me),
                    g.getItems(), g.getGuess(), g.getGuessBy(), g.getScore()));
        }

        CoupleHomeCheck mineC = checkMapper.find(space.getId(), month, me);
        CoupleHomeCheck otherC = checkMapper.find(space.getId(), month, partner);
        CheckVO check = new CheckVO(month, mineC == null ? "" : mineC.getItems(),
                otherC == null ? "" : otherC.getItems(), mineC != null && otherC != null);
        List<String> checkMiss = new ArrayList<>();
        for (int i = CHECK_MISS_LOOKBACK - 1; i >= 0; i--) {
            LocalDate m = now.minusMonths(i);
            String mm = m.toString().substring(0, 7);
            if (checkMapper.findByMonth(space.getId(), mm).size() < 2) {
                checkMiss.add(CoupleFactoryBank.checkMissLine(mm));
            }
        }

        return new BoardVO(day, week, month, spins, owed, spinLine, shop, shopChampion,
                stock, expiring, parcels, wake, meds, stand, advances, openTotal, groceries,
                check, checkMiss);
    }

    // ========== F270 家务轮盘 ==========

    /** 一转定分工：逗号分隔事项 ≤8 条，按 hash 交替分配，一周一转。 */
    public BoardVO spin(String me, String items) {
        CoupleSpace space = requireSpace(me);
        String week = monday(LocalDate.now());
        if (!spinMapper.findByWeek(space.getId(), week).isEmpty()) {
            throw new BusinessException(400, "本周已经转过盘了，下周再来一赌");
        }
        List<String> list = splitItems(items, SPIN_ITEMS_MAX, SPIN_ITEM_LEN);
        if (list.size() < 2) {
            throw new BusinessException(400, "至少写两件事，不然不用转");
        }
        boolean startA = Math.floorMod(CoupleRitualBank.stableHash(space.getId() + "|spin|" + week), 2) == 0;
        String first = startA ? space.getUserA() : space.getUserB();
        String second = startA ? space.getUserB() : space.getUserA();
        for (int i = 0; i < list.size(); i++) {
            spinMapper.insert(CoupleSpinTask.of(space.getId(), week, list.get(i), i % 2 == 0 ? first : second));
        }
        push.pushCoupleEventBoth("factory-spin-open", me, space.getUserA(), space.getUserB(),
                CoupleFactoryBank.spinOpenLine(CoupleRitualBank.stableHash(space.getId() + "|spin|" + week)));
        return board(me);
    }

    /** 对方给天选之人的任务认账（双签生效）。 */
    public BoardVO confirmSpin(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSpinTask row = requireSpin(space, id);
        if (row.getAssignedUser().equals(me)) {
            throw new BusinessException(400, "自己的活自己认，TA 的活等 TA 认");
        }
        if (row.isConfirmed()) {
            return board(me);
        }
        row.setConfirmed(1);
        spinMapper.updateById(row);
        push.pushCoupleEvent("factory-spin-confirm", me, row.getAssignedUser(),
                "「" + row.getItem() + "」对方认账了，就等你干完 ✍️");
        return board(me);
    }

    /** 天选之人干完打勾；本周全干完推 both 清空卡。 */
    public BoardVO doneSpin(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleSpinTask row = requireSpin(space, id);
        if (!row.getAssignedUser().equals(me)) {
            throw new BusinessException(400, "这活不是你的，抢功也得等下周");
        }
        if (row.isDone()) {
            return board(me);
        }
        if (!row.isConfirmed()) {
            throw new BusinessException(400, "先等对方认账，干了也白干");
        }
        row.setDone(1);
        row.setDoneAt(System.currentTimeMillis());
        spinMapper.updateById(row);
        List<CoupleSpinTask> siblings = spinMapper.findByWeek(space.getId(), row.getWeek());
        boolean allDone = siblings.stream().allMatch(CoupleSpinTask::isDone);
        if (allDone) {
            push.pushCoupleEventBoth("factory-spin-clear", me, space.getUserA(), space.getUserB(),
                    "本周家务全部干完，车间熄灯放假 🎉");
        } else {
            push.pushCoupleEvent("factory-spin-item-done", me, space.partnerOf(me),
                    "TA 把「" + row.getItem() + "」干完了，今日份靠谱 +1");
        }
        return board(me);
    }

    // ========== F271 采买清单 ==========

    /** 往超市清单加一项。 */
    public BoardVO shopAdd(String me, String name, String qty) {
        CoupleSpace space = requireSpace(me);
        String n = trim(name, "要买什么总要写吧");
        if (n.length() > SHOP_NAME_LEN) {
            throw new BusinessException(400, "名字最多 60 字");
        }
        String q = qty == null ? "" : qty.trim();
        if (q.length() > 30) {
            throw new BusinessException(400, "数量最多 30 字");
        }
        shopMapper.insert(CoupleShopItem.of(space.getId(), n, q, me));
        return board(me);
    }

    /** 划掉清单一项（谁登记谁划）。 */
    public BoardVO shopRemove(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleShopItem row = requireShop(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "这瓶是 TA 点的，让 TA 自己划");
        }
        shopMapper.deleteById(row.getId());
        return board(me);
    }

    /** 买回来了打勾，推登记人。 */
    public BoardVO shopDone(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleShopItem row = requireShop(space, id);
        if (CoupleShopItem.STATUS_DONE.equals(row.getStatus())) {
            return board(me);
        }
        row.setStatus(CoupleShopItem.STATUS_DONE);
        row.setDoneBy(me);
        row.setDoneAt(System.currentTimeMillis());
        shopMapper.updateById(row);
        if (!row.getFromUser().equals(me)) {
            push.pushCoupleEvent("factory-shop-bought", me, row.getFromUser(),
                    "TA 把「" + row.getName() + "」买回来了 🧺");
        }
        return board(me);
    }

    // ========== F272 冰箱库存 ==========

    /** 入库/补货（同名覆盖复活）。 */
    public BoardVO stockAdd(String me, String item, String qty, String expireDay) {
        CoupleSpace space = requireSpace(me);
        String n = trim(item, "食材名要写");
        if (n.length() > 60) {
            throw new BusinessException(400, "食材名最多 60 字");
        }
        String ed = expireDay == null || expireDay.isBlank() ? "" : expireDay.trim();
        if (!ed.isEmpty()) {
            try {
                LocalDate.parse(ed);
            } catch (DateTimeParseException e) {
                throw new BusinessException(400, "赏味期格式应为 yyyy-MM-dd");
            }
        }
        CoupleStock exist = stockMapper.findItem(space.getId(), n);
        if (exist != null) {
            exist.setQty(qty == null ? "" : qty.trim());
            exist.setPutDay(LocalDate.now().toString());
            exist.setExpireDay(ed);
            exist.setStatus(CoupleStock.STATUS_IN);
            exist.setFromUser(me);
            exist.setUpdatedAt(System.currentTimeMillis());
            stockMapper.updateById(exist);
        } else {
            stockMapper.insert(CoupleStock.of(space.getId(), n, qty, LocalDate.now().toString(), ed, me));
        }
        push.pushCoupleEvent("factory-stock-in", me, space.partnerOf(me), "冰箱补进了「" + n + "」🧊");
        return board(me);
    }

    /** 用完/清掉。 */
    public BoardVO stockOut(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleStock row = id == null ? null : stockMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "冰箱里没有这件");
        }
        if (CoupleStock.STATUS_OUT.equals(row.getStatus())) {
            return board(me);
        }
        row.setStatus(CoupleStock.STATUS_OUT);
        row.setUpdatedAt(System.currentTimeMillis());
        stockMapper.updateById(row);
        push.pushCoupleEvent("factory-stock-out", me, space.partnerOf(me),
                "「" + row.getItem() + "」被我们吃掉了，记得补货");
        return board(me);
    }

    // ========== F273 代拿快递 ==========

    /** 下一单求代拿。 */
    public BoardVO parcelNew(String me, String note) {
        CoupleSpace space = requireSpace(me);
        String n = trim(note, "");
        if (n.length() > PARCEL_NOTE_LEN) {
            throw new BusinessException(400, "描述最多 60 字");
        }
        CoupleParcel row = CoupleParcel.of(space.getId(), n, me);
        parcelMapper.insert(row);
        push.pushCoupleEvent("factory-parcel-new", me, space.partnerOf(me),
                "有快递等领养：" + (n.isEmpty() ? "「" + row.getId().substring(0, 6) + "号件」" : "「" + n + "」") + " 📦");
        return board(me);
    }

    /** 接单侠认领（只能接 TA 的单）。 */
    public BoardVO parcelGrab(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleParcel row = requireParcel(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己接单不算乐于助人，等 TA 来");
        }
        if (!CoupleParcel.STATUS_SENT.equals(row.getStatus())) {
            throw new BusinessException(400, "这单已经被认领了");
        }
        row.setStatus(CoupleParcel.STATUS_GRABBED);
        row.setGrabber(me);
        row.setUpdatedAt(System.currentTimeMillis());
        parcelMapper.updateById(row);
        push.pushCoupleEvent("factory-parcel-grab", me, row.getFromUser(), "你的快递有接单侠了 🏃");
        return board(me);
    }

    /** 送达销单，向积分台账插小额感谢章。 */
    public BoardVO parcelDone(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleParcel row = requireParcel(space, id);
        if (!me.equals(row.getGrabber())) {
            throw new BusinessException(400, "谁领的单谁销单");
        }
        if (CoupleParcel.STATUS_DONE.equals(row.getStatus())) {
            return board(me);
        }
        row.setStatus(CoupleParcel.STATUS_DONE);
        row.setDoneAt(System.currentTimeMillis());
        row.setUpdatedAt(row.getDoneAt());
        parcelMapper.updateById(row);
        ledgerMapper.insert(CouplePointLedger.of(space.getId(), me,
                CouplePointLedger.TYPE_EARN, CoupleFactoryBank.PARCEL_REASON, CoupleFactoryBank.PARCEL_EARN_POINTS));
        push.pushCoupleEventBoth("factory-parcel-done", me, space.getUserA(), space.getUserB(),
                "快递已送达，接单侠喜提 " + CoupleFactoryBank.PARCEL_EARN_POINTS + " 积分感谢章 🏅");
        return board(me);
    }

    // ========== F274 叫醒服务 ==========

    /** 定本周叫醒词（可改写）。 */
    public BoardVO wakeSet(String me, String content) {
        CoupleSpace space = requireSpace(me);
        String week = monday(LocalDate.now());
        String c = trim(content, "叫醒词总要有一句");
        if (c.length() > WAKE_LEN) {
            throw new BusinessException(400, "叫醒词最多 60 字");
        }
        CoupleWakeWord exist = wakeMapper.find(space.getId(), week, me);
        if (exist != null) {
            exist.setContent(c);
            wakeMapper.updateById(exist);
            return board(me);
        }
        wakeMapper.insert(CoupleWakeWord.of(space.getId(), week, me, c));
        push.pushCoupleEvent("factory-wake-set", me, space.partnerOf(me),
                "TA 给你定了本周叫醒词，明早生效 ⏰");
        return board(me);
    }

    /** 递今日叫醒卡（只能递 TA 定的词，一天一张）。 */
    public BoardVO wakeGive(String me) {
        CoupleSpace space = requireSpace(me);
        String week = monday(LocalDate.now());
        String day = LocalDate.now().toString();
        CoupleWakeWord row = wakeMapper.find(space.getId(), week, space.partnerOf(me));
        if (row == null) {
            throw new BusinessException(400, "TA 本周还没定叫醒词");
        }
        if (day.equals(row.getGivenDay())) {
            throw new BusinessException(400, "今天已经叫过了，让 TA 再睡会儿");
        }
        row.setGivenDay(day);
        row.setGivenBy(me);
        wakeMapper.updateById(row);
        push.pushCoupleEvent("factory-wake-given", me, space.partnerOf(me),
                "叫醒卡送达：「" + row.getContent() + "」☀️");
        return board(me);
    }

    // ========== F275 服药提醒链 ==========

    /** 登记在服药物（停药后同名可复活）。 */
    public BoardVO medAdd(String me, String name, String times) {
        CoupleSpace space = requireSpace(me);
        String n = trim(name, "药名要写");
        if (n.length() > MED_NAME_LEN) {
            throw new BusinessException(400, "药名最多 40 字");
        }
        String t = trim(times, "每日时段要写一句（如：早饭后）");
        if (t.length() > MED_TIMES_LEN) {
            throw new BusinessException(400, "时段最多 60 字");
        }
        CoupleMedicine exist = medMapper.find(space.getId(), n, me);
        if (exist != null) {
            if (CoupleMedicine.STATUS_ONGOING.equals(exist.getStatus())) {
                throw new BusinessException(400, "这种药已经登记在服了");
            }
            exist.setStatus(CoupleMedicine.STATUS_ONGOING);
            exist.setTimes(t);
            exist.setStreak(0);
            exist.setLastTakenDay("");
            exist.setLastRemindDay("");
            medMapper.updateById(exist);
        } else {
            medMapper.insert(CoupleMedicine.of(space.getId(), n, t, me));
        }
        push.pushCoupleEvent("factory-med-add", me, space.partnerOf(me),
                "TA 新登记了「" + n + "」，之后每天需要你点一下「提醒了」💊");
        return board(me);
    }

    /** 停服。 */
    public BoardVO medStop(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleMedicine row = requireMed(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "药停了没，本人说了算");
        }
        row.setStatus(CoupleMedicine.STATUS_STOPPED);
        medMapper.updateById(row);
        push.pushCoupleEvent("factory-med-stop", me, space.partnerOf(me), "「" + row.getName() + "」疗程结束，停药 🎉");
        return board(me);
    }

    /** TA 点「提醒了」（一天一次）。 */
    public BoardVO medRemind(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleMedicine row = requireMed(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "提醒是对方做的事");
        }
        String day = LocalDate.now().toString();
        if (day.equals(row.getLastRemindDay())) {
            return board(me);
        }
        row.setLastRemindDay(day);
        medMapper.updateById(row);
        push.pushCoupleEvent("factory-med-remind", me, row.getFromUser(), "TA 提醒你吃「" + row.getName() + "」了 🔔");
        return board(me);
    }

    /** 本人点「吃了」，链 +1（断日重开）。 */
    public BoardVO medTaken(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleMedicine row = requireMed(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "吃没吃，本人来报");
        }
        String day = LocalDate.now().toString();
        if (day.equals(row.getLastTakenDay())) {
            return board(me);
        }
        LocalDate prev = LocalDate.parse(row.getLastTakenDay().isEmpty() ? day : row.getLastTakenDay());
        int streak = prev.plusDays(1).toString().equals(day)
                ? (row.getStreak() == null ? 0 : row.getStreak()) + 1 : 1;
        row.setLastTakenDay(day);
        row.setStreak(streak);
        medMapper.updateById(row);
        push.pushCoupleEvent("factory-med-taken", me, space.partnerOf(me),
                "「" + row.getName() + "」已服下，连续链 " + streak + " 天 🔗");
        return board(me);
    }

    // ========== F276 久坐互拍 ==========

    /** 站起来拍一下；与 TA 间隔≤1h 记同起。 */
    public BoardVO standup(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        CoupleStandup exist = standMapper.find(space.getId(), day, me);
        if (exist != null) {
            throw new BusinessException(400, "今天已经拍过了，别赖在同一格");
        }
        CoupleStandup row = CoupleStandup.of(space.getId(), day, me);
        standMapper.insert(row);
        CoupleStandup other = standMapper.find(space.getId(), day, space.partnerOf(me));
        boolean pairHit = false;
        if (other != null && Math.abs(other.getTappedAt() - row.getTappedAt()) <= STANDUP_PAIR_WINDOW_MS) {
            pairHit = true;
            row.setPaired(1);
            other.setPaired(1);
            standMapper.updateById(row);
            standMapper.updateById(other);
        }
        if (pairHit) {
            push.pushCoupleEventBoth("factory-standup-both", me, space.getUserA(), space.getUserB(),
                    CoupleFactoryBank.STANDUP_BOTH);
        } else {
            push.pushCoupleEvent("factory-standup-tap", me, space.partnerOf(me), "TA 站起来拍了一下，一小时内跟拍就算同起 🧍");
        }
        return board(me);
    }

    // ========== F277 垫付本 ==========

    /** 记一笔垫付。 */
    public BoardVO advanceAdd(String me, String item, Integer amountCents, String note) {
        CoupleSpace space = requireSpace(me);
        String n = trim(item, "名目要写");
        if (n.length() > ADVANCE_ITEM_LEN) {
            throw new BusinessException(400, "名目最多 60 字");
        }
        if (amountCents == null || amountCents <= 0 || amountCents > ADVANCE_MAX_CENTS) {
            throw new BusinessException(400, "金额要填对的整数分（最多 " + (ADVANCE_MAX_CENTS / 100) + " 元）");
        }
        String t = note == null ? "" : note.trim();
        if (t.length() > 140) {
            throw new BusinessException(400, "备注最多 140 字");
        }
        advanceMapper.insert(CoupleAdvance.of(space.getId(), n, me, amountCents, t));
        push.pushCoupleEvent("factory-advance-new", me, space.partnerOf(me),
                "TA 为「" + n + "」垫付了 " + (amountCents / 100.0) + " 元，记得清账");
        return board(me);
    }

    /** 清账（欠的一方点确认已还）。 */
    public BoardVO advanceSettle(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleAdvance row = id == null ? null : advanceMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这笔垫付不存在");
        }
        if (CoupleAdvance.STATUS_SETTLED.equals(row.getStatus())) {
            return board(me);
        }
        if (row.getPayerUser().equals(me)) {
            throw new BusinessException(400, "还钱的一方按确认键，不是你");
        }
        row.setStatus(CoupleAdvance.STATUS_SETTLED);
        row.setSettledAt(System.currentTimeMillis());
        advanceMapper.updateById(row);
        push.pushCoupleEventBoth("factory-advance-settled", me, space.getUserA(), space.getUserB(),
                "「" + row.getItem() + "」" + (row.getAmountCents() / 100.0) + " 元已还清，" + CoupleFactoryBank.ADVANCE_SETTLED);
        return board(me);
    }

    // ========== F278 逛超市战利品 ==========

    /** 本周战利品上報（可改写）。 */
    public BoardVO grocery(String me, String items) {
        CoupleSpace space = requireSpace(me);
        String week = monday(LocalDate.now());
        String it = trim(items, "买了啥总要写");
        if (it.length() > GROCERY_ITEMS_LEN) {
            throw new BusinessException(400, "清单最多 300 字");
        }
        CoupleGrocery exist = groceryMapper.find(space.getId(), week, me);
        if (exist != null) {
            exist.setItems(it);
            exist.setUpdatedAt(System.currentTimeMillis());
            groceryMapper.updateById(exist);
            return board(me);
        }
        groceryMapper.insert(CoupleGrocery.of(space.getId(), week, me, it));
        push.pushCoupleEvent("factory-grocery-new", me, space.partnerOf(me),
                "TA 本周扫了一圈超市，去猜猜为什么买 🛒");
        return board(me);
    }

    /** 猜 TA 的采购动机（一次机会）。 */
    public BoardVO groceryGuess(String me, String id, String guess) {
        CoupleSpace space = requireSpace(me);
        CoupleGrocery row = requireGrocery(space, id);
        if (row.getFromUser().equals(me)) {
            throw new BusinessException(400, "自己买的东西不用猜");
        }
        if (!row.getGuessBy().isEmpty()) {
            throw new BusinessException(400, "猜心只有一次机会，落子无悔");
        }
        String g = trim(guess, "猜测总要写一句");
        if (g.length() > GROCERY_GUESS_LEN) {
            throw new BusinessException(400, "猜测最多 300 字");
        }
        row.setGuess(g);
        row.setGuessBy(me);
        row.setUpdatedAt(System.currentTimeMillis());
        groceryMapper.updateById(row);
        push.pushCoupleEvent("factory-grocery-guess", me, row.getFromUser(), "TA 交了采购动机答卷，去打分 📝");
        return board(me);
    }

    /** 采购方打分 0-5。 */
    public BoardVO groceryRate(String me, String id, Integer score) {
        CoupleSpace space = requireSpace(me);
        CoupleGrocery row = requireGrocery(space, id);
        if (!row.getFromUser().equals(me)) {
            throw new BusinessException(400, "只有买家能给猜的心打分");
        }
        if (row.getGuessBy().isEmpty()) {
            throw new BusinessException(400, "TA 还没交猜测");
        }
        if (row.getScore() != null) {
            return board(me);
        }
        int s = Math.max(0, Math.min(5, score == null ? 0 : score));
        row.setScore(s);
        row.setUpdatedAt(System.currentTimeMillis());
        groceryMapper.updateById(row);
        push.pushCoupleEventBoth("factory-grocery-score", me, space.getUserA(), space.getUserB(),
                "本周采购默契分 " + s + "/5：" + CoupleFactoryBank.groceryScoreLine(s));
        return board(me);
    }

    // ========== F279 家安月检 ==========

    /** 提交本月家安勾选（六项齐全，可改写不重推）。 */
    public BoardVO homeCheck(String me, String items) {
        CoupleSpace space = requireSpace(me);
        String month = LocalDate.now().toString().substring(0, 7);
        List<String> got = splitItems(items, 6, 20);
        if (!got.containsAll(CoupleFactoryBank.CHECK_ORDER) || got.size() != CoupleFactoryBank.CHECK_ORDER.size()) {
            throw new BusinessException(400, "六项都要勾：燃气/水管/插座/门窗/门锁/急救箱");
        }
        CoupleHomeCheck exist = checkMapper.find(space.getId(), month, me);
        if (exist != null) {
            exist.setItems(String.join(",", got));
            exist.setUpdatedAt(System.currentTimeMillis());
            checkMapper.updateById(exist);
            return board(me);
        }
        checkMapper.insert(CoupleHomeCheck.of(space.getId(), month, me, String.join(",", got)));
        String partner = space.partnerOf(me);
        if (checkMapper.find(space.getId(), month, partner) != null) {
            push.pushCoupleEventBoth("factory-homecheck-both", me, space.getUserA(), space.getUserB(),
                    month + " 家安月检双人签齐，这个家稳稳的 🏠");
        } else {
            push.pushCoupleEvent("factory-homecheck-mine", me, partner, "TA 交了这个月的家安月检，等你那份 ✅");
        }
        return board(me);
    }

    // ========== 内部 ==========

    /** 本月买回最多的人头衔（并列=双委员）。 */
    private String shopChampion(CoupleSpace space, LocalDate now) {
        long monthStart = now.withDayOfMonth(1)
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        Map<String, Integer> count = new HashMap<>();
        for (CoupleShopItem s : shopMapper.findDoneSince(space.getId(), monthStart)) {
            if (s.getDoneBy() != null && !s.getDoneBy().isEmpty()) {
                count.merge(s.getDoneBy(), 1, Integer::sum);
            }
        }
        if (count.isEmpty()) {
            return "";
        }
        int max = count.values().stream().max(Integer::compareTo).orElse(0);
        List<String> top = new ArrayList<>(count.entrySet().stream()
                .filter(e -> e.getValue() == max).map(Map.Entry::getKey).toList());
        if (top.size() > 1) {
            return "双委员并列：" + String.join(" & ", top) + "（各 " + max + " 件）";
        }
        return top.get(0) + " · " + CoupleFactoryBank.SHOP_CHAMPION + "（" + max + " 件）";
    }

    private List<String> splitItems(String raw, int max, int lenEach) {
        List<String> out = new ArrayList<>();
        if (raw == null) {
            throw new BusinessException(400, "内容不能为空");
        }
        for (String s : raw.split("[,，、]")) {
            String t = s.trim();
            if (t.isEmpty()) {
                continue;
            }
            if (t.length() > lenEach) {
                throw new BusinessException(400, "每条最多 " + lenEach + " 字");
            }
            if (out.contains(t)) {
                throw new BusinessException(400, "有重复项");
            }
            out.add(t);
        }
        if (out.isEmpty()) {
            throw new BusinessException(400, "至少写一项");
        }
        if (out.size() > max) {
            throw new BusinessException(400, "最多 " + max + " 项，贪多干不完");
        }
        return out;
    }

    private String monday(LocalDate d) {
        return d.with(DayOfWeek.MONDAY).toString();
    }

    private String msToDate(long ms) {
        return java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString();
    }

    private CoupleSpinTask requireSpin(CoupleSpace space, String id) {
        CoupleSpinTask row = id == null ? null : spinMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这条任务不存在");
        }
        return row;
    }

    private CoupleShopItem requireShop(CoupleSpace space, String id) {
        CoupleShopItem row = id == null ? null : shopMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "清单里没有这件");
        }
        return row;
    }

    private CoupleParcel requireParcel(CoupleSpace space, String id) {
        CoupleParcel row = id == null ? null : parcelMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这单快递不存在");
        }
        return row;
    }

    private CoupleMedicine requireMed(CoupleSpace space, String id) {
        CoupleMedicine row = id == null ? null : medMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这条用药记录不存在");
        }
        return row;
    }

    private CoupleGrocery requireGrocery(CoupleSpace space, String id) {
        CoupleGrocery row = id == null ? null : groceryMapper.selectById(id);
        if (row == null || !row.getSpaceId().equals(space.getId())) {
            throw new BusinessException(400, "这份战利品清单不存在");
        }
        return row;
    }

    private String trim(String s, String failMessage) {
        String t = s == null ? "" : s.trim();
        if (t.isEmpty() && !failMessage.isEmpty()) {
            throw new BusinessException(400, failMessage);
        }
        return t;
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
