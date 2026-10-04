package com.smart.chat.couple.domain.mood;

import java.util.Optional;

/**
 * 心情回应的仓储端口：只问「我在那天贴了什么」和「把这条贴上去」。
 * <p>
 * 没有列表读法：现役看板只取双方当天各自的那一条（{@code CoupleBondService.moodReactions} 查两次），
 * 不预支没人调用的读法。按 {@code (空间, 心情日, 回应人)} 取行就是 {@code uq_couple_mood_reaction} 的口径，
 * 「每人每天一条、重复提交视为修改」靠它保证。
 */
public interface MoodReactionRepository {

    /** 某人某天贴给对方心情的回应（没贴过就是空）。 */
    Optional<MoodReaction> findBySpaceAndUserOn(String spaceId, String moodDay, String fromUser);

    /** 存回聚合：新行整行插入；已存在则<b>只回写聚合纳管的列</b>（回应与修改时刻）。 */
    void save(MoodReaction reaction);
}
