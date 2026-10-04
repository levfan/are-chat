package com.smart.chat.couple.domain.safeword;

import java.util.List;
import java.util.Optional;

/**
 * 暂停喊停流水的仓储端口。
 * <p>
 * 一天一人一次（uk(space_id, day, user_name)）：{@link #save} 对新喊停整行插入（复盘落空串），
 * 对已存在的记录只回写复盘与更新时间——复盘之外的一切都不动（{@code 05-tactical-playbook.md} 第 2.4 条纪律）。
 */
public interface SafewordUseRepository {

    /** 某人那天是否已经喊过一次停（每日一次闸门的读数）。 */
    Optional<SafewordUse> findBySpaceAndDayAndUser(String spaceId, String user, String day);

    /** 按 id 取那次暂停，且限定属于这个空间（不属于即空，用例据此抛 404）。 */
    Optional<SafewordUse> findByIdInSpace(String spaceId, String id);

    /** 空间全部暂停记录（近的在前，看板用）。 */
    List<SafewordUse> listBySpace(String spaceId);

    /** 存回聚合：id 不存在则新建整行（{@code spaceId} 仅此时用），已存在只回写复盘与更新时间。 */
    void save(String spaceId, SafewordUse use);
}
