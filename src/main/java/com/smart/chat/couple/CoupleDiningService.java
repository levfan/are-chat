package com.smart.chat.couple;

import com.smart.chat.common.BusinessException;
import com.smart.chat.im.ImPushService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 两个人的饭桌（F210-F219，批次十七）：今晚饭票、吃什么裁决、吃过星评、踩雷库、本周菜单、
 * 家常菜搭档、点单机、外卖搭伙车、饭桌话题卡、年度干饭账。
 * 情绪价值设计：「今天吃什么」是情侣每天最大的哲学题——把选择题变成一起玩的小游戏，
 * 撞菜是缘分，锁车是默契，星评和踩雷攒出来的是「我们的餐厅档案」。
 */
@Service
public class CoupleDiningService {

    private static final int DISH_MAX = 30;
    private static final int TEXT_MAX = 80;

    private final CoupleSpaceMapper spaceMapper;
    private final CoupleDineTicketMapper ticketMapper;
    private final CoupleDineRateMapper rateMapper;
    private final CoupleDineNogoMapper nogoMapper;
    private final CoupleDineWeekplanMapper planMapper;
    private final CoupleDineHomecookMapper homecookMapper;
    private final CoupleDineCartMapper cartMapper;
    private final CoupleDineTopicMapper topicMapper;
    private final CoupleBodyRedlineMapper redlineMapper;
    private final ImPushService push;

