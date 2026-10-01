package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F301 一日互换日记数据访问。 */
@Mapper
public interface CoupleSwapDiaryMapper extends BaseMapperCompat<CoupleSwapDiary> {

    default List<CoupleSwapDiary> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleSwapDiary>()
                .eq(CoupleSwapDiary::getSpaceId, spaceId)
                .eq(CoupleSwapDiary::getDay, day));
    }

    default List<CoupleSwapDiary> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSwapDiary>()
                .eq(CoupleSwapDiary::getSpaceId, spaceId)
                .orderByDesc(CoupleSwapDiary::getDay));
    }
}
