package com.smart.chat.couple.domain.space;

import java.util.Optional;

/**
 * 空间聚合的仓储端口：领域只说「我要这个人的有效空间」和「把它存回去」，
 * 怎么查、走哪个 Mapper、哪一列归谁写，都是 infrastructure 的事。
 */
public interface CoupleSpaceRepository {

    /** 该成员当前有效的空间；没有则空 */
    Optional<CoupleSpace> findActiveByMember(String username);

    /** 新空间落库、已有空间按聚合持有的字段更新（存储里聚合不认识的列一律保持原值） */
    void save(CoupleSpace space);
}
