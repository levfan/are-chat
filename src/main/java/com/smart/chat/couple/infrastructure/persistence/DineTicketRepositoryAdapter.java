package com.smart.chat.couple.infrastructure.persistence;

import com.smart.chat.couple.domain.dine.DineTicket;
import com.smart.chat.couple.domain.dine.DineTicketRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link DineTicketRepository} 的 MyBatis-Plus 适配器：PO ↔ 聚合的双向翻译只发生在这里。
 * <p>
 * 查询复用 {@link CoupleDineTicketMapper} 已有的 default 方法，取数口径与改造前逐字相同。
 * {@link #save} 沿用 {@code CoupleSpaceRepositoryAdapter} 的「只回写聚合持有的列」纪律：
 * upsert 命中现有票时只改写 dish / reason（一人一票的改票），id / space_id / day / from_user / created 一律不碰。
 */
@Component
public class DineTicketRepositoryAdapter implements DineTicketRepository {

    private final CoupleDineTicketMapper ticketMapper;

    public DineTicketRepositoryAdapter(CoupleDineTicketMapper ticketMapper) {
        this.ticketMapper = ticketMapper;
    }

    @Override
    public Optional<DineTicket> findBySpaceAndUserAndDay(String spaceId, String user, String day) {
        return Optional.ofNullable(ticketMapper.find(spaceId, day, user)).map(DineTicketRepositoryAdapter::toDomain);
    }

    @Override
    public List<DineTicket> listByDay(String spaceId, String day) {
        return ticketMapper.findByDay(spaceId, day).stream().map(DineTicketRepositoryAdapter::toDomain).toList();
    }

    @Override
    public void save(DineTicket ticket) {
        CoupleDineTicketPO existing = ticketMapper.find(ticket.spaceId(), ticket.day(), ticket.fromUser());
        if (existing == null) {
            ticketMapper.insert(toPo(ticket));
            return;
        }
        applyOwnedFields(existing, ticket);
        ticketMapper.updateById(existing);
    }

    /** 聚合负责维护的列：改票只覆盖菜名与理由。 */
    private static void applyOwnedFields(CoupleDineTicketPO po, DineTicket ticket) {
        po.setDish(ticket.dish());
        po.setReason(ticket.reason());
    }

    private static CoupleDineTicketPO toPo(DineTicket ticket) {
        CoupleDineTicketPO po = new CoupleDineTicketPO();
        po.setId(ticket.id());
        po.setSpaceId(ticket.spaceId());
        po.setDay(ticket.day());
        po.setFromUser(ticket.fromUser());
        po.setDish(ticket.dish());
        po.setReason(ticket.reason());
        po.setCreated(ticket.created());
        return po;
    }

    private static DineTicket toDomain(CoupleDineTicketPO po) {
        return DineTicket.restore(po.getId(), po.getSpaceId(), po.getDay(), po.getFromUser(),
                po.getDish(), po.getReason(), po.getCreated());
    }
}
