package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleHouseRuleMapper extends BaseMapperCompat<CoupleHouseRule> {

    /** 全部条款（按创建正序，宪法就是一部编年史）。 */
    default List<CoupleHouseRule> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleHouseRule>()
                .eq(CoupleHouseRule::getSpaceId, spaceId)
                .orderByAsc(CoupleHouseRule::getCreated));
    }
}
