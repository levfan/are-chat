package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CoupleMoodMapper extends BaseMapperCompat<CoupleMood> {

    /** 某人某天的心情（自然日唯一）。 */
    default Optional<CoupleMood> find(String spaceId, String username, String day) {
        return Optional.ofNullable(selectOne(new LambdaQueryWrapper<CoupleMood>()
                .eq(CoupleMood::getSpaceId, spaceId)
                .eq(CoupleMood::getUsername, username)
                .eq(CoupleMood::getMoodDay, day)
                .last("LIMIT 1")));
    }

    /** 某空间全部心情记录（做双人曲线/心动值统计，量级：每天 ≤2 条，可控）。 */
    default List<CoupleMood> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleMood>()
                .eq(CoupleMood::getSpaceId, spaceId));
    }

    /** 84 注销清理。 */
    default void deleteBySpace(String spaceId) {
        delete(new LambdaQueryWrapper<CoupleMood>().eq(CoupleMood::getSpaceId, spaceId));
    }
}
