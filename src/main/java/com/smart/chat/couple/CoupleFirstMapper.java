package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleFirstMapper extends BaseMapperCompat<CoupleFirst> {

    /** 第一次清单：按发生日期升序（像沿着时间线回忆）。 */
    default List<CoupleFirst> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleFirst>()
                .eq(CoupleFirst::getSpaceId, spaceId)
                .orderByAsc(CoupleFirst::getFirstDay)
                .orderByAsc(CoupleFirst::getCreated));
    }
}
