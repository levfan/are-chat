package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSkillSwapMapper extends BaseMapperCompat<CoupleSkillSwap> {

    /** 全部挂牌（按创建倒序）。 */
    default List<CoupleSkillSwap> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSkillSwap>()
                .eq(CoupleSkillSwap::getSpaceId, spaceId)
                .orderByDesc(CoupleSkillSwap::getCreated));
    }
}
