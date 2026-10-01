package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F330 拜访攻略数据访问。 */
@Mapper
public interface CoupleWorldVisitMapper extends BaseMapperCompat<CoupleWorldVisit> {

    /** 空间待确认的攻略（拜访日由近及远）。 */
    default List<CoupleWorldVisit> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldVisit>()
                .eq(CoupleWorldVisit::getSpaceId, spaceId)
                .eq(CoupleWorldVisit::getStatus, CoupleWorldVisit.STATUS_OPEN)
                .orderByAsc(CoupleWorldVisit::getDay));
    }

    /** 某日的攻略（双方各一条，登记时间老→新）。 */
    default List<CoupleWorldVisit> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleWorldVisit>()
                .eq(CoupleWorldVisit::getSpaceId, spaceId)
                .eq(CoupleWorldVisit::getDay, day)
                .orderByAsc(CoupleWorldVisit::getCreated));
    }

    /** 空间全部攻略（拜访日新→旧）。 */
    default List<CoupleWorldVisit> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldVisit>()
                .eq(CoupleWorldVisit::getSpaceId, spaceId)
                .orderByDesc(CoupleWorldVisit::getDay));
    }
}
