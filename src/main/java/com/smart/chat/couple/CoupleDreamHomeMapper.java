package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F293 想象中的家数据访问。 */
@Mapper
public interface CoupleDreamHomeMapper extends BaseMapperCompat<CoupleDreamHome> {

    /** 某人某年版一份。 */
    default CoupleDreamHome find(String spaceId, String year, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleDreamHome>()
                .eq(CoupleDreamHome::getSpaceId, spaceId)
                .eq(CoupleDreamHome::getYear, year)
                .eq(CoupleDreamHome::getFromUser, fromUser));
    }

    /** 全部版本（新年→旧年）。 */
    default List<CoupleDreamHome> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDreamHome>()
                .eq(CoupleDreamHome::getSpaceId, spaceId)
                .orderByDesc(CoupleDreamHome::getYear));
    }
}
