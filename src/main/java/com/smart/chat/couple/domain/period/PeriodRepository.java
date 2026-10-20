package com.smart.chat.couple.domain.period;

import java.util.List;
import java.util.Optional;

/** 经期记录的仓储端口。 */
public interface PeriodRepository {

    /** 整个空间的记录：开始日倒序（最近的一次在最前） */
    List<PeriodRecord> findBySpace(String spaceId);

    Optional<PeriodRecord> findById(String recordId);

    /** 同空间同开始日是否已经记过（唯一键的前置友好提示） */
    boolean existsByStart(String spaceId, String startDay);

    /** 新建则整行插入；已存在则只回写聚合纳管的结束日 */
    void save(PeriodRecord record);

    void deleteById(String id);
}
