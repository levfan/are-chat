package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F373 生病陪护单数据访问（uk(space_id,patient_user,open_day) 同人同日只一单）。 */
@Mapper
public interface CoupleQuestNurseMapper extends BaseMapperCompat<CoupleQuestNurse> {

    /** 空间全部陪护单（开单日渐降序，总览与陪护家用）。 */
    default List<CoupleQuestNurse> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestNurse>()
                .eq(CoupleQuestNurse::getSpaceId, spaceId)
                .orderByDesc(CoupleQuestNurse::getOpenDay));
    }

    /** 按 uk 定位某人某天的陪护单（写前查重与代记回填用）。 */
    default CoupleQuestNurse find(String spaceId, String patientUser, String openDay) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestNurse>()
                .eq(CoupleQuestNurse::getSpaceId, spaceId)
                .eq(CoupleQuestNurse::getPatientUser, patientUser)
                .eq(CoupleQuestNurse::getOpenDay, openDay));
    }

    /** 某人还在陪护中的那一单（IN_FLIGHT_MAX=1，没有=没在途）。 */
    default CoupleQuestNurse findOpen(String spaceId, String patientUser) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestNurse>()
                .eq(CoupleQuestNurse::getSpaceId, spaceId)
                .eq(CoupleQuestNurse::getPatientUser, patientUser)
                .eq(CoupleQuestNurse::getStatus, CoupleQuestNurse.STATUS_OPEN));
    }
}
