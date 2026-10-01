package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F327 我错了榜数据访问。 */
@Mapper
public interface CoupleAdmitLogMapper extends BaseMapperCompat<CoupleAdmitLog> {

    /** 某人某天的认错（uk 保证最多一条）。 */
    default CoupleAdmitLog findByDayUser(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleAdmitLog>()
                .eq(CoupleAdmitLog::getSpaceId, spaceId)
                .eq(CoupleAdmitLog::getDay, day)
                .eq(CoupleAdmitLog::getFromUser, fromUser));
    }

    /** 从某天起的认错记录（认错日新→旧）。 */
    default List<CoupleAdmitLog> findRecent(String spaceId, String fromDay) {
        return selectList(new LambdaQueryWrapper<CoupleAdmitLog>()
                .eq(CoupleAdmitLog::getSpaceId, spaceId)
                .ge(CoupleAdmitLog::getDay, fromDay)
                .orderByDesc(CoupleAdmitLog::getDay));
    }

    /** 空间全部认错（认错日新→旧）。 */
    default List<CoupleAdmitLog> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleAdmitLog>()
                .eq(CoupleAdmitLog::getSpaceId, spaceId)
                .orderByDesc(CoupleAdmitLog::getDay));
    }
}
