package com.smart.chat.couple.domain.wish;

import java.util.List;
import java.util.Optional;

/** 愿望清单的仓储端口。 */
public interface WishRepository {

    /** 整个空间的愿望：未实现的在前，同组按记录时刻倒序（排序口径在存储那边） */
    List<Wish> findBySpace(String spaceId);

    Optional<Wish> findById(String wishId);

    /** 同人同空间的同名愿望是否已经存在（重复写一遍没有意义，这是现役口径） */
    boolean sameTitleExists(String spaceId, String ownerUser, String title);

    /** 已实现条数（清单页与回顾页都要用） */
    long countFulfilled(String spaceId);

    long countFulfilledBy(String spaceId, String ownerUser);

    /** 新建则整行插入；已存在则<b>只回写聚合纳管的列</b>（状态、备注、三个时刻） */
    void save(Wish wish);

    void deleteById(String id);
}
