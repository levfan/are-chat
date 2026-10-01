package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F281 默契综艺数据访问。 */
@Mapper
public interface CoupleQuizShowMapper extends BaseMapperCompat<CoupleQuizShow> {

    /** 某天一期。 */
    default CoupleQuizShow findDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleQuizShow>()
                .eq(CoupleQuizShow::getSpaceId, spaceId)
                .eq(CoupleQuizShow::getDay, day));
    }

    /** 历届（新→旧）。 */
    default List<CoupleQuizShow> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuizShow>()
                .eq(CoupleQuizShow::getSpaceId, spaceId)
                .orderByDesc(CoupleQuizShow::getDay));
    }
}
