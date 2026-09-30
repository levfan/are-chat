package com.smart.chat.couple;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.chat.im.BaseMapperCompat;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CoupleFeelingWordMapper extends BaseMapperCompat<CoupleFeelingWord> {

    /** 空间的情绪词汇足迹（新的在前）。 */
    default List<CoupleFeelingWord> findBySpace(String spaceId, int limit) {
        return selectList(new LambdaQueryWrapper<CoupleFeelingWord>()
                .eq(CoupleFeelingWord::getSpaceId, spaceId)
                .orderByDesc(CoupleFeelingWord::getDay)
                .orderByAsc(CoupleFeelingWord::getFromUser)
                .last("LIMIT " + Math.max(1, limit)));
    }
}
