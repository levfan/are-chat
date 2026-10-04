package com.smart.chat.couple.domain.safeword;

import java.util.List;
import java.util.Optional;

/**
 * 安全词约定的仓储端口：领域只说「某人约的那个词」「空间里两个词」「把它存回去」。
 * <p>
 * 一人一格（uk(space_id, from_user)）——{@link #save} 是 upsert：这个词没约过就新建，约过就改写词与说明。
 * 走哪个 Mapper、按什么排序都是 infrastructure 的事（见 {@code docs/ddd/05-tactical-playbook.md} 第 2.4 条）。
 */
public interface SafewordRepository {

    /** 某人约定的那个词（喊停前置校验与 upsert 定位用）。 */
    Optional<Safeword> findBySpaceAndUser(String spaceId, String fromUser);

    /** 空间里的全部安全词（按约定先后升序，看板用）。 */
    List<Safeword> listBySpace(String spaceId);

    /** 存回聚合：按 (spaceId, fromUser) upsert，新建整行、已存在只改写词与说明（并刷新更新时间）。 */
    void save(String spaceId, String fromUser, Safeword safeword);
}
