package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F382 安全词约定数据访问（uk(space_id,from_user) 每人一行，可改写）。 */
@Mapper
public interface CoupleCatchSafewordMapper extends BaseMapperCompat<CoupleCatchSafewordPO> {

    /** 空间两个安全词（与安全词使用记录无关，按约定先后升序）。 */
    default List<CoupleCatchSafewordPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCatchSafewordPO>()
                .eq(CoupleCatchSafewordPO::getSpaceId, spaceId)
                .orderByAsc(CoupleCatchSafewordPO::getCreated));
    }

    /** 某人约定的那个词（upsert 定位用）。 */
    default CoupleCatchSafewordPO find(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCatchSafewordPO>()
                .eq(CoupleCatchSafewordPO::getSpaceId, spaceId)
                .eq(CoupleCatchSafewordPO::getFromUser, fromUser));
    }
}