    public CoupleDiningService(CoupleSpaceMapper spaceMapper, CoupleDineTicketMapper ticketMapper,
                               CoupleDineRateMapper rateMapper, CoupleDineNogoMapper nogoMapper,
                               CoupleDineWeekplanMapper planMapper, CoupleDineHomecookMapper homecookMapper,
                               CoupleDineCartMapper cartMapper, CoupleDineTopicMapper topicMapper,
                               CoupleBodyRedlineMapper redlineMapper, ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.ticketMapper = ticketMapper;
        this.rateMapper = rateMapper;
        this.nogoMapper = nogoMapper;
        this.planMapper = planMapper;
        this.homecookMapper = homecookMapper;
        this.cartMapper = cartMapper;
        this.topicMapper = topicMapper;
        this.redlineMapper = redlineMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record TicketVO(String fromUser, boolean mine, String dish, String reason,
                            List<String> redlines) {
    }

    public record TodayVO(String day, TicketVO mine, TicketVO partner, boolean hit,
                          String verdict, String topic, boolean topicMarked) {
    }

    public record RateVO(String id, String day, String dish, Integer stars, String comment,
                         String fromUser, boolean mine, Long created) {
    }

    public record NogoVO(String id, String name, String reason, String fromUser, boolean mine, Long created) {
    }

    public record PlanVO(String day, String dish, String updatedBy, boolean mineLastEdit) {
    }

    public record HomecookVO(String fromUser, boolean mine, String dish, Integer score) {
    }

    public record CartVO(String id, String fromUser, boolean mine, String item, Integer qty,
                         String status, List<String> locked, boolean canLock) {
    }

    public record BoardVO(String week, List<PlanVO> plans, List<HomecookVO> homecooks, List<CartVO> cart) {
    }

    public record DrinkVO(String mood, String emoji, String name, String note) {
    }

    public record DishTopVO(String dish, Integer times, Double avgStars) {
    }

    public record YearVO(String year, Integer rateCount, Double avgStars, List<DishTopVO> topDishes,
                         Integer nogoCount, Integer ticketCount, Integer plannedCount) {
    }

    // ========== F210/F211/F218 今日饭桌 ==========

    /** 今日饭桌总览：双方饭票 + 撞菜判定 + 裁决菜 + 今日话题。 */
    public TodayVO today(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        CoupleDineTicket mineRow = ticketMapper.find(space.getId(), day, me);
        CoupleDineTicket partnerRow = ticketMapper.find(space.getId(), day, space.partnerOf(me));
        boolean hit = sameDish(mineRow, partnerRow);
        return new TodayVO(day, ticketVO(mineRow, me, space), ticketVO(partnerRow, me, space), hit,
                verdictOf(space, day), CoupleDiningBank.topicOf(space.getId(), day),
                topicMapper.find(space.getId(), day) != null);
    }

    /** F210 投今晚饭票：每人每天一票，改票=覆盖；两票撞同一菜自动推双方。 */
    public TodayVO throwTicket(String me, String dish, String reason) {
        CoupleSpace space = requireSpace(me);
        String d = trimLimit(dish, DISH_MAX, "菜名 30 字以内哦");
        if (d == null) {
            throw new BusinessException(400, "先写下今晚想吃什么呀 🍚");
        }
        String r = orEmpty(trimLimit(reason, TEXT_MAX, "理由 80 字以内哦"));
        String day = LocalDate.now().toString();
        CoupleDineTicket row = ticketMapper.find(space.getId(), day, me);
        if (row == null) {
            ticketMapper.insert(CoupleDineTicket.of(space.getId(), day, me, d, r));
        } else {
            row.setDish(d);
            row.setReason(r);
            ticketMapper.updateById(row);
        }
        CoupleDineTicket partner = ticketMapper.find(space.getId(), day, space.partnerOf(me));
        if (partner != null && sameDish(ticketMapper.find(space.getId(), day, me), partner)) {
            push.pushCoupleEventBoth("dine-hit", me, space.getUserA(), space.getUserB(),
                    "🎯 今晚饭票撞菜啦：你们都投了「" + d + "」，这就是缘分饭桌！");
        } else {
            push.pushCoupleEvent("dine-ticket", me, space.partnerOf(me),
                    "🎫 TA 今晚投了「" + d + "」，你也快投一票，撞上了今晚就吃它！");
        }
        return today(me);
    }

    /** F211 吃什么裁决：从双方当日饭票（去重）里按空间+日稳定 hash 定一道，两人刷新结果一致。 */
    public String verdictOf(CoupleSpace space, String day) {
        List<String> pool = new ArrayList<>();
        for (CoupleDineTicket t : ticketMapper.findByDay(space.getId(), day)) {
            if (t.getDish() != null && !t.getDish().isBlank() && !pool.contains(t.getDish())) {
                pool.add(t.getDish());
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        int idx = Math.floorMod(CoupleRitualBank.stableHash(space.getId() + "|dine-verdict|" + day), pool.size());
        return pool.get(idx);
    }

    /** 今天聊这个：双方各自标记「聊过了」，一人标记即打卡。 */
    public TodayVO markTopic(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        if (topicMapper.find(space.getId(), day) == null) {
            topicMapper.insert(CoupleDineTopic.of(space.getId(), day, me));
        }
        return today(me);
    }

    // ========== F212 吃过星评 ==========

    /** 登记一笔星评（1-5 星自动钳位），返回最新流水。 */
    public List<RateVO> rate(String me, String day, String dish, Integer stars, String comment) {
        CoupleSpace space = requireSpace(me);
        String d = normalizeDay(day, "吃的那天要是 2026-10-01 这样的日期哦");
        String dishName = trimLimit(dish, DISH_MAX, "菜名 30 字以内哦");
        if (dishName == null) {
            throw new BusinessException(400, "先写下吃了什么呀 🍜");
        }
        int s = stars == null ? 5 : Math.min(5, Math.max(1, stars));
        rateMapper.insert(CoupleDineRate.of(space.getId(), d, dishName, s,
                orEmpty(trimLimit(comment, TEXT_MAX, "点评 80 字以内哦")), me));
        return rates(me);
    }

    /** 星评流水（最新 30 条）。 */
    public List<RateVO> rates(String me) {
        CoupleSpace space = requireSpace(me);
        return rateMapper.findBySpace(space.getId()).stream().limit(30)
                .map(r -> new RateVO(r.getId(), r.getDay(), r.getDish(), r.getStars(), r.getComment(),
                        r.getFromUser(), me.equals(r.getFromUser()), r.getCreated()))
                .toList();
    }

    // ========== F213 踩雷库 ==========

    public List<NogoVO> nogos(String me) {
        CoupleSpace space = requireSpace(me);
        return nogoMapper.findBySpace(space.getId()).stream()
                .map(n -> new NogoVO(n.getId(), n.getName(), n.getReason(), n.getFromUser(),
                        me.equals(n.getFromUser()), n.getCreated()))
                .toList();
    }

    /** 拉黑一家店：同名不重复收。 */
    public List<NogoVO> addNogo(String me, String name, String reason) {
        CoupleSpace space = requireSpace(me);
        String n = trimLimit(name, DISH_MAX, "店名 30 字以内哦");
        if (n == null) {
            throw new BusinessException(400, "写下店名和避雷理由吧 ⚡");
        }
        if (nogoMapper.find(space.getId(), n) != null) {
            throw new BusinessException(400, "这家已经在踩雷库里啦，别重复拉黑");
        }
        nogoMapper.insert(CoupleDineNogo.of(space.getId(), n,
                orEmpty(trimLimit(reason, TEXT_MAX, "理由 80 字以内哦")), me));
        return nogos(me);
    }

    /** 划掉踩雷：谁提议谁有权划掉。 */
    public List<NogoVO> removeNogo(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleDineNogo row = nogoMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "这条踩雷记录不在了");
        }
        if (!me.equals(row.getFromUser())) {
            throw new BusinessException(400, "谁提议拉黑谁划掉，尊重提议人哦");
        }
        nogoMapper.deleteById(id);
        return nogos(me);
    }

    // ========== F214 本周菜单 ==========

    /** 排/改某一天的正餐；菜名留空 = 擦掉这个格子。day 为本周内某天 yyyy-MM-dd。 */
    public BoardVO setPlan(String me, String day, String dish) {
        CoupleSpace space = requireSpace(me);
        String d = normalizeDay(day, "排餐日要是 2026-10-01 这样的日期哦");
        String week = mondayOf(d);
        String dishName = trimLimit(dish, DISH_MAX, "菜名 30 字以内哦");
        CoupleDineWeekplan row = planMapper.find(space.getId(), week, d);
        if (dishName == null) {
            if (row != null) {
                planMapper.deleteById(row.getId());
            }
            return board(me);
        }
        if (row == null) {
            planMapper.insert(CoupleDineWeekplan.of(space.getId(), week, d, dishName, me));
        } else {
            row.setDish(dishName);
            row.setUpdatedBy(me);
            row.setUpdatedAt(System.currentTimeMillis());
            planMapper.updateById(row);
        }
        return board(me);
    }

    // ========== F215 家常菜搭档 ==========

    /** 本周拿手菜：每人一周一道，改报=覆盖。 */
    public BoardVO reportHomecook(String me, String dish, Integer score) {
        CoupleSpace space = requireSpace(me);
        String d = trimLimit(dish, DISH_MAX, "菜名 30 字以内哦");
        if (d == null) {
            throw new BusinessException(400, "报上你的拿手菜呀 🍳");
        }
        int s = score == null ? 5 : Math.min(5, Math.max(1, score));
        String week = currentWeek();
        CoupleDineHomecook row = homecookMapper.find(space.getId(), week, me);
        if (row == null) {
            homecookMapper.insert(CoupleDineHomecook.of(space.getId(), week, me, d, s));
        } else {
            row.setDish(d);
            row.setScore(s);
            homecookMapper.updateById(row);
        }
        push.pushCoupleEvent("dine-homecook", me, space.partnerOf(me),
                "🍳 TA 这周报上了拿手菜「" + d + "」，配饭指数 " + s + " 星，来凑一桌！");
        return board(me);
    }

    // ========== F216 点单机（静态，无表） ==========

    /** 今日心情选一杯。 */
    public DrinkVO drink(String mood) {
        CoupleDiningBank.Drink dk = CoupleDiningBank.drinkOf(mood);
        return new DrinkVO(dk.mood(), dk.emoji(), dk.name(), dk.note());
    }

    // ========== F217 外卖搭伙车 ==========

    /** 往本周车里加一道菜。 */
    public BoardVO cartAdd(String me, String item, Integer qty) {
        CoupleSpace space = requireSpace(me);
        String it = trimLimit(item, DISH_MAX, "菜品名 30 字以内哦");
        if (it == null) {
            throw new BusinessException(400, "先写下要拼的菜品 🛒");
        }
        int q = qty == null ? 1 : Math.min(20, Math.max(1, qty));
        cartMapper.insert(CoupleDineCart.of(space.getId(), currentWeek(), me, it, q));
        return board(me);
    }

    /** 按锁：双方各锁一次整车才成行。 */
    public BoardVO cartLock(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleDineCart row = cartMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "这道菜不在本周车里");
        }
        if (CoupleDineCart.STATUS_LOCKED.equals(row.getStatus())) {
            return board(me);
        }
        List<String> locked = splitCsv(row.getLockedBy());
        if (!locked.contains(me)) {
            locked.add(me);
        }
        row.setLockedBy(String.join(",", locked));
        if (locked.contains(space.getUserA()) && locked.contains(space.getUserB())) {
            row.setStatus(CoupleDineCart.STATUS_LOCKED);
            push.pushCoupleEventBoth("dine-cart-locked", me, space.getUserA(), space.getUserB(),
                    "🛒 搭伙车「" + row.getItem() + "」双方都按锁了，下单！");
        }
        cartMapper.updateById(row);
        return board(me);
    }

    /** 删自己加的菜（车未锁时）。 */
    public BoardVO cartRemove(String me, String id) {
        CoupleSpace space = requireSpace(me);
        CoupleDineCart row = cartMapper.selectById(id);
        if (row == null || !space.getId().equals(row.getSpaceId())) {
            throw new BusinessException(404, "这道菜不在本周车里");
        }
        if (!me.equals(row.getFromUser())) {
            throw new BusinessException(400, "只能删自己加的菜哦，TA 的菜让 TA 拿掉");
        }
        if (CoupleDineCart.STATUS_LOCKED.equals(row.getStatus())) {
            throw new BusinessException(400, "车已锁定，先和 TA 商量重新拼一车");
        }
        cartMapper.deleteById(id);
        return board(me);
    }

    // ========== F219 年度干饭账 ==========

    /** 今年吃过星评 top、踩雷数、菜单排了多少顿、票投了多少回。 */
    public YearVO yearReport(String me, String year) {
        CoupleSpace space = requireSpace(me);
        String y = year == null || year.isBlank() ? String.valueOf(LocalDate.now().getYear()) : year.trim();
        List<CoupleDineRate> yearRates = rateMapper.findBySpace(space.getId()).stream()
                .filter(r -> r.getDay() != null && r.getDay().startsWith(y)).toList();
        Map<String, List<CoupleDineRate>> byDish = new LinkedHashMap<>();
        double starSum = 0;
        for (CoupleDineRate r : yearRates) {
            byDish.computeIfAbsent(r.getDish(), k -> new ArrayList<>()).add(r);
            starSum += r.getStars() == null ? 0 : r.getStars();
        }
        List<DishTopVO> top = byDish.entrySet().stream()
                .map(e -> new DishTopVO(e.getKey(), e.getValue().size(),
                        Math.round(e.getValue().stream().mapToInt(r -> r.getStars() == null ? 0 : r.getStars()).average().orElse(0) * 10) / 10.0))
                .sorted(Comparator.comparingInt(DishTopVO::times).reversed())
                .limit(5).toList();
        int nogoCount = (int) nogoMapper.findBySpace(space.getId()).stream()
                .filter(n -> n.getCreated() != null && LocalDate.ofInstant(
                        java.time.Instant.ofEpochMilli(n.getCreated()), java.time.ZoneId.systemDefault())
                        .getYear() == parseYear(y)).count();
        int ticketCount = (int) ticketMapper.findBySpace(space.getId()).stream()
                .filter(t -> t.getDay() != null && t.getDay().startsWith(y)).count();
        int plannedCount = planMapper.findByYear(space.getId(), y).size();
        return new YearVO(y, yearRates.size(),
                yearRates.isEmpty() ? 0.0 : Math.round(starSum / yearRates.size() * 10) / 10.0,
                top, nogoCount, ticketCount, plannedCount);
    }

    // ========== 内部工具 ==========

    private TicketVO ticketVO(CoupleDineTicket row, String me, CoupleSpace space) {
        if (row == null) {
            return null;
        }
        return new TicketVO(row.getFromUser(), me.equals(row.getFromUser()), row.getDish(), row.getReason(),
                hitRedlines(space, row.getDish()));
    }

    /** F316 联动：今晚饭票里含忌口红线项的菜名，标出来但不拦（点不点由两个人决定）。 */
    private List<String> hitRedlines(CoupleSpace space, String dish) {
        if (dish == null || dish.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (CoupleBodyRedline r : redlineMapper.findBySpace(space.getId())) {
            if (dish.contains(r.getItem()) && !out.contains(r.getItem())) {
                out.add(r.getItem());
            }
        }
        return out;
    }

    private PlanVO planVO(CoupleDineWeekplan row, String me) {
        return new PlanVO(row.getDay(), row.getDish(), row.getUpdatedBy(), me.equals(row.getUpdatedBy()));
    }

    private HomecookVO homecookVO(CoupleDineHomecook row, String me) {
        return new HomecookVO(row.getFromUser(), me.equals(row.getFromUser()), row.getDish(), row.getScore());
    }

    private CartVO cartVO(CoupleDineCart row, String me) {
        List<String> locked = splitCsv(row.getLockedBy());
        boolean open = !CoupleDineCart.STATUS_LOCKED.equals(row.getStatus());
        return new CartVO(row.getId(), row.getFromUser(), me.equals(row.getFromUser()), row.getItem(),
                row.getQty(), row.getStatus(), locked, open && !locked.contains(me));
    }

    private List<PlanVO> weekPlans(CoupleSpace space, String week, String me) {
        return planMapper.findByWeek(space.getId(), week).stream().map(p -> planVO(p, me)).toList();
    }

    private List<HomecookVO> weekHomecooks(CoupleSpace space, String week, String me) {
        return homecookMapper.findByWeek(space.getId(), week).stream().map(h -> homecookVO(h, me)).toList();
    }

    private List<CartVO> weekCart(CoupleSpace space, String week, String me) {
        return cartMapper.findByWeek(space.getId(), week).stream().map(c -> cartVO(c, me)).toList();
    }

    /** 本周总览：菜单格 + 拿手菜 + 搭伙车。 */
    public BoardVO board(String me) {
        CoupleSpace space = requireSpace(me);
        String week = currentWeek();
        return new BoardVO(week, weekPlans(space, week, me), weekHomecooks(space, week, me), weekCart(space, week, me));
    }

    static boolean sameDish(CoupleDineTicket a, CoupleDineTicket b) {
        return a != null && b != null && a.getDish() != null && a.getDish().equalsIgnoreCase(b.getDish());
    }

    static String currentWeek() {
        return LocalDate.now().with(DayOfWeek.MONDAY).toString();
    }

    static String mondayOf(String day) {
        return LocalDate.parse(day).with(DayOfWeek.MONDAY).toString();
    }

    private static int parseYear(String y) {
        try {
            return Integer.parseInt(y);
        } catch (NumberFormatException e) {
            return LocalDate.now().getYear();
        }
    }

    private static List<String> splitCsv(String csv) {
        List<String> out = new ArrayList<>();
        if (csv != null) {
            for (String s : csv.split(",")) {
                if (!s.isBlank()) {
                    out.add(s.trim());
                }
            }
        }
        return out;
    }

    private static String orEmpty(String v) {
        return v == null ? "" : v;
    }

    private static String trimLimit(String v, int max, String errMsg) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        if (t.isEmpty()) {
            return null;
        }
        if (t.length() > max) {
            throw new BusinessException(400, errMsg);
        }
        return t;
    }

    private static String normalizeDay(String day, String errMsg) {
        if (day == null || day.isBlank()) {
            throw new BusinessException(400, errMsg);
        }
        try {
            return LocalDate.parse(day.trim()).toString();
        } catch (DateTimeParseException e) {
            throw new BusinessException(400, errMsg);
        }
    }

    private CoupleSpace requireSpace(String me) {
        return spaceMapper.findActiveByUser(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
