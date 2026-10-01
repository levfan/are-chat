package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F297 解梦局数据访问。 */
@Mapper
public interface CoupleDreamCaseMapper extends BaseMapperCompat<CoupleDreamCase> {

    /** 某人某日一梦。 */
    default CoupleDreamCase find(String spaceId, String day, String dreamerUser) {
        return selectOne(new LambdaQueryWrapper<CoupleDreamCase>()
                .eq(CoupleDreamCase::getSpaceId, spaceId)
                .eq(CoupleDreamCase::getDay, day)
                .eq(CoupleDreamCase::getDreamerUser, dreamerUser));
    }

    /** 全部案卷（新→旧）。 */
    default List<CoupleDreamCase> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDreamCase>()
                .eq(CoupleDreamCase::getSpaceId, spaceId)
                .orderByDesc(CoupleDreamCase::getDay));
    }
}
