package com.smart.chat.couple.domain.comfort;

import java.util.List;
import java.util.Optional;

/**
 * 求抱抱的仓储端口：「某人今天求过没有」「空间里的求助流水」「把这条存回去」。
 * <p>
 * 按 {@code (空间, 人, 天)} 取行是现役口径（{@code uk_comfort_day}）——
 * 「自己不能接」的归属闸门就藏在这里：接住时只会去取<b>对方</b>那一行，取不到自然接不了。
 */
public interface ComfortRepository {

    /** 某人某天的那条求助（每人每天一条）。 */
    Optional<ComfortRequest> findBySpaceAndUserOn(String spaceId, String username, String day);

    /** 空间全部求助（新→旧，看板的历史流按这个顺序截前 20 条）。 */
    List<ComfortRequest> listBySpace(String spaceId);

    /** 存回聚合：新行整行插入；已存在则<b>只回写聚合纳管的列</b>（感受、接住标记、那句话与接住时刻）。 */
    void save(ComfortRequest request);
}
