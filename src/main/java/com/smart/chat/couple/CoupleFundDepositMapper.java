package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleFundDepositMapper extends BaseMapperCompat<CoupleFundDeposit> {

    /** 某心愿基金的存入流水（新→旧）。 */
    default List<CoupleFundDeposit> findByFund(String fundId) {
        return selectList(new LambdaQueryWrapper<CoupleFundDeposit>()
                .eq(CoupleFundDeposit::getFundId, fundId)
                .orderByDesc(CoupleFundDeposit::getCreated));
    }

    /** 84 注销清理。 */
    default void deleteBySpace(String spaceId) {
        delete(new LambdaQueryWrapper<CoupleFundDeposit>().eq(CoupleFundDeposit::getSpaceId, spaceId));
    }
}
