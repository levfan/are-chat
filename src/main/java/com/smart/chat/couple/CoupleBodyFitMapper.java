package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F314 运动链数据访问。 */
@Mapper
public interface CoupleBodyFitMapper extends BaseMapperCompat<CoupleBodyFit> {

    /** 当日同项目一行（uk 保证最多一条）。 */
    default CoupleBodyFit findByDayKind(String spaceId, String day, String kind) {
        return selectOne(new LambdaQueryWrapper<CoupleBodyFit>()
                .eq(CoupleBodyFit::getSpaceId, spaceId)
                .eq(CoupleBodyFit::getDay, day)
                .eq(CoupleBodyFit::getKind, kind));
    }

    /** fromDay 起的计数行（旧→新，接链天数统计用）。 */
    default List<CoupleBodyFit> findRecent(String spaceId, String fromDay) {
        return selectList(new LambdaQueryWrapper<CoupleBodyFit>()
                .eq(CoupleBodyFit::getSpaceId, spaceId)
                .ge(CoupleBodyFit::getDay, fromDay)
                .orderByAsc(CoupleBodyFit::getDay));
    }
}
