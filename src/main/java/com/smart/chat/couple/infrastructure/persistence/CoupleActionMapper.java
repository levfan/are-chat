package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleActionMapper extends BaseMapperCompat<CoupleAction> {

    /** 某空间全部动作流水（新→旧）。 */
    default List<CoupleAction> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleAction>()
                .eq(CoupleAction::getSpaceId, spaceId)
                .orderByDesc(CoupleAction::getCreated));
    }

    /** 某空间某类动作的累计条数。 */
    default long countByKind(String spaceId, String kind) {
        return selectCount(new LambdaQueryWrapper<CoupleAction>()
                .eq(CoupleAction::getSpaceId, spaceId)
                .eq(CoupleAction::getKind, kind));
    }

    /** 某空间某类动作中某人发送的条数。 */
    default long countByKindAndUser(String spaceId, String kind, String username) {
        return selectCount(new LambdaQueryWrapper<CoupleAction>()
                .eq(CoupleAction::getSpaceId, spaceId)
                .eq(CoupleAction::getKind, kind)
                .eq(CoupleAction::getUsername, username));
    }

    /** 某空间某类动作最近一条时间（无记录返回 null）。 */
    default Long lastCreatedAt(String spaceId, String kind) {
        CoupleAction last = selectOne(new LambdaQueryWrapper<CoupleAction>()
                .eq(CoupleAction::getSpaceId, spaceId)
                .eq(CoupleAction::getKind, kind)
                .orderByDesc(CoupleAction::getCreated)
                .last("LIMIT 1"));
        return last == null ? null : last.getCreated();
    }
}
