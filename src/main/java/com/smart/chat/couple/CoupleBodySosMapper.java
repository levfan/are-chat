package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F315 身体不适 SOS 数据访问。 */
@Mapper
public interface CoupleBodySosMapper extends BaseMapperCompat<CoupleBodySos> {

    /** 空间全部 SOS（新→旧）。 */
    default List<CoupleBodySos> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBodySos>()
                .eq(CoupleBodySos::getSpaceId, spaceId)
                .orderByDesc(CoupleBodySos::getCreated));
    }

    /** 在途未接住的（发出先后升序）。 */
    default List<CoupleBodySos> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBodySos>()
                .eq(CoupleBodySos::getSpaceId, spaceId)
                .eq(CoupleBodySos::getStatus, CoupleBodySos.STATUS_SENT)
                .orderByAsc(CoupleBodySos::getCreated));
    }

    /** 某人报过的 SOS（新→旧）。 */
    default List<CoupleBodySos> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleBodySos>()
                .eq(CoupleBodySos::getSpaceId, spaceId)
                .eq(CoupleBodySos::getFromUser, fromUser)
                .orderByDesc(CoupleBodySos::getCreated));
    }
}
