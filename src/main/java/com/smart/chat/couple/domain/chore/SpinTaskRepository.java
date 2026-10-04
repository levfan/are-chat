package com.smart.chat.couple.domain.chore;

import java.util.List;
import java.util.Optional;

/**
 * 家务轮盘的仓储端口：一格的「读—签—勾」三种说法，外加一转之前的「这周转过没有」。
 * <p>
 * 端口里不出现 {@code select}/{@code update}/{@code Wrapper}，也不出现 PO；
 * 排序沿用 {@code CoupleSpinTaskMapper.findByWeek} 的现役口径（无 order by，即主键序）。
 * 刻意不提供删除：轮盘没有「擦掉一格」的用例，欠账栏靠读上一周解决。
 */
public interface SpinTaskRepository {

    /** 本周是否已经转过盘（一周一转这条闸要读存储，判定与话术在 {@link SpinTask#draw}）。 */
    boolean alreadySpun(String spaceId, String week);

    /** 某周的全部格子。 */
    List<SpinTask> findByWeek(String spaceId, String week);

    /** 只认这张空间里的格子——别人的格子号在这里等同于不存在。 */
    Optional<SpinTask> findByIdIn(String id, String spaceId);

    /** 存回聚合：新格子整行插入；已存在则<b>只回写双签这几列</b>（事项、分配、周锚、创建时刻发布即冻结）。 */
    void save(SpinTask task);
}
