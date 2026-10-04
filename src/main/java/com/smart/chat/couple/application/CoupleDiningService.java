package com.smart.chat.couple.application;

import com.smart.chat.couple.infrastructure.content.CoupleRitualBank;
import com.smart.chat.couple.domain.dine.DineTicket;
import com.smart.chat.couple.domain.dine.DineTicketRepository;
import com.smart.chat.couple.domain.space.CoupleSpace;
import com.smart.chat.couple.domain.space.CoupleSpaceRepository;
import com.smart.chat.sharedkernel.web.BusinessException;
import com.smart.chat.messaging.domain.CoupleEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 今晚饭桌（保留卡 `couple-dine-today`，原 F210/F211）：每人每天一票，
 * 「今晚吃什么」由当日票池按空间+日稳定 hash 裁决——两个人刷新出来是同一道菜。
 *
 * 系统裁剪：吃过星评、踩雷库、本周菜单、拿手菜、点单机、外卖搭伙车、话题卡、年度干饭账全部下线。
 *
 * DDD 收口：内容闸门与撞菜判定沉到 {@link DineTicket}，取数经 {@link DineTicketRepository}；
 * 本类只编排、投影 VO、推 WS。stableHash 裁决留在用例里（领域层不许依赖 infrastructure.content），口径不变。
 */
import static com.smart.chat.couple.application.DomainRules.rule;
@Service
public class CoupleDiningService {

    private final CoupleSpaceRepository spaceRepository;
    private final DineTicketRepository ticketRepository;
    private final CoupleEventPublisher push;

    public CoupleDiningService(CoupleSpaceRepository spaceRepository, DineTicketRepository ticketRepository,
                               CoupleEventPublisher push) {
        this.spaceRepository = spaceRepository;
        this.ticketRepository = ticketRepository;
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
        Optional<DineTicket> mineRow = ticketRepository.findBySpaceAndUserAndDay(space.id(), me, day);
        Optional<DineTicket> partnerRow = ticketRepository.findBySpaceAndUserAndDay(space.id(), space.partnerOf(me), day);
        boolean hit = mineRow.map(t -> t.sameDish(partnerRow.orElse(null))).orElse(false);
        return new TodayVO(day, mineRow.map(t -> ticketVO(t, me)).orElse(null),
                partnerRow.map(t -> ticketVO(t, me)).orElse(null), hit, verdictOf(space, day));
    }

    /** 投今晚饭票：每人每天一票，改票=覆盖；两票撞同一菜自动推双方。 */
    public TodayVO throwTicket(String me, String dish, String reason) {
        CoupleSpace space = requireSpace(me);
        String day = LocalDate.now().toString();
        DineTicket ticket = rule(() -> DineTicket.offer(space.id(), day, me, dish, reason));
        ticketRepository.save(ticket);
        String partner = space.partnerOf(me);
        Optional<DineTicket> partnerTicket = ticketRepository.findBySpaceAndUserAndDay(space.id(), partner, day);
        if (partnerTicket.isPresent() && ticket.sameDish(partnerTicket.get())) {
            push.pushCoupleEventBoth("dine-hit", me, space.userA(), space.userB(),
                    "🎯 今晚饭票撞菜啦：你们都投了「" + ticket.dish() + "」，这就是缘分饭桌！");
        } else {
            push.pushCoupleEvent("dine-ticket", me, partner,
                    "🎫 TA 今晚投了「" + ticket.dish() + "」，你也快投一票，撞上了今晚就吃它！");
        }
        return today(me);
    }

    /** 吃什么裁决：从双方当日饭票（去重）里按空间+日稳定 hash 定一道，两人刷新结果一致。 */
    public String verdictOf(CoupleSpace space, String day) {
        List<String> pool = new ArrayList<>();
        for (DineTicket t : ticketRepository.listByDay(space.id(), day)) {
            if (t.dish() != null && !t.dish().isBlank() && !pool.contains(t.dish())) {
                pool.add(t.dish());
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        int idx = Math.floorMod(CoupleRitualBank.stableHash(space.id() + "|dine-verdict|" + day), pool.size());
        return pool.get(idx);
    }

    // ========== 内部工具 ==========

    private TicketVO ticketVO(DineTicket row, String me) {
        return new TicketVO(row.fromUser(), me.equals(row.fromUser()), row.dish(), row.reason());
    }

    private CoupleSpace requireSpace(String me) {
        return spaceRepository.findActiveByMember(me)
                .orElseThrow(() -> new BusinessException(404, "还没有建立情侣空间，先邀请一位好友吧"));
    }
}
