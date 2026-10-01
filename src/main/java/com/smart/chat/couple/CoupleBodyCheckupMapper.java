package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F317 体检陪同数据访问。 */
@Mapper
public interface CoupleBodyCheckupMapper extends BaseMapperCompat<CoupleBodyCheckup> {

    /** fromDay 起还没过去的体检（由近及远）。 */
    default List<CoupleBodyCheckup> findUpcoming(String spaceId, String fromDay) {
        return selectList(new LambdaQueryWrapper<CoupleBodyCheckup>()
                .eq(CoupleBodyCheckup::getSpaceId, spaceId)
                .ge(CoupleBodyCheckup::getDay, fromDay)
                .orderByAsc(CoupleBodyCheckup::getDay));
    }

    /** 空间全部体检（新→旧）。 */
    default List<CoupleBodyCheckup> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleBodyCheckup>()
                .eq(CoupleBodyCheckup::getSpaceId, spaceId)
                .orderByDesc(CoupleBodyCheckup::getDay));
    }

    /** 当日体检人一行（uk 保证最多一条）。 */
    default CoupleBodyCheckup findByDayUser(String spaceId, String day, String ownerUser) {
        return selectOne(new LambdaQueryWrapper<CoupleBodyCheckup>()
                .eq(CoupleBodyCheckup::getSpaceId, spaceId)
                .eq(CoupleBodyCheckup::getDay, day)
                .eq(CoupleBodyCheckup::getOwnerUser, ownerUser));
    }
}
