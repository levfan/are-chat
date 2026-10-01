package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F273 代拿快递数据访问。 */
@Mapper
public interface CoupleParcelMapper extends BaseMapperCompat<CoupleParcel> {

    /** 在途单（SENT/GRABBED）。 */
    default List<CoupleParcel> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleParcel>()
                .eq(CoupleParcel::getSpaceId, spaceId)
                .in(CoupleParcel::getStatus, CoupleParcel.STATUS_SENT, CoupleParcel.STATUS_GRABBED)
                .orderByAsc(CoupleParcel::getCreated));
    }
}
