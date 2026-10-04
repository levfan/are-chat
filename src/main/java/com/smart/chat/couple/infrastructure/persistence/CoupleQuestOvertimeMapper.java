package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F372 加班预报数据访问（uk(space_id,day,from_user) 每人每天一行）。 */
@Mapper
public interface CoupleQuestOvertimeMapper extends BaseMapperCompat<CoupleQuestOvertimePO> {

    /** 空间全部预报（新的在前，总览用）。 */
    default List<CoupleQuestOvertimePO> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestOvertimePO>()
                .eq(CoupleQuestOvertimePO::getSpaceId, spaceId)
                .orderByDesc(CoupleQuestOvertimePO::getDay));
    }

    /** 按 uk 定位某人那天的预报（写前查重与留灯回填用）。 */
    default CoupleQuestOvertimePO find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestOvertimePO>()
                .eq(CoupleQuestOvertimePO::getSpaceId, spaceId)
                .eq(CoupleQuestOvertimePO::getDay, day)
                .eq(CoupleQuestOvertimePO::getFromUser, fromUser));
    }

    /** 某天两人的全部预报（读对方今天有没有预报）。 */
    default List<CoupleQuestOvertimePO> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleQuestOvertimePO>()
                .eq(CoupleQuestOvertimePO::getSpaceId, spaceId)
                .eq(CoupleQuestOvertimePO::getDay, day)
                .orderByAsc(CoupleQuestOvertimePO::getDay));
    }
}
