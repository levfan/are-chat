package com.smart.chat.couple.domain.dine;

import java.util.List;
import java.util.Optional;

/**
 * 饭票的仓储端口。领域只说「某人那天的票」「某天双方的票池」「把票投回去」。
 * <p>
 * 一人一票由 {@link #save} 的 upsert 落实：{@code (spaceId, day, fromUser)} 命中就覆盖菜名与理由，没命中就新建。
 * 覆盖不报错（改票是允许的），所以这里没有「今天已经投过」这类闸门。
 */
public interface DineTicketRepository {

    /** 某人某天的饭票。 */
    Optional<DineTicket> findBySpaceAndUserAndDay(String spaceId, String user, String day);

    /** 某天双方的饭票（裁决票池用，口径与原 findByDay 一致）。 */
    List<DineTicket> listByDay(String spaceId, String day);

    /** 存回聚合：按 (spaceId, day, fromUser) upsert——没投过整行新建，投过只改写菜名与理由。 */
    void save(DineTicket ticket);
}
