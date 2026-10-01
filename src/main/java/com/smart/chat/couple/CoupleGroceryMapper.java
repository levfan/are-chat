package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F278 逛超市战利品数据访问。 */
@Mapper
public interface CoupleGroceryMapper extends BaseMapperCompat<CoupleGrocery> {

    /** 某人某周一条。 */
    default CoupleGrocery find(String spaceId, String week, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleGrocery>()
                .eq(CoupleGrocery::getSpaceId, spaceId)
                .eq(CoupleGrocery::getWeek, week)
                .eq(CoupleGrocery::getFromUser, fromUser));
    }

    /** 本周双单。 */
    default List<CoupleGrocery> findByWeek(String spaceId, String week) {
        return selectList(new LambdaQueryWrapper<CoupleGrocery>()
                .eq(CoupleGrocery::getSpaceId, spaceId)
                .eq(CoupleGrocery::getWeek, week));
    }
}
