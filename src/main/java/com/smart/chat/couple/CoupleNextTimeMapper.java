package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleNextTimeMapper extends BaseMapperCompat<CoupleNextTime> {

    /** 空间的「下次一定」（待兑现在前）。 */
    default List<CoupleNextTime> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleNextTime>()
                .eq(CoupleNextTime::getSpaceId, spaceId)
                .orderByAsc(CoupleNextTime::getStatus)
                .orderByDesc(CoupleNextTime::getCreated));
    }
}
