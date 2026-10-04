package com.smart.chat.couple.domain.surprise;

import java.util.List;
import java.util.Optional;

/**
 * 恋爱盲盒的仓储端口。
 * <p>
 * 只有三种说法：列一盒子的盒、按空间认盒号、存回一个盒。
 * 刻意不提供删除与「改盒子里的话」——现役用例没有这两件事，编造出来只会误导后来人。
 */
public interface MysteryBoxRepository {

    /** 空间的盲盒列表，新的在前。 */
    List<MysteryBox> findBySpace(String spaceId);

    /** 只认这张空间里的盒——别人的盒号在这里等同于不存在。 */
    Optional<MysteryBox> findByIdIn(String id, String spaceId);

    /** 存回聚合：新盒整行插入；已存在则<b>只回写拆开这两列</b>（内容、归属、开箱日装好即冻结）。 */
    void save(MysteryBox box);
}
