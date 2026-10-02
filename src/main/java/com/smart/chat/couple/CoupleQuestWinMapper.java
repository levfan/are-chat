package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F377 小胜利账本数据访问（uk(space_id,day,from_user) 每人每天一条）。 */
@Mapper
public interface CoupleQuestWinMapper extends BaseMapperCompat<CoupleQuestWin> {

    /** 空间全部账本（日渐降序，新的在前）。 */
    default List<CoupleQuestWin> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestWin>()
                .eq(CoupleQuestWin::getSpaceId, spaceId)
                .orderByDesc(CoupleQuestWin::getDay));
    }

    /** 某天两人的全部记账（当日看板用）。 */
    default List<CoupleQuestWin> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleQuestWin>()
                .eq(CoupleQuestWin::getSpaceId, spaceId)
                .eq(CoupleQuestWin::getDay, day));
    }

    /** 按 uk 定位某人那天的账（写前查重与颁奖回填用）。 */
    default CoupleQuestWin find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestWin>()
                .eq(CoupleQuestWin::getSpaceId, spaceId)
                .eq(CoupleQuestWin::getDay, day)
                .eq(CoupleQuestWin::getFromUser, fromUser));
    }

    /** [fromDay,toDay] 闭区间（周赛奖与周报用，日渐升序）。 */
    default List<CoupleQuestWin> findByDayRange(String spaceId, String fromDay, String toDay) {
        return selectList(new LambdaQueryWrapper<CoupleQuestWin>()
                .eq(CoupleQuestWin::getSpaceId, spaceId)
                .ge(CoupleQuestWin::getDay, fromDay)
                .le(CoupleQuestWin::getDay, toDay)
                .orderByAsc(CoupleQuestWin::getDay));
    }
}
