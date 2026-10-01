package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F347 传世清单数据访问。 */
@Mapper
public interface CoupleLegacyItemMapper extends BaseMapperCompat<CoupleLegacyItem> {

    /** 空间未封存的条目（登记时间老→新）。 */
    default List<CoupleLegacyItem> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLegacyItem>()
                .eq(CoupleLegacyItem::getSpaceId, spaceId)
                .eq(CoupleLegacyItem::getStatus, CoupleLegacyItem.STATUS_OPEN)
                .orderByAsc(CoupleLegacyItem::getCreated));
    }

    /** 同名条目（uk(space_id,item) 保证最多一条）。 */
    default CoupleLegacyItem findByItem(String spaceId, String item) {
        return selectOne(new LambdaQueryWrapper<CoupleLegacyItem>()
                .eq(CoupleLegacyItem::getSpaceId, spaceId)
                .eq(CoupleLegacyItem::getItem, item));
    }

    /** 空间全部条目（登记时间新→旧）。 */
    default List<CoupleLegacyItem> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleLegacyItem>()
                .eq(CoupleLegacyItem::getSpaceId, spaceId)
                .orderByDesc(CoupleLegacyItem::getCreated));
    }
}
