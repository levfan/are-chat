package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleReadProgressMapper extends BaseMapperCompat<CoupleReadProgress> {

    /** 某计划全部进度上报（新→旧，取每人最新一条由服务层处理）。 */
    default List<CoupleReadProgress> findByPlan(String planId) {
        return selectList(new LambdaQueryWrapper<CoupleReadProgress>()
                .eq(CoupleReadProgress::getPlanId, planId)
                .orderByDesc(CoupleReadProgress::getCreated));
    }
}
