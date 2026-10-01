package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F337 社会信用数据访问。 */
@Mapper
public interface CoupleWorldVowMapper extends BaseMapperCompat<CoupleWorldVow> {

    /** 到期日已到、还没解除的保证（today 为 yyyy-MM-dd）。 */
    default List<CoupleWorldVow> findDue(String spaceId, String today) {
        return selectList(new LambdaQueryWrapper<CoupleWorldVow>()
                .eq(CoupleWorldVow::getSpaceId, spaceId)
                .eq(CoupleWorldVow::getStatus, CoupleWorldVow.STATUS_OPEN)
                .le(CoupleWorldVow::getDueDay, today));
    }

    /** 某人立的保证（成立时间新→旧）。 */
    default List<CoupleWorldVow> findByOwner(String spaceId, String ownerUser) {
        return selectList(new LambdaQueryWrapper<CoupleWorldVow>()
                .eq(CoupleWorldVow::getSpaceId, spaceId)
                .eq(CoupleWorldVow::getOwnerUser, ownerUser)
                .orderByDesc(CoupleWorldVow::getCreated));
    }

    /** 空间全部保证（成立时间新→旧）。 */
    default List<CoupleWorldVow> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleWorldVow>()
                .eq(CoupleWorldVow::getSpaceId, spaceId)
                .orderByDesc(CoupleWorldVow::getCreated));
    }
}
