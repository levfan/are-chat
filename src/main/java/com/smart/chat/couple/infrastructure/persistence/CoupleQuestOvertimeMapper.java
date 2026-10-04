package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F372 加班预报数据访问（uk(space_id,day,from_user) 每人每天一行）。 */
@Mapper
public interface CoupleQuestOvertimeMapper extends BaseMapperCompat<CoupleQuestOvertime> {

    /** 空间全部预报（新的在前，总览用）。 */
    default List<CoupleQuestOvertime> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestOvertime>()
                .eq(CoupleQuestOvertime::getSpaceId, spaceId)
                .orderByDesc(CoupleQuestOvertime::getDay));
    }

    /** 按 uk 定位某人那天的预报（写前查重与留灯回填用）。 */
    default CoupleQuestOvertime find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestOvertime>()
                .eq(CoupleQuestOvertime::getSpaceId, spaceId)
                .eq(CoupleQuestOvertime::getDay, day)
                .eq(CoupleQuestOvertime::getFromUser, fromUser));
    }

    /** 某天两人的全部预报（读对方今天有没有预报）。 */
    default List<CoupleQuestOvertime> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleQuestOvertime>()
                .eq(CoupleQuestOvertime::getSpaceId, spaceId)
                .eq(CoupleQuestOvertime::getDay, day)
                .orderByAsc(CoupleQuestOvertime::getDay));
    }
}
