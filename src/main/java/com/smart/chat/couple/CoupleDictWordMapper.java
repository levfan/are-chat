package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleDictWordMapper extends BaseMapperCompat<CoupleDictWord> {

    /** 空间的专属词汇（新→旧）。 */
    default List<CoupleDictWord> findBySpace(String spaceId) {
        return selectList(new LambdaQueryWrapper<CoupleDictWord>()
                .eq(CoupleDictWord::getSpaceId, spaceId)
                .orderByDesc(CoupleDictWord::getCreated));
    }
}
