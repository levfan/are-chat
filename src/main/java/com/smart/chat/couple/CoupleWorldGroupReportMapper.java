package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F338 群聊记者数据访问。 */
@Mapper
public interface CoupleWorldGroupReportMapper extends BaseMapperCompat<CoupleWorldGroupReport> {

    /** 某日的素材（uk 保证最多一条）。 */
    default CoupleWorldGroupReport findByDay(String spaceId, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleWorldGroupReport>()
                .eq(CoupleWorldGroupReport::getSpaceId, spaceId)
                .eq(CoupleWorldGroupReport::getDay, day));
    }

    /** 从某天起的素材（素材日新→旧）。 */
    default List<CoupleWorldGroupReport> findRecent(String spaceId, String fromDay) {
        return selectList(new LambdaQueryWrapper<CoupleWorldGroupReport>()
                .eq(CoupleWorldGroupReport::getSpaceId, spaceId)
                .ge(CoupleWorldGroupReport::getDay, fromDay)
                .orderByDesc(CoupleWorldGroupReport::getDay));
    }

    /** 空间全部素材（素材日新→旧）。 */
    default List<CoupleWorldGroupReport> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldGroupReport>()
                .eq(CoupleWorldGroupReport::getSpaceId, spaceId)
                .orderByDesc(CoupleWorldGroupReport::getDay));
    }
}
