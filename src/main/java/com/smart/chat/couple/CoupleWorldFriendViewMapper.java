package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F332 朋友视角问卷数据访问。 */
@Mapper
public interface CoupleWorldFriendViewMapper extends BaseMapperCompat<CoupleWorldFriendView> {

    /** 第几题的一行（uk 保证最多一条）。 */
    default CoupleWorldFriendView findBySlot(String spaceId, int slot) {
        return selectOne(new LambdaQueryWrapper<CoupleWorldFriendView>()
                .eq(CoupleWorldFriendView::getSpaceId, spaceId)
                .eq(CoupleWorldFriendView::getSlot, slot));
    }

    /** 空间整卷（题号升序）。 */
    default List<CoupleWorldFriendView> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldFriendView>()
                .eq(CoupleWorldFriendView::getSpaceId, spaceId)
                .orderByAsc(CoupleWorldFriendView::getSlot));
    }
}
