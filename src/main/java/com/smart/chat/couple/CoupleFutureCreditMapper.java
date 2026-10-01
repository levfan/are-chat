package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** F299 未来信用卡数据访问。 */
@Mapper
public interface CoupleFutureCreditMapper extends BaseMapperCompat<CoupleFutureCredit> {

    /** 在途承诺。 */
    default List<CoupleFutureCredit> findOpen(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleFutureCredit>()
                .eq(CoupleFutureCredit::getSpaceId, spaceId)
                .eq(CoupleFutureCredit::getStatus, "OPEN")
                .orderByAsc(CoupleFutureCredit::getDueDay));
    }

    /** 全部（新→旧，额度与流水用）。 */
    default List<CoupleFutureCredit> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleFutureCredit>()
                .eq(CoupleFutureCredit::getSpaceId, spaceId)
                .orderByDesc(CoupleFutureCredit::getCreated));
    }
}
