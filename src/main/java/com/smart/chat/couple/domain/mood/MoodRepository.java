package com.smart.chat.couple.domain.mood;

import java.util.List;
import java.util.Optional;

/**
 * 心情日记的仓储端口：现役用例只「按天查一个人记没记」「把空间内全部心情拉出来算同步率」。
 * <p>
 * 刻意不提供 save：写入（每人每天一行、当天改写）的用例还在 {@code CoupleService.setMood}，
 * 那一条收口时再往这里加，不在这里预支一个没人调用的写口。
 */
public interface MoodRepository {

    /** 某人某天的那一条心情（每人每空间每天一行，没记就是空）。 */
    Optional<Mood> findBySpaceAndUserOn(String spaceId, String username, String day);

    /** 空间内全部心情记录（双人曲线/情绪同步率从这里算；量级每天 ≤2 条，可控）。 */
    List<Mood> listBySpace(String spaceId);
}
