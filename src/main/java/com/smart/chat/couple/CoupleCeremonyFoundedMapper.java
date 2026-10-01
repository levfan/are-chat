package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F230 小日子（建国纪念日）数据访问。 */
@Mapper
public interface CoupleCeremonyFoundedMapper extends BaseMapperCompat<CoupleCeremonyFounded> {

    /** 空间内全部小日子（按起始日聚合）。 */
    default List<CoupleCeremonyFounded> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCeremonyFounded>()
                .eq(CoupleCeremonyFounded::getSpaceId, spaceId));
    }
}
