package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F220 晚安同熄灯数据访问。 */
@Mapper
public interface CoupleCozyLightoutMapper extends BaseMapperCompat<CoupleCozyLightout> {

    /** 某人某天的一条。 */
    default CoupleCozyLightout find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCozyLightout>()
                .eq(CoupleCozyLightout::getSpaceId, spaceId)
                .eq(CoupleCozyLightout::getDay, day)
                .eq(CoupleCozyLightout::getFromUser, fromUser));
    }

    /** 某天双方是否都熄灯。 */
    default List<CoupleCozyLightout> findByDay(String spaceId, String day) {
        return selectList(new LambdaQueryWrapper<CoupleCozyLightout>()
                .eq(CoupleCozyLightout::getSpaceId, spaceId)
                .eq(CoupleCozyLightout::getDay, day));
    }

    /** 一段时间的熄灯记录（月度小结）。 */
    default List<CoupleCozyLightout> findRange(String spaceId, String fromDay, String toDay) {
        return selectList(new LambdaQueryWrapper<CoupleCozyLightout>()
                .eq(CoupleCozyLightout::getSpaceId, spaceId)
                .between(CoupleCozyLightout::getDay, fromDay, toDay));
    }
}
