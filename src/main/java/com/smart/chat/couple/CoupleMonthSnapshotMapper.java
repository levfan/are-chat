package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleMonthSnapshotMapper extends BaseMapperCompat<CoupleMonthSnapshot> {

    /** 某人某月的存档。 */
    default CoupleMonthSnapshot find(String spaceId, String month, String fromUser) {
        return selectOne(new LambdaQueryWrapper<CoupleMonthSnapshot>()
                .eq(CoupleMonthSnapshot::getSpaceId, spaceId)
                .eq(CoupleMonthSnapshot::getMonth, month)
                .eq(CoupleMonthSnapshot::getFromUser, fromUser));
    }

    /** 全部存档点（按月倒序）。 */
    default List<CoupleMonthSnapshot> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleMonthSnapshot>()
                .eq(CoupleMonthSnapshot::getSpaceId, spaceId)
                .orderByDesc(CoupleMonthSnapshot::getMonth));
    }
}
