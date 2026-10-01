package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F236 愿望券数据访问。 */
@Mapper
public interface CoupleCeremonyCouponMapper extends BaseMapperCompat<CoupleCeremonyCoupon> {

    /** 空间内全部券。 */
    default List<CoupleCeremonyCoupon> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleCeremonyCoupon>()
                .eq(CoupleCeremonyCoupon::getSpaceId, spaceId));
    }

    /** 某来源标记是否已 payout 过（保险柜幂等）。 */
    default boolean existsRef(String spaceId, String ref) {
        return selectCount(new LambdaQueryWrapper<CoupleCeremonyCoupon>()
                .eq(CoupleCeremonyCoupon::getSpaceId, spaceId)
                .eq(CoupleCeremonyCoupon::getRef, ref)) > 0;
    }
}
