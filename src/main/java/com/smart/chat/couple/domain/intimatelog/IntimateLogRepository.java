package com.smart.chat.couple.domain.intimatelog;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 亲密记录的仓储端口。 */
public interface IntimateLogRepository {

    /** 整个空间的记录：日期倒序，同一天按记录时刻倒序 */
    List<IntimateLog> findBySpace(String spaceId);

    /** 区间的记录：日期正序、同一天按记录时刻正序，供日历按月铺格子（尾端不含） */
    List<IntimateLog> findInRange(String spaceId, LocalDate startInclusive, LocalDate endExclusive);

    Optional<IntimateLog> findById(String recordId);

    /** 一行一次，只插入；记录写下后不再修改 */
    void save(IntimateLog log);

    void deleteById(String id);
}
