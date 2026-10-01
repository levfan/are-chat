package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F266 三行打卡数据访问。 */
@Mapper
public interface CoupleThreeLineMapper extends BaseMapperCompat<CoupleThreeLine> {

    /** 某人某天一条。 */
    default CoupleThreeLine find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleThreeLine>()
                .eq(CoupleThreeLine::getSpaceId, spaceId)
                .eq(CoupleThreeLine::getDay, day)
                .eq(CoupleThreeLine::getFromUser, fromUser));
    }

    /** 某人全部打卡（按日升序，连击计算用）。 */
    default List<CoupleThreeLine> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleThreeLine>()
                .eq(CoupleThreeLine::getSpaceId, spaceId)
                .eq(CoupleThreeLine::getFromUser, fromUser)
                .orderByAsc(CoupleThreeLine::getDay));
    }
}
