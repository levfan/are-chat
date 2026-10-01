package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F291 大事册数据访问。 */
@Mapper
public interface CoupleBucketMapper extends BaseMapperCompat<CoupleBucket> {

    /** 同名一件。 */
    default CoupleBucket findName(String spaceId, String name) {
        return selectOne(new LambdaQueryWrapper<CoupleBucket>()
                .eq(CoupleBucket::getSpaceId, spaceId)
                .eq(CoupleBucket::getName, name));
    }

    /** 进行中的大事。 */
    default List<CoupleBucket> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBucket>()
                .eq(CoupleBucket::getSpaceId, spaceId)
                .eq(CoupleBucket::getStatus, CoupleBucket.STATUS_OPEN)
                .orderByAsc(CoupleBucket::getCreated));
    }
}
