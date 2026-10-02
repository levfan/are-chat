package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F371 出关战报数据访问（uk(battle_id) 一战只一报）。 */
@Mapper
public interface CoupleQuestReportMapper extends BaseMapperCompat<CoupleQuestReport> {

    /** 空间全部战报（新的在前，总览与成就墙用）。 */
    default List<CoupleQuestReport> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestReport>()
                .eq(CoupleQuestReport::getSpaceId, spaceId)
                .orderByDesc(CoupleQuestReport::getCreated));
    }

    /** 某一关的战报（按 uk 取，没有=还没报）。 */
    default CoupleQuestReport findByBattle(String battleId) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestReport>()
                .eq(CoupleQuestReport::getBattleId, battleId));
    }
}
