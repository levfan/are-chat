package com.smart.chat.couple.domain.streak;

import java.util.List;
import java.util.Optional;

/** 贴贴打卡日的仓储端口：一天一行，确认了就只读不写。 */
public interface BondDayRepository {

    /** 全部打卡日，按日子升序（连续段算法要按顺序走） */
    List<BondDay> findBySpace(String spaceId);

    /** 某一天是否已确认打卡 */
    Optional<BondDay> find(String spaceId, String day);

    /** 区间内的补签条数（每月最多 3 次这条上限的读数来源） */
    long countMakeupBetween(String spaceId, String fromDay, String toDay);

    /** 只追加：打卡日一旦确认就不再改，改动等于篡改连续天数的事实 */
    void append(BondDay bondDay);
}
