package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F225 熬夜守护数据访问。 */
@Mapper
public interface CoupleCozyLatenightMapper extends BaseMapperCompat<CoupleCozyLatenight> {

    /** 某人某天的一条。 */
    default CoupleCozyLatenight find(String spaceId, String day, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleCozyLatenight>()
                .eq(CoupleCozyLatenight::getSpaceId, spaceId)
                .eq(CoupleCozyLatenight::getDay, day)
                .eq(CoupleCozyLatenight::getFromUser, fromUser));
    }
}
