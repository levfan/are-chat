package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleReunionLogMapper extends BaseMapperCompat<CoupleReunionLog> {

    /** 空间的见面日记（新的在前）。 */
    default List<CoupleReunionLog> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleReunionLog>()
                .eq(CoupleReunionLog::getSpaceId, spaceId)
                .orderByDesc(CoupleReunionLog::getMeetDay)
                .last("LIMIT 60"));
    }

    /** 最近一次见面的日期（能量瓶用）。 */
    default CoupleReunionLog findLatest(String spaceId) {
        return selectOne(new LambdaQueryWrapper<CoupleReunionLog>()
                .eq(CoupleReunionLog::getSpaceId, spaceId)
                .orderByDesc(CoupleReunionLog::getMeetDay)
                .last("LIMIT 1"));
    }
}
