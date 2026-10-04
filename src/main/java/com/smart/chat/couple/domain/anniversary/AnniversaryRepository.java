package com.smart.chat.couple.domain.anniversary;

import java.util.List;
import java.util.Optional;

/** 共同日历的仓储端口。 */
public interface AnniversaryRepository {

    /** 该空间的全部日子（列表按存储顺序返回，排序口径不变） */
    List<Anniversary> findBySpace(String spaceId);

    Optional<Anniversary> findById(String id);

    /** 新建则插入、已有则整行回写（本聚合镜像 couple_anniversary 的全部列） */
    void save(Anniversary anniversary);

    void deleteById(String id);

    /** 账号注销连带清理整个空间的日历 */
    void deleteBySpace(String spaceId);
}
