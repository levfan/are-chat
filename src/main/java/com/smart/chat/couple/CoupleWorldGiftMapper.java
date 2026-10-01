package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F331 送礼互助池数据访问。 */
@Mapper
public interface CoupleWorldGiftMapper extends BaseMapperCompat<CoupleWorldGift> {

    /** 某条想法（uk 保证最多一条）。 */
    default CoupleWorldGift findByIdea(String spaceId, String idea) {
        return selectOne(new LambdaQueryWrapper<CoupleWorldGift>()
                .eq(CoupleWorldGift::getSpaceId, spaceId)
                .eq(CoupleWorldGift::getIdea, idea));
    }

    /** 还没人接单的清单（登记时间老→新）。 */
    default List<CoupleWorldGift> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldGift>()
                .eq(CoupleWorldGift::getSpaceId, spaceId)
                .eq(CoupleWorldGift::getStatus, CoupleWorldGift.STATUS_OPEN)
                .orderByAsc(CoupleWorldGift::getCreated));
    }

    /** 空间全部清单（登记时间新→旧）。 */
    default List<CoupleWorldGift> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldGift>()
                .eq(CoupleWorldGift::getSpaceId, spaceId)
                .orderByDesc(CoupleWorldGift::getCreated));
    }
}
