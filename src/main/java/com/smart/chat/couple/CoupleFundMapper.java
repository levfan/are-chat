package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleFundMapper extends BaseMapperCompat<CoupleFund> {

    /** 某空间全部心愿基金（新→旧）。 */
    default List<CoupleFund> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleFund>()
                .eq(CoupleFund::getSpaceId, spaceId)
                .orderByAsc(CoupleFund::getStatus)
                .orderByDesc(CoupleFund::getCreated));
    }

    /** 84 注销清理。 */
    default void deleteBySpace(String spaceId) {
        delete(new LambdaQueryWrapper<CoupleFund>().eq(CoupleFund::getSpaceId, spaceId));
    }
}
