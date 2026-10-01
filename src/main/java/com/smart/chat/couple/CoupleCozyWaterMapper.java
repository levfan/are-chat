package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F223 喝水接力数据访问。 */
@Mapper
public interface CoupleCozyWaterMapper extends BaseMapperCompat<CoupleCozyWater> {

    /** 某人某天的一条。 */
    default CoupleCozyWater find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCozyWater>()
                .eq(CoupleCozyWater::getSpaceId, spaceId)
                .eq(CoupleCozyWater::getDay, day)
                .eq(CoupleCozyWater::getFromUser, fromUser));
    }

    /** 一段时间的喝水记录（月度小结）。 */
    default List<CoupleCozyWater> findRange(String spaceId, String fromDay, String toDay) {
        return selectList(new LambdaQueryWrapper<CoupleCozyWater>()
                .eq(CoupleCozyWater::getSpaceId, spaceId)
                .between(CoupleCozyWater::getDay, fromDay, toDay));
    }
}
