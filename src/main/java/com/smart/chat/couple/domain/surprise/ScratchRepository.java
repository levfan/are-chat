package com.smart.chat.couple.domain.surprise;

import java.util.List;
import java.util.Optional;

/**
 * 刮刮乐周券的仓储端口。
 * <p>
 * 查询全部复用 {@code CoupleScratchMapper} 已有的 default 方法，所以「谁这周已经有券了」
 * 由 {@link #findByWeek} 交回整周列表，补发判断留在用例——现役懒生成口径就是这个形状。
 * 端口里没有删除：券是承诺的凭证，刮过没刮过都要留痕。
 */
public interface ScratchRepository {

    /** 某周的全部周券（双方各一张，缺哪张补哪张）。 */
    List<Scratch> findByWeek(String spaceId, String weekKey);

    /** 某人自己的周券，新的在前（列表就是「我的刮刮乐」页）。 */
    List<Scratch> findByOwner(String spaceId, String owner);

    /** 只认这张空间里的券——别人的券号在这里等同于不存在。 */
    Optional<Scratch> findByIdIn(String id, String spaceId);

    /** 存回聚合：新券整行插入；已存在则<b>只回写刮开与核销这几列</b>（券面、归属、周锚发布即冻结）。 */
    void save(Scratch card);
}
