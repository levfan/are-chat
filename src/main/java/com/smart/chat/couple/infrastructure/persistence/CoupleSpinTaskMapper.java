package com.smart.chat.couple.infrastructure.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.sharedkernel.persistence.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F270 家务轮盘数据访问。 */
@Mapper
public interface CoupleSpinTaskMapper extends BaseMapperCompat<CoupleSpinTaskPO> {

    /** 某周全部任务。 */
    default List<CoupleSpinTaskPO> findByWeek(String spaceId, String week) {
        return selectList(new LambdaQueryWrapper<CoupleSpinTaskPO>()
                .eq(CoupleSpinTaskPO::getSpaceId, spaceId)
                .eq(CoupleSpinTaskPO::getWeek, week));
    }

    /** 某周某事一条。 */
    default CoupleSpinTaskPO find(String spaceId, String week, String item) {
        return selectOne(new LambdaQueryWrapper<CoupleSpinTaskPO>()
                .eq(CoupleSpinTaskPO::getSpaceId, spaceId)
                .eq(CoupleSpinTaskPO::getWeek, week)
                .eq(CoupleSpinTaskPO::getItem, item));
    }
}
