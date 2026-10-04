package com.smart.chat.couple.domain.quest;

import java.util.List;
import java.util.Optional;

/**
 * 加班预报的仓储端口。领域只说「某人那天的预报」「某天两人的预报」「按 id 取那条」「存回去」。
 * <p>
 * 每人每天一行（uk(space_id,day,from_user)）：{@link #save} 对新预报整行插入，对已存在的行
 * 只回写聚合纳管的列。留灯归属闸门（只有对方能留）在 {@link QuestOvertime#leaveLampBy}，读法在这里按空间过滤。
 */
public interface QuestOvertimeRepository {

    /** 某人某天的预报（预报 upsert 定位用）。 */
    Optional<QuestOvertime> findBySpaceAndUserAndDay(String spaceId, String user, String day);

    /** 按 id 取那条预报，且限定属于这个空间（不属于即空，用例据此抛 404）。 */
    Optional<QuestOvertime> findByIdInSpace(String spaceId, String id);

    /** 某天两人的全部预报（看板读双方视角，口径与原 findByDay 一致）。 */
    List<QuestOvertime> listByDay(String spaceId, String day);

    /** 存回聚合：id 不存在整行插入，已存在只回写 until_hour / note / lamp / lamp_by / updated_at。 */
    void save(QuestOvertime overtime);
}
