package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleHeartFlashMapper extends BaseMapperCompat<CoupleHeartFlash> {

    /** 全部心动闪光（按创建倒序）。 */
    default List<CoupleHeartFlash> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleHeartFlash>()
                .eq(CoupleHeartFlash::getSpaceId, spaceId)
                .orderByDesc(CoupleHeartFlash::getCreated));
    }
}
