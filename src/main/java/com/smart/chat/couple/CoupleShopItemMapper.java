package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F271 采买清单数据访问。 */
@Mapper
public interface CoupleShopItemMapper extends BaseMapperCompat<CoupleShopItem> {

    /** 在途清单。 */
    default List<CoupleShopItem> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleShopItem>()
                .eq(CoupleShopItem::getSpaceId, spaceId)
                .eq(CoupleShopItem::getStatus, CoupleShopItem.STATUS_OPEN)
                .orderByAsc(CoupleShopItem::getCreated));
    }

    /** 已买回（月榜统计按买回时间）。 */
    default List<CoupleShopItem> findDoneSince(String spaceId, long fromMs) {
        return selectList(new LambdaQueryWrapper<CoupleShopItem>()
                .eq(CoupleShopItem::getSpaceId, spaceId)
                .eq(CoupleShopItem::getStatus, CoupleShopItem.STATUS_DONE)
                .ge(CoupleShopItem::getDoneAt, fromMs));
    }
}
