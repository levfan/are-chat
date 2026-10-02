package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F374 考试周静音舱数据访问（uk(space_id,from_user,start_day) 同人同入舱日只一舱）。 */
@Mapper
public interface CoupleQuestPodMapper extends BaseMapperCompat<CoupleQuestPod> {

    /** 空间全部静音舱（入舱日降序，新的在前）。 */
    default List<CoupleQuestPod> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleQuestPod>()
                .eq(CoupleQuestPod::getSpaceId, spaceId)
                .orderByDesc(CoupleQuestPod::getStartDay));
    }

    /** 按 uk 定位某人某日入的那舱（写前查重与加油回填用）。 */
    default CoupleQuestPod find(String spaceId, String fromUser, String startDay) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestPod>()
                .eq(CoupleQuestPod::getSpaceId, spaceId)
                .eq(CoupleQuestPod::getFromUser, fromUser)
                .eq(CoupleQuestPod::getStartDay, startDay));
    }

    /** 某人还在舱里的那一舱（status=IN）。 */
    default CoupleQuestPod findIn(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleQuestPod>()
                .eq(CoupleQuestPod::getSpaceId, spaceId)
                .eq(CoupleQuestPod::getFromUser, fromUser)
                .eq(CoupleQuestPod::getStatus, CoupleQuestPod.STATUS_IN));
    }
}
