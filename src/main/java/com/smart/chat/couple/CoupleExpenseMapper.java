package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleExpenseMapper extends BaseMapperCompat<CoupleExpense> {

    /** 某空间某个月（yyyy-MM）的全部账单，按日期新→旧（ISO 日期字符串可直接按字典序比较）。 */
    default List<CoupleExpense> findByMonth(String spaceId, String month) {
        return selectList(new LambdaQueryWrapper<CoupleExpense>()
                .eq(CoupleExpense::getSpaceId, spaceId)
                .ge(CoupleExpense::getSpentDay, month + "-01")
                .le(CoupleExpense::getSpentDay, month + "-31")
                .orderByDesc(CoupleExpense::getSpentDay)
                .orderByDesc(CoupleExpense::getCreated));
    }

    /** 某空间全部账单（AA 全史用）。 */
    default List<CoupleExpense> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleExpense>()
                .eq(CoupleExpense::getSpaceId, spaceId)
                .orderByDesc(CoupleExpense::getSpentDay));
    }
}
