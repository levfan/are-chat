package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleGuessRoundMapper extends BaseMapperCompat<CoupleGuessRound> {

    /** 空间的对局记录（新的在前）。 */
    default List<CoupleGuessRound> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleGuessRound>()
                .eq(CoupleGuessRound::getSpaceId, spaceId)
                .orderByDesc(CoupleGuessRound::getCreated)
                .last("LIMIT 50"));
    }

    /** 某天已开的轮数（限流用）。 */
    default long countByDay(String spaceId, String day) {
        return selectCount(new LambdaQueryWrapper<CoupleGuessRound>()
                .eq(CoupleGuessRound::getSpaceId, spaceId)
                .eq(CoupleGuessRound::getDay, day));
    }
}
