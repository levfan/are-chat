package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F277 垫付本数据访问。 */
@Mapper
public interface CoupleAdvanceMapper extends BaseMapperCompat<CoupleAdvance> {

    /** 未清账（新→旧）。 */
    default List<CoupleAdvance> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleAdvance>()
                .eq(CoupleAdvance::getSpaceId, spaceId)
                .eq(CoupleAdvance::getStatus, CoupleAdvance.STATUS_OPEN)
                .orderByDesc(CoupleAdvance::getCreated));
    }

    /** 全部（清账聚合用）。 */
    default List<CoupleAdvance> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleAdvance>()
                .eq(CoupleAdvance::getSpaceId, spaceId));
    }
}
