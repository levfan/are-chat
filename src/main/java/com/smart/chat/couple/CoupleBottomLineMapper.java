package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F326 底线声明卡数据访问。 */
@Mapper
public interface CoupleBottomLineMapper extends BaseMapperCompat<CoupleBottomLine> {

    /** 某人声明的底线（第几条升序）。 */
    default List<CoupleBottomLine> findByUser(String spaceId, String fromUser) {
        return selectList(new LambdaQueryWrapper<CoupleBottomLine>()
                .eq(CoupleBottomLine::getSpaceId, spaceId)
                .eq(CoupleBottomLine::getFromUser, fromUser)
                .orderByAsc(CoupleBottomLine::getSlot));
    }

    /** 某人某一条底线（uk 保证最多一条）。 */
    default CoupleBottomLine findBySlot(String spaceId, String fromUser, int slot) {
        return selectOne(new LambdaQueryWrapper<CoupleBottomLine>()
                .eq(CoupleBottomLine::getSpaceId, spaceId)
                .eq(CoupleBottomLine::getFromUser, fromUser)
                .eq(CoupleBottomLine::getSlot, slot));
    }

    /** 空间全部底线（登记时间老→新）。 */
    default List<CoupleBottomLine> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBottomLine>()
                .eq(CoupleBottomLine::getSpaceId, spaceId)
                .orderByAsc(CoupleBottomLine::getCreated));
    }
}
