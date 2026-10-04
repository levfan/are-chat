package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CoupleMoodMapper extends BaseMapperCompat<CoupleMoodPO> {

    /** 某人某天的心情（自然日唯一）。 */
    default Optional<CoupleMoodPO> find(String spaceId, String username, String day) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<CoupleMoodPO>()
                .eq(CoupleMoodPO::getSpaceId, spaceId)
                .eq(CoupleMoodPO::getUsername, username)
                .eq(CoupleMoodPO::getMoodDay, day)
                .last("LIMIT 1")));
    }

    /** 某空间全部心情记录（做双人曲线/心动值统计，量级：每天 ≤2 条，可控）。 */
    default List<CoupleMoodPO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleMoodPO>()
                .eq(CoupleMoodPO::getSpaceId, spaceId));
    }

    /** 84 注销清理。 */
    default void deleteBySpace(String spaceId) {
        delete(new LambdaQueryWrapper<CoupleMoodPO>().eq(CoupleMoodPO::getSpaceId, spaceId));
    }
}
