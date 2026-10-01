package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F294 退休计划数据访问。 */
@Mapper
public interface CoupleRetirePlanMapper extends BaseMapperCompat<CoupleRetirePlan> {

    /** 某人某档一份。 */
    default CoupleRetirePlan find(String spaceId, String ageBand, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleRetirePlan>()
                .eq(CoupleRetirePlan::getSpaceId, spaceId)
                .eq(CoupleRetirePlan::getAgeBand, ageBand)
                .eq(CoupleRetirePlan::getFromUser, fromUser));
    }

    /** 全部计划。 */
    default List<CoupleRetirePlan> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleRetirePlan>()
                .eq(CoupleRetirePlan::getSpaceId, spaceId));
    }
}
