package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F329 和平纪念碑数据访问。 */
@Mapper
public interface CouplePeaceLineMapper extends BaseMapperCompat<CouplePeaceLine> {

    /** 空间全部碑文（和好日新→旧）。 */
    default List<CouplePeaceLine> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePeaceLine>()
                .eq(CouplePeaceLine::getSpaceId, spaceId)
                .orderByDesc(CouplePeaceLine::getDay));
    }

    /** 从某天起的碑文（和好日新→旧）。 */
    default List<CouplePeaceLine> findRecent(String spaceId, String fromDay) {
        return selectList(new LambdaQueryWrapper<CouplePeaceLine>()
                .eq(CouplePeaceLine::getSpaceId, spaceId)
                .ge(CouplePeaceLine::getDay, fromDay)
                .orderByDesc(CouplePeaceLine::getDay));
    }
}
