package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F288 口味变迁数据访问。 */
@Mapper
public interface CoupleTasteShiftMapper extends BaseMapperCompat<CoupleTasteShift> {

    /** 本人某对象一条。 */
    default CoupleTasteShift findThing(String spaceId, String thing, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleTasteShift>()
                .eq(CoupleTasteShift::getSpaceId, spaceId)
                .eq(CoupleTasteShift::getThing, thing)
                .eq(CoupleTasteShift::getFromUser, fromUser));
    }

    /** 全部变迁（按转折日升序）。 */
    default List<CoupleTasteShift> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTasteShift>()
                .eq(CoupleTasteShift::getSpaceId, spaceId)
                .orderByAsc(CoupleTasteShift::getShiftedDay));
    }
}
