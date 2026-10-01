package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F232 过法任务卡数据访问。 */
@Mapper
public interface CoupleCeremonyRitualMapper extends BaseMapperCompat<CoupleCeremonyRitual> {

    /** 空间内全部过法卡。 */
    default List<CoupleCeremonyRitual> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCeremonyRitual>()
                .eq(CoupleCeremonyRitual::getSpaceId, spaceId));
    }

    /** 某小日子的过法卡。 */
    default List<CoupleCeremonyRitual> findByFounded(String spaceId, String foundedId) {
        return selectList(new LambdaQueryWrapper<CoupleCeremonyRitual>()
                .eq(CoupleCeremonyRitual::getSpaceId, spaceId)
                .eq(CoupleCeremonyRitual::getFoundedId, foundedId));
    }
}
