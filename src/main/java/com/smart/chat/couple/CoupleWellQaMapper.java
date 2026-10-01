package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F295 许愿井周问数据访问。 */
@Mapper
public interface CoupleWellQaMapper extends BaseMapperCompat<CoupleWellQa> {

    /** 某人某周一答。 */
    default CoupleWellQa find(String spaceId, String week, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleWellQa>()
                .eq(CoupleWellQa::getSpaceId, spaceId)
                .eq(CoupleWellQa::getWeek, week)
                .eq(CoupleWellQa::getFromUser, fromUser));
    }

    /** 某年全部井答（周升序，回看用）。 */
    default List<CoupleWellQa> findYear(String spaceId, String weekFrom, String weekTo) {
        return selectList(new LambdaQueryWrapper<CoupleWellQa>()
                .eq(CoupleWellQa::getSpaceId, spaceId)
                .between(CoupleWellQa::getWeek, weekFrom, weekTo)
                .orderByAsc(CoupleWellQa::getWeek));
    }
}
