package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F376 低谷通行证数据访问（uk(space_id,from_user,open_day) 同人同宣布日只一张）。 */
@Mapper
public interface CoupleQuestValleyMapper extends BaseMapperCompat<CoupleQuestValley> {

    /** 空间全部通行证（宣布日降序，新的在前）。 */
    default List<CoupleQuestValley> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestValley>()
                .eq(CoupleQuestValley::getSpaceId, spaceId)
                .orderByDesc(CoupleQuestValley::getOpenDay));
    }

    /** 按 uk 定位某人某日开的那张（写前查重与递卡回填用）。 */
    default CoupleQuestValley find(String spaceId, String fromUser, String openDay) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestValley>()
                .eq(CoupleQuestValley::getSpaceId, spaceId)
                .eq(CoupleQuestValley::getFromUser, fromUser)
                .eq(CoupleQuestValley::getOpenDay, openDay));
    }

    /** 某人还在谷底的那张（status=LOW，在途每人 ≤1）。 */
    default CoupleQuestValley findLow(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestValley>()
                .eq(CoupleQuestValley::getSpaceId, spaceId)
                .eq(CoupleQuestValley::getFromUser, fromUser)
                .eq(CoupleQuestValley::getStatus, CoupleQuestValley.STATUS_LOW));
    }
}
