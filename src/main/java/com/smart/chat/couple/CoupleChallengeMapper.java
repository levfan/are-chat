package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CoupleChallengeMapper extends BaseMapperCompat<CoupleChallenge> {

    /** 某天的挑战（每天一题）。 */
    default CoupleChallenge find(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleChallenge>()
                .eq(CoupleChallenge::getSpaceId, spaceId)
                .eq(CoupleChallenge::getDay, day));
    }

    /** 空间挑战历史（新→旧）。 */
    default java.util.List<CoupleChallenge> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleChallenge>()
                .eq(CoupleChallenge::getSpaceId, spaceId)
                .orderByDesc(CoupleChallenge::getDay));
    }
}
