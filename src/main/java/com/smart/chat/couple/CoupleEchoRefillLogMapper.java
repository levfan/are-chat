package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F351 能量补给领取日志数据访问。 */
@Mapper
public interface CoupleEchoRefillLogMapper extends BaseMapperCompat<CoupleEchoRefillLog> {

    /** 某人某天是否已领过（uk(space_id,from_user,day) 最多一条）。 */
    default CoupleEchoRefillLog findByDayUser(String spaceId, String fromUser, String day) {
        return selectOne(new LambdaQueryWrapper<CoupleEchoRefillLog>()
                .eq(CoupleEchoRefillLog::getSpaceId, spaceId)
                .eq(CoupleEchoRefillLog::getFromUser, fromUser)
                .eq(CoupleEchoRefillLog::getDay, day));
    }

    /** 空间全部领取日志（日历/年报按天聚合用）。 */
    default List<CoupleEchoRefillLog> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleEchoRefillLog>()
                .eq(CoupleEchoRefillLog::getSpaceId, spaceId)
                .orderByAsc(CoupleEchoRefillLog::getDay));
    }
}
