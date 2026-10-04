package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.content.CoupleRitualBank;
import com.smart.chat.couple.infrastructure.persistence.CoupleDineTicketPO;
import com.smart.chat.couple.infrastructure.persistence.CoupleDineTicketMapper;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 今晚饭桌（保留卡 `couple-dine-today`，原 F210/F211）：每人每天一票，
 * 「今晚吃什么」由当日票池按空间+日稳定 hash 裁决——两个人刷新出来是同一道菜。
 *
 * 系统裁剪：吃过星评、踩雷库、本周菜单、拿手菜、点单机、外卖搭伙车、话题卡、年度干饭账全部下线。
 */
@Service
public class CoupleDiningService {

    private static final int DISH_MAX = 30;
    private static final int TEXT_MAX = 80;

    private final CoupleSpaceRepository spaceRepository;
    private final CoupleDineTicketMapper ticketMapper;
    private final CoupleEventPublisher push;

    public CoupleDiningService(CoupleSpaceRepository spaceRepository, CoupleDineTicketMapper ticketMapper,
                               CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.ticketMapper = ticketMapper;
        this.push = push;
    }

    // ========== VO ==========

    public record TicketVO(String fromUser, boolean mine, String dish, String reason) {
    }

    public record TodayVO(String day, TicketVO mine, TicketVO partner, boolean hit, String verdict) {
    }

    // ========== 今日饭桌 ==========

    /** 今日饭桌总览：双方饭票 + 撞菜判定 + 裁决菜。 */
    public TodayVO today(String me) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        CoupleDineTicketPO mineRow = ticketMapper.find(space.id(), day, me);
        CoupleDineTicketPO partnerRow = ticketMapper.find(space.id(), day, space.partnerOf(me));
        boolean hit = sameDish(mineRow, partnerRow);
        return new TodayVO(day, ticketVO(mineRow, me), ticketVO(partnerRow, me), hit, verdictOf(space, day));
    }

    /** 投今晚饭票：每人每天一票，改票=覆盖；两票撞同一菜自动推双方。 */
    public TodayVO throwTicket(String me, String dish, String reason) {
        CoupleSpace space = requireSpace(me);
        String d = trimLimit(dish, DISH_MAX, "菜名 30 字以内哦");
        if (d == null) {
            throw new BusinessException(400, "先写下今晚想吃什么呀 🍚");
        }
        String r = orEmpty(trimLimit(reason, TEXT_MAX, "理由 80 字以内哦"));
        String day = LocalDate.now().toString();
        CoupleDineTicketPO row = ticketMapper.find(space.id(), day, me);
        if (row == null) {
            ticketMapper.insert(CoupleDineTicketPO.of(space.id(), day, me, d, r));
        } else {
            row.setDish(d);
            row.setReason(r);
            ticketMapper.updateById(row);
        }
        CoupleDineTicketPO partner = ticketMapper.find(space.id(), day, space.partnerOf(me));
        if (partner != null && sameDish(ticketMapper.find(space.id(), day, me), partner)) {
            push.pushCoupleEventBoth("dine-hit", me, space.userA(), space.userB(),
                    "🎯 今晚饭票撞菜啦：你们都投了「" + d + "」，这就是缘分饭桌！");
        } else {
            push.pushCoupleEvent("dine-ticket", me, space.partnerOf(me),
                    "🎫 TA 今晚投了「" + d + "」，你也快投一票，撞上了今晚就吃它！");
        }
        return today(me);
    }

    /** 吃什么裁决：从双方当日饭票（去重）里按空间+日稳定 hash 定一道，两人刷新结果一致。 */
    public String verdictOf(CoupleSpace space, String day) {
        List<String> pool = new ArrayList<>();
        for (CoupleDineTicketPO t : ticketMapper.findByDay(space.id(), day)) {
            if (t.getDish() != null && !t.getDish().isBlank() && !pool.contains(t.getDish())) {
                pool.add(t.getDish());
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        int idx = Math.floorMod(CoupleRitualBank.stableHash(space.id() + "|dine-verdict|" + day), pool.size());
        return pool.get(idx);
    }

    // ========== 内部工具 ==========

    private TicketVO ticketVO(CoupleDineTicketPO row, String me) {
        if (row == null) {
            return null;
        }
        return new TicketVO(row.getFromUser(), me.equals(row.getFromUser()), row.getDish(), row.getReason());
    }

    static boolean sameDish(CoupleDineTicketPO a, CoupleDineTicketPO b) {
        return a != null && b != null && a.getDish() != null && a.getDish().equalsIgnoreCase(b.getDish());
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

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
