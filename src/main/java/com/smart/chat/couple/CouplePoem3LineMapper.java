package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouplePoem3LineMapper extends BaseMapperCompat<CouplePoem3Line> {

    /** 全部三行情书（按创建倒序）。 */
    default List<CouplePoem3Line> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CouplePoem3Line>()
                .eq(CouplePoem3Line::getSpaceId, spaceId)
                .orderByDesc(CouplePoem3Line::getCreated));
    }
}
