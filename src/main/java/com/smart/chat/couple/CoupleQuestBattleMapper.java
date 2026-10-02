package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F370 关卡预告数据访问（uk(space_id,from_user,day,name) 同人同日同名只一行）。 */
@Mapper
public interface CoupleQuestBattleMapper extends BaseMapperCompat<CoupleQuestBattle> {

    /** 空间全部关卡（关卡日升序，双人时间轴用）。 */
    default List<CoupleQuestBattle> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestBattle>()
                .eq(CoupleQuestBattle::getSpaceId, spaceId)
                .orderByAsc(CoupleQuestBattle::getDay));
    }

    /** 按 uk 定位那一关（写前查重、战报回填用）。 */
    default CoupleQuestBattle find(String spaceId, String fromUser, String day, String name) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestBattle>()
                .eq(CoupleQuestBattle::getSpaceId, spaceId)
                .eq(CoupleQuestBattle::getFromUser, fromUser)
                .eq(CoupleQuestBattle::getDay, day)
                .eq(CoupleQuestBattle::getName, name));
    }

    /** 某人还在途的关卡数（IN_FLIGHT_MAX 上限校验用）。 */
    default long countPrep(String spaceId, String fromUser) {
        return selectCount(new LambdaQueryWrapper<CoupleQuestBattle>()
                .eq(CoupleQuestBattle::getSpaceId, spaceId)
                .eq(CoupleQuestBattle::getFromUser, fromUser)
                .eq(CoupleQuestBattle::getStatus, CoupleQuestBattle.STATUS_PREP));
    }
}
