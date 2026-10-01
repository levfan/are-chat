package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F344 恋爱汇率数据访问。 */
@Mapper
public interface CoupleLegacyFxMapper extends BaseMapperCompat<CoupleLegacyFx> {

    /** 某人报的汇率（uk(space_id,from_user) 保证最多一条）。 */
    default CoupleLegacyFx findByUser(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleLegacyFx>()
                .eq(CoupleLegacyFx::getSpaceId, spaceId)
                .eq(CoupleLegacyFx::getFromUser, fromUser));
    }

    /** 空间两人的汇率（按人排）。 */
    default List<CoupleLegacyFx> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLegacyFx>()
                .eq(CoupleLegacyFx::getSpaceId, spaceId)
                .orderByAsc(CoupleLegacyFx::getFromUser));
    }
}
