package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSosPingMapper extends BaseMapperCompat<CoupleSosPing> {

    /** 空间的 SOS 记录（新的在前）。 */
    default List<CoupleSosPing> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSosPing>()
                .eq(CoupleSosPing::getSpaceId, spaceId)
                .orderByDesc(CoupleSosPing::getCreated)
                .last("LIMIT 20"));
    }

    /** 某人最近一条 SOS。 */
    default CoupleSosPing findLatestByUser(String spaceId, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleSosPing>()
                .eq(CoupleSosPing::getSpaceId, spaceId)
                .eq(CoupleSosPing::getFromUser, fromUser)
                .orderByDesc(CoupleSosPing::getCreated)
                .last("LIMIT 1"));
    }
}
