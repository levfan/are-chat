package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F379 下次关卡预约数据访问（uk(space_id,day,from_user,title) 同人同日同名只一条）。 */
@Mapper
public interface CoupleQuestUpcomingMapper extends BaseMapperCompat<CoupleQuestUpcoming> {

    /** 空间全部预约（日渐升序，双人时间轴用）。 */
    default List<CoupleQuestUpcoming> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestUpcoming>()
                .eq(CoupleQuestUpcoming::getSpaceId, spaceId)
                .orderByAsc(CoupleQuestUpcoming::getDay));
    }

    /** 按 uk 定位某条预约（写前查重与到场回填用）。 */
    default CoupleQuestUpcoming find(String spaceId, String day, String fromUser, String title) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestUpcoming>()
                .eq(CoupleQuestUpcoming::getSpaceId, spaceId)
                .eq(CoupleQuestUpcoming::getDay, day)
                .eq(CoupleQuestUpcoming::getFromUser, fromUser)
                .eq(CoupleQuestUpcoming::getTitle, title));
    }

    /** 某天的全部预约（当日「我会到场」看板用）。 */
    default List<CoupleQuestUpcoming> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleQuestUpcoming>()
                .eq(CoupleQuestUpcoming::getSpaceId, spaceId)
                .eq(CoupleQuestUpcoming::getDay, day));
    }
}
