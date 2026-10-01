package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F272 冰箱库存数据访问。 */
@Mapper
public interface CoupleStockMapper extends BaseMapperCompat<CoupleStock> {

    /** 同空间同名一条（不限状态，复活覆盖用）。 */
    default CoupleStock findItem(String spaceId, String item) {
        return selectOne(new LambdaQueryWrapper<CoupleStock>()
                .eq(CoupleStock::getSpaceId, spaceId)
                .eq(CoupleStock::getItem, item));
    }

    /** 在库清单。 */
    default List<CoupleStock> findIn(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleStock>()
                .eq(CoupleStock::getSpaceId, spaceId)
                .eq(CoupleStock::getStatus, CoupleStock.STATUS_IN)
                .orderByAsc(CoupleStock::getExpireDay));
    }
}
