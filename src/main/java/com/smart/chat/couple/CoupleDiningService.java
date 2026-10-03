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
    private final ImPushService push;

    public CoupleDiningService(CoupleSpaceMapper spaceMapper, CoupleDineTicketMapper ticketMapper,
                               CoupleDineRateMapper rateMapper, CoupleDineNogoMapper nogoMapper,
                               ImPushService push) {
        this.spaceMapper = spaceMapper;
        this.ticketMapper = ticketMapper;
        this.rateMapper = rateMapper;
        this.nogoMapper = nogoMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record TicketVO(String fromUser, boolean mine, String dish, String reason) {
    }

    public record TodayVO(String day, TicketVO mine, TicketVO partner, boolean hit,
                          String verdict) {
    }

    public record RateVO(String id, String day, String dish, Integer stars, String comment,
                         String fromUser, boolean mine, Long created) {
    }

    public record NogoVO(String id, String name, String reason, String fromUser, boolean mine, Long created) {
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
                verdictOf(space, day));
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

    // ========== F215 家常菜搭档 ==========

    // ========== F216 点单机（静态，无表） ==========

    // ========== F217 外卖搭伙车 ==========

    // ========== F219 年度干饭账 ==========

    // ========== 内部工具 ==========

    private TicketVO ticketVO(CoupleDineTicket row, String me, CoupleSpace space) {
        if (row == null) {
            return null;
        }
        return new TicketVO(row.getFromUser(), me.equals(row.getFromUser()), row.getDish(), row.getReason());
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
