package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F366 走神温柔哨数据访问（每人每天 ≤2 张，额度按 from_user 独立）。 */
@Mapper
public interface CoupleFocusNudgeMapper extends BaseMapperCompat<CoupleFocusNudge> {

    /** 某人当天已递的卡（额度校验用）。 */
    default List<CoupleFocusNudge> findByDayUser(String spaceId, String day, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleFocusNudge>()
                .eq(CoupleFocusNudge::getSpaceId, spaceId)
                .eq(CoupleFocusNudge::getDay, day)
                .eq(CoupleFocusNudge::getFromUser, fromUser)
                .orderByAsc(CoupleFocusNudge::getCreated));
    }

    /** 当天双方的卡（总览用，新的在前）。 */
    default List<CoupleFocusNudge> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleFocusNudge>()
                .eq(CoupleFocusNudge::getSpaceId, spaceId)
                .eq(CoupleFocusNudge::getDay, day)
                .orderByDesc(CoupleFocusNudge::getCreated));
    }

    /** 某年全部（周报/年报计数用）。 */
    default List<CoupleFocusNudge> findByYear(String spaceId, String year) {
        return selectList(new LambdaQueryWrapper<CoupleFocusNudge>()
                .eq(CoupleFocusNudge::getSpaceId, spaceId)
                .likeRight(CoupleFocusNudge::getDay, year + "-")
                .orderByAsc(CoupleFocusNudge::getDay));
    }
}
