package com.smart.chat.couple.domain.space;

import java.util.List;
import java.util.Optional;

/**
 * 空间聚合的仓储端口：领域只说「我要这个人的有效空间」和「把它存回去」，
 * 怎么查、走哪个 Mapper、哪一列归谁写，都是 infrastructure 的事。
 */
public interface CoupleSpaceRepository {

    /** 该成员当前有效的空间；没有则空 */
    Optional<CoupleSpace> findActiveByMember(String username);

    /** 按 id 取空间（不论有效还是已解散） */
    Optional<CoupleSpace> findById(String id);

    /** 全部有效空间：每日出题、逾期提醒、夜间关怀这类批处理要遍历双方 */
    List<CoupleSpace> findAllActive();

    /** 全部空间（含已解散）：运营看板统计口径用 */
    List<CoupleSpace> findAll();

    /** 新空间落库、已有空间按聚合持有的字段更新（存储里聚合不认识的列一律保持原值） */
    void save(CoupleSpace space);
}
