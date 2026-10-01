package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F282 本人榜数据访问。 */
@Mapper
public interface CoupleTopListMapper extends BaseMapperCompat<CoupleTopList> {

    /** 某人某类目一榜。 */
    default CoupleTopList find(String spaceId, String category, String ownerUser) {
        return selectOne(new LambdaQueryWrapper<CoupleTopList>()
                .eq(CoupleTopList::getSpaceId, spaceId)
                .eq(CoupleTopList::getCategory, category)
                .eq(CoupleTopList::getOwnerUser, ownerUser));
    }

    /** 全部榜单。 */
    default List<CoupleTopList> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleTopList>()
                .eq(CoupleTopList::getSpaceId, spaceId));
    }
}
