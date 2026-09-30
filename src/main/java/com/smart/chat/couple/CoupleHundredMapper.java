package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleHundredMapper extends BaseMapperCompat<CoupleHundred> {

    /** 空间的百日之约（新→旧）。 */
    default List<CoupleHundred> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleHundred>()
                .eq(CoupleHundred::getSpaceId, spaceId)
                .orderByDesc(CoupleHundred::getCreated));
    }
}
