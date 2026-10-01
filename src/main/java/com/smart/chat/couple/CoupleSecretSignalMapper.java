package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleSecretSignalMapper extends BaseMapperCompat<CoupleSecretSignal> {

    /** 全部动作暗语（按创建倒序）。 */
    default List<CoupleSecretSignal> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleSecretSignal>()
                .eq(CoupleSecretSignal::getSpaceId, spaceId)
                .orderByDesc(CoupleSecretSignal::getCreated));
    }
}
