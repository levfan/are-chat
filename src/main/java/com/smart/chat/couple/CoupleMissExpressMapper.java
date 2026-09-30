package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleMissExpressMapper extends BaseMapperCompat<CoupleMissExpress> {

    /** 我发出的思念（新→旧）。 */
    default List<CoupleMissExpress> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleMissExpress>()
                .eq(CoupleMissExpress::getSpaceId, spaceId)
                .eq(CoupleMissExpress::getFromUser, fromUser)
                .orderByDesc(CoupleMissExpress::getCreated));
    }

    /** 到点未送达的思念（定时任务扫描用）。 */
    default List<CoupleMissExpress> findDueUndelivered(long now) {
        return selectList(new LambdaQueryWrapper<CoupleMissExpress>()
                .eq(CoupleMissExpress::isDelivered, false)
                .le(CoupleMissExpress::getDeliverAt, now)
                .orderByAsc(CoupleMissExpress::getDeliverAt));
    }
}
